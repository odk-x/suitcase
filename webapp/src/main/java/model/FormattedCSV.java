package model;

import net.RESTClient;
import org.opendatakit.aggregate.odktables.rest.RFC4180CsvWriter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by jbeorse on 8/5/15.
 */
public class FormattedCSV {

    private enum EXEC_STATE {
        DEFINE_HEADERS,
        FILL_DATA
    }

    private enum COLUMN_CLASS {
       KEEP,
        FILTER,
        URI
    }

    private EXEC_STATE currState;

    RFC4180CsvWriter writer;
    private Map<Integer, COLUMN_CLASS> columnClassMap;
    private String[] csvArr;

    private int csvIndex;
    private int mapIndex;

    private String  baseURI;
    private String appId;
    private String tableId;
    private String schemaETag;

    public FormattedCSV (int numberOfColsToMake, RFC4180CsvWriter writer, String baseURI, String appId,
                         String tableId, String schemaETag) {
        columnClassMap = new HashMap<Integer, COLUMN_CLASS>();
        mapIndex = 0;

        csvArr = new String[numberOfColsToMake];
        csvIndex = 0;

        currState = EXEC_STATE.DEFINE_HEADERS;

        this.baseURI = baseURI;
        this.appId = appId;
        this.tableId = tableId;
        this.schemaETag = schemaETag;

        this.writer = writer;
    }

    public void addHeader(String value) throws Exception {
        if (currState != EXEC_STATE.DEFINE_HEADERS) {
            throw (new Exception("Invalid State"));
        }

        columnClassMap.put(mapIndex++, COLUMN_CLASS.KEEP);
        csvArr[csvIndex++] = value;
    }

    public void filterCol() throws Exception {
        if (currState != EXEC_STATE.DEFINE_HEADERS) {
            throw (new Exception("Invalid State"));
        }

        columnClassMap.put(mapIndex++, COLUMN_CLASS.FILTER);
    }

    public void uriCol() throws Exception {
        if (currState != EXEC_STATE.DEFINE_HEADERS) {
            throw (new Exception("Invalid State"));
        }

        columnClassMap.put(mapIndex++, COLUMN_CLASS.URI);
    }

    public void addValue(String value, String rowId) throws Exception {
        if (currState != EXEC_STATE.FILL_DATA) {
            throw (new Exception("Invalid State"));
        }

        COLUMN_CLASS colClass = columnClassMap.get(mapIndex++);
        switch (colClass) {
            case KEEP:
                csvArr[csvIndex++] = value;
                break;
            case FILTER:
                // Do nothing; we're ignoring this column
                break;
            case URI:
                // TODO: Add URI formatting
                String path = baseURI + RESTClient.separator + appId + RESTClient.uriTablesFragment
                        + RESTClient.separator + tableId + RESTClient.uriRefFragment + schemaETag
                        + RESTClient.uriAttachmentsFragment + rowId + RESTClient.uriFileFragment
                        + value;
                csvArr[csvIndex++] = "=HYPERLINK(\"" + path + "\", \"Ctrl+Click to view\")";
                break;
            default:
                throw (new Exception("Unknown column class"));
        }
    }

    public void writeHeaders() throws Exception {
        if (currState != EXEC_STATE.DEFINE_HEADERS) {
            throw (new Exception("Invalid State"));
        }

        writer.writeNext(csvArr);
        beginRow();
    }

    public void writeRow() throws Exception {
        if (currState != EXEC_STATE.FILL_DATA) {
            throw (new Exception("Invalid State"));
        }

        writer.writeNext(csvArr);
        beginRow();
    }

    private void beginRow() throws Exception {
        currState = EXEC_STATE.FILL_DATA;
        mapIndex = 0;
        csvIndex = 0;
        csvArr = new String[csvArr.length];
    }
}
