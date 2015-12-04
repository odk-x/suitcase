package model;

import com.squareup.okhttp.Request;
import model.serialization.Field;
import model.serialization.FieldsWrapper;
import net.RESTClient;
import net.WebAgent;
import org.opendatakit.aggregate.odktables.rest.RFC4180CsvWriter;
import utils.JSONUtils;

import java.util.*;

/**
 * Created by Jeff Beorse on 8/5/15.
 *
 * Handles processing of raw CSV files from Aggregate to M&E Formatting
 *
 */
public class FormattedCSV {

    // There are two executions states: defining the headers and adding rows
    private enum EXEC_STATE {
        DEFINE_HEADERS,
        FILL_DATA
    }

    private enum COLUMN_CLASS {
        KEEP,                   // Regular column data
        FILTER,                 // Metadata that we don't want in the M&E Report
        IMAGE,                  // Image file name
        FILE,                   // Generic file type
        RAW                     // Paired with IMAGE columns, holds the value predicted by the image
    }

    private class Header {
        public COLUMN_CLASS colClass;
        public String name;

        public Header(COLUMN_CLASS colClass, String name) {
            this.colClass = colClass;
            this.name = name;
        }
    }

    private EXEC_STATE currState;

    RFC4180CsvWriter writer;
    private List<List<String>> tableData;
    private List<Header> headerRow;
    private List<String> currRow;
    private String currRawJson;
    private ListIterator<Header> headerIterator;

    private String baseURI;
    private String appId;
    private String tableId;
    private String schemaETag;

    // TODO:
    private static int rowCount = 0;

    public FormattedCSV(int numberOfColsToMake, RFC4180CsvWriter writer, String baseURI, String appId,
                        String tableId, String schemaETag) {
        // Keep track of which CSV columns are should be processed in which way
        tableData = new LinkedList<List<String>>();
        headerRow = new ArrayList<Header>(numberOfColsToMake);

        // Initialize to accept header names and column classes
        currState = EXEC_STATE.DEFINE_HEADERS;

        this.baseURI = baseURI;
        this.appId = appId;
        this.tableId = tableId;
        this.schemaETag = schemaETag;

        this.writer = writer;
    }

    /**
     * Add a data header to the CSV
     *
     * @param colName The name of the column
     * @throws Exception
     */
    public void addHeader(String colName) throws Exception {
        if (currState != EXEC_STATE.DEFINE_HEADERS) {
            throw (new Exception("Invalid State"));
        }

        if (colName.contains("contentType")) {
            headerRow.add(new Header(COLUMN_CLASS.FILTER, colName));

            // Add nothing to the headerRow
        } else if (colName.contains("uriFragment")) {
            // Clean up the name
            int nameFinishedIndex = colName.indexOf("_image");
            if (nameFinishedIndex > 0) {
                // Add a column to hold the raw, unmodified value returned by the classifier, followed by the path to
                // the source file
                headerRow.add(new Header(COLUMN_CLASS.RAW, "raw_" + colName.substring(0, nameFinishedIndex)));
                headerRow.add(new Header(COLUMN_CLASS.IMAGE, colName));
            } else {
                headerRow.add(new Header(COLUMN_CLASS.FILE, colName));
            }

        } else {
            headerRow.add(new Header(COLUMN_CLASS.KEEP, colName));
        }

    }

    /**
     * Users can add the unfiltered, raw csv data values and this method will apply the appropriate effects
     *
     * @param value The row value
     * @param rowId The ODK row ID, which is used for URI formatting
     * @throws Exception
     */
    public void addValue(String value, String rowId) throws Exception {
        if (currState != EXEC_STATE.FILL_DATA) {
            throw (new Exception("Invalid State"));
        }

        String path;
        Header column = headerIterator.next();
        switch (column.colClass) {
            case KEEP:
                currRow.add(value);
                break;
            case FILTER:
                // We're ignoring this column, but we must add a placeholder to keep the columns aligned with the header
                currRow.add("");
                break;
            case RAW:
                // Add a placeholder until the row is finished and we can insert raw values
                currRow.add("");

                // Add the URI column data which always immediately follows the raw
                headerIterator.next();

                // Fall through to add the link formatting for the IMAGE column
            case FILE:
                path = baseURI + RESTClient.separator + appId + RESTClient.uriTablesFragment
                        + RESTClient.separator + tableId + RESTClient.uriRefFragment + schemaETag
                        + RESTClient.uriAttachmentsFragment + rowId + RESTClient.uriFileFragment
                        + value;
                currRow.add("=HYPERLINK(\"" + path + "\", \"Ctrl+Click to view\")");

                if (value.matches("raw_.*\\.json")) {
                    currRawJson = path;
                }
                break;
            // We should be handling images in the RAW case, so if we somehow hit it, an error has occurred
            case IMAGE:
            default:
                throw (new Exception("Invalid column class"));
        }
    }

    /**
     * Store the headers and prepare for row data.
     *
     * @throws Exception
     */
    public void finishedHeaders() throws Exception {
        if (currState != EXEC_STATE.DEFINE_HEADERS) {
            throw (new Exception("Invalid State"));
        }

        // Strip out the string column names from the header data structure.
        List<String> headerData = new ArrayList<String>();
        headerIterator = headerRow.listIterator();
        Header col;
        while (headerIterator.hasNext()) {
            col = headerIterator.next();
            if (col.colClass != COLUMN_CLASS.FILTER) {
                headerData.add(col.name);
            }
        }

        // Add the header row to the table
        tableData.add(headerData);
        beginRow();
    }

    /**
     * Store this row and prepare for the next.
     *
     * @throws Exception
     */
    public void finishedRow() throws Exception {
        if (currState != EXEC_STATE.FILL_DATA) {
            throw (new Exception("Invalid State"));
        }

        processRawValues();
        processFilteredValues();

        // Current row should never be null after headers are defined
        tableData.add(currRow);

        beginRow();

        // TODO:
        rowCount++;
    }

    /**
     * Reset internal state to expect data to start a new row.
     *
     * @throws Exception
     */
    private void beginRow() throws Exception {
        currState = EXEC_STATE.FILL_DATA;
        headerIterator = headerRow.listIterator();
        currRow = new ArrayList<String>(headerRow.size());
        currRawJson = null;
    }

    /**
     * Remove the columns that are marked as filtered
     *
     * @throws Exception
     */
    private void processFilteredValues() throws Exception {
        headerIterator = headerRow.listIterator();
        ListIterator<String> rowColIterator = currRow.listIterator();

        while (headerIterator.hasNext()) {
            if (!rowColIterator.hasNext()) {
                throw (new Exception("Invalid number of columns in row"));
            }

            rowColIterator.next();
            if (headerIterator.next().colClass == COLUMN_CLASS.FILTER) {
                rowColIterator.remove();
            }
        }
    }

    /**
     * Get the raw json and insert the values into the appropriate columns
     *
     * @throws Exception
     */
    private void processRawValues() throws Exception {
        WebAgent mWebAgent = new WebAgent();

        // Get the raw json file
        if (currRawJson == null) {
            return;
        }

        ResponseWrapper responseWrapper;
        try {
            Request request = new Request.Builder()
                    .url(currRawJson)
                    .header("User-Agent", "OkHttp Headers.java")
                    .addHeader("Accept", "application/json;")
                    .build();
            responseWrapper = mWebAgent.doGET(request);
        } catch (Exception e) {
            // If we can't download the raw file, just skip it
            return;
        }

        // Read the raw json file
        String json = responseWrapper.getResponse().body().string();
        FieldsWrapper obj;
        if (JSONUtils.doesJSONExists(json)) {
            if (json.startsWith("\"Unable")) {
                // File unavailable, skip it for now. TODO: Handle this case better
                return;
            }
            obj = JSONUtils.getObj(json, FieldsWrapper.class);
        } else {
            obj = new FieldsWrapper();
        }
        List<Field> rawFields = obj.getFields();

        // Transcribe the raw fields into a map
        Map<String, String> rawFieldsMap = new HashMap<String, String>(currRow.size());
        Iterator<Field> rawFieldsIterator = rawFields.iterator();
        while (rawFieldsIterator.hasNext()) {
            Field rawField = rawFieldsIterator.next();
            rawFieldsMap.put(rawField.getLabel(), rawField.getValue());
        }

        // Iterate through the current row and insert the raw values
        headerIterator = headerRow.listIterator();
        Header col;
        while (headerIterator.hasNext()) {
            col = headerIterator.next();

            if (col.colClass != COLUMN_CLASS.RAW) {
                continue;
            }

            // Find the raw value (the 4 character prefix is "raw_")
            String colName = col.name.substring(4);
            if (!rawFieldsMap.containsKey(colName)) {
                throw (new Exception("Error processing raw fields")) ;
            }

            currRow.set(headerIterator.previousIndex(), rawFieldsMap.get(colName));
        }
    }

    /**
     * Write out the table to the csv file.
     *
     * @throws Exception
     */
    public void writeCSV() throws Exception {
        Iterator<List<String>> rowIterator = tableData.iterator();

        List<String> row;
        while (rowIterator.hasNext()) {
           row = rowIterator.next();
           if (row != null) {
               writer.writeNext(row.toArray(new String[row.size()]));
           }
        }
    }
}
