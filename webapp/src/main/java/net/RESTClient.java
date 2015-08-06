package net;

import model.FormattedCSV;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;

import org.opendatakit.wink.client.WinkClient;

import org.opendatakit.aggregate.odktables.rest.RFC4180CsvReader;
import org.opendatakit.aggregate.odktables.rest.RFC4180CsvWriter;
import org.opendatakit.aggregate.odktables.rest.entity.Row;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/19/15
 * Time: 11:27 AM
 */
public class RESTClient {
    // Server data
    public static final String AGGREGATE_URL = "https://vraggregate2.appspot.com/odktables";
    public static final String APP_ID = "tables";
    public static final String TABLE_ID = "scan_MNH_Register1";

    // Formatting and protocol constants
    public static String separator = "/";
    public static String jsonRowsString = "rows";
    public static String jsonId = "id";
    public static String jsonFormId = "formId";
    public static String jsonLocale = "locale";
    public static String jsonSavepointType = "savepointType";
    public static String jsonSavepointTimestamp = "savepointTimestamp";
    public static String jsonSavepointCreator = "savepointCreator";
    public static String jsonFilterScope = "filterScope";
    public static String orderedColumnsDef = "orderedColumns";
    public static String rowDefId = "_id";
    public static String rowDefFormId = "_form_id";
    public static String rowDefLocale = "_locale";
    public static String rowDefSavepointType = "_savepoint_type";
    public static String rowDefSavepointTimestamp = "_savepoint_timestamp";
    public static String rowDefSavepointCreator = "_savepoint_creator";
    public static String rowDefRowETag = "_row_etag";
    public static String rowDefFilterType = "_filter_type";
    public static String rowDefFilterValue = "_filter_value";
    public static String uriTablesFragment = "/tables";
    public static String uriRefFragment = "/ref/";
    public static String uriAttachmentsFragment = "/attachments/";
    public static String uriFileFragment = "/file/";

    public static String defaultFetchLimit = "1000";

    private WinkClient mWinkClient;
    private String schemaETag;

    public RESTClient() {
        mWinkClient = new WinkClient();
        schemaETag = WinkClient.getSchemaETagForTable(AGGREGATE_URL, APP_ID, TABLE_ID);
    }

    public void resetData(String dirToSaveDataTo) throws Exception {
        FileUtils.deleteDirectory(new File(dirToSaveDataTo));

        schemaETag = WinkClient.getSchemaETagForTable(AGGREGATE_URL, APP_ID, TABLE_ID);
    }

    public void fetchRows(int numRows) throws Exception {
        JSONObject rowWrapper;
        String resumeCursor = null;

        rowWrapper = mWinkClient.getRows(AGGREGATE_URL, APP_ID, TABLE_ID, schemaETag, resumeCursor, "1000");
        JSONArray rows = rowWrapper.getJSONArray(jsonRowsString);

        if (resumeCursor != null) {
            System.out.println("CURSOR: " + resumeCursor);
        } else {
            System.out.println("CURSOR IS NULL");
        }

        JSONObject repRow = rows.getJSONObject(0);
        JSONArray orderedColumnsRep = repRow.getJSONArray(orderedColumnsDef);
        int numberOfColsToMake = 9 + orderedColumnsRep.size();
        String[] colArray = new String[numberOfColsToMake];

        int i = 0;
        colArray[i++] = rowDefId;
        colArray[i++] = rowDefFormId;
        colArray[i++] = rowDefLocale;
        colArray[i++] = rowDefSavepointType;
        colArray[i++] = rowDefSavepointTimestamp;
        colArray[i++] = rowDefSavepointCreator;

        for (int j = 0; j < orderedColumnsRep.size(); j++) {
            JSONObject obj = orderedColumnsRep.getJSONObject(j);
            colArray[i++] = obj.getString("column");
        }

        colArray[i++] = rowDefRowETag;
        colArray[i++] = rowDefFilterType;
        colArray[i++] = rowDefFilterValue;
    }

    public void downloadDefinitions(String dirToSaveDataTo) throws Exception {
        // Get all Table Level Files
        mWinkClient.getAllTableLevelFilesFromUri(AGGREGATE_URL, APP_ID, TABLE_ID, dirToSaveDataTo);

        // Write out Table Definition CSV's
        String tableDefinitionCSVPath = dirToSaveDataTo + separator + "tables" + separator + TABLE_ID
                + separator + "definition.csv";
        mWinkClient.writeTableDefinitionToCSV(AGGREGATE_URL, APP_ID, TABLE_ID, schemaETag, tableDefinitionCSVPath);
    }

    public void downloadRawCSV(String dirToSaveDataTo) throws Exception {
        // Write out the Table Data CSV's
        String dataCSVPath = dirToSaveDataTo + separator + "assets" + separator + "csv" + separator
                + TABLE_ID + "_raw.csv";

        // TODO: Redo this without the fetch limit
        mWinkClient.writeRowDataToCSV(AGGREGATE_URL, APP_ID, TABLE_ID, schemaETag, dataCSVPath);
    }

    public void downloadFormattedCSV(String dirToSaveDataTo) throws Exception {
        // Write out the Table Data CSV's
        String dataCSVPath = dirToSaveDataTo + separator + "assets" + separator + "csv" + separator
                + TABLE_ID + "_formatted.csv";

        // TODO: Redo this without the fetch limit
        RFC4180CsvWriter writer;
        JSONObject rowWrapper;
        String resumeCursor = null;

        rowWrapper = mWinkClient.getRows(AGGREGATE_URL, APP_ID, TABLE_ID, schemaETag, resumeCursor, defaultFetchLimit);

        JSONArray rows = rowWrapper.getJSONArray(jsonRowsString);

        if (rows.size() <= 0) {
            System.out.println("writeRowDataToCSV: There are no rows to write out!");
            return;
        }

        File file = new File(dataCSVPath);
        file.getParentFile().mkdirs();
        if (!file.exists()) {
            file.createNewFile();
        }

        // This fileWriter could be causing the issue with
        // UTF-8 characters - should probably use an OutputStream
        // here instead
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        writer = new RFC4180CsvWriter(fw);

        JSONObject repRow = rows.getJSONObject(0);
        JSONArray orderedColumnsRep = repRow.getJSONArray(orderedColumnsDef);
        int numberOfColsToMake = 9 + orderedColumnsRep.size();
        FormattedCSV csv = new FormattedCSV(numberOfColsToMake, writer, AGGREGATE_URL, APP_ID, TABLE_ID, schemaETag);

        csv.addHeader(rowDefId);
        csv.addHeader(rowDefFormId);
        csv.addHeader(rowDefLocale);
        csv.addHeader(rowDefSavepointType);
        csv.addHeader(rowDefSavepointTimestamp);
        csv.addHeader(rowDefSavepointCreator);

        // TODO: Clean this up
        String colName;
        for (int j = 0; j < orderedColumnsRep.size(); j++) {
            JSONObject obj = orderedColumnsRep.getJSONObject(j);
            colName = obj.getString("column");
            if (colName.contains("contentType")) {
                System.out.println("Filter " + colName);
                csv.filterCol();
            } else if (colName.contains("uriFragment")) {
                System.out.println("URI " + colName);
                csv.uriCol();
            } else {
                csv.addHeader(colName);
            }
        }

        csv.addHeader(rowDefRowETag);
        csv.addHeader(rowDefFilterType);
        csv.addHeader(rowDefFilterValue);

        csv.writeHeaders();

        do {
            rowWrapper = mWinkClient.getRows(AGGREGATE_URL, APP_ID, TABLE_ID, schemaETag, resumeCursor, defaultFetchLimit);

            rows = rowWrapper.getJSONArray(jsonRowsString);

            writeOutFetchLimitRows(writer, rows, csv);

            resumeCursor = rowWrapper.getString("webSafeResumeCursor");

        } while (rowWrapper.getBoolean("hasMoreResults"));

        writer.close();
    }

    private void writeOutFetchLimitRows(RFC4180CsvWriter writer, JSONArray rows, FormattedCSV csv) throws Exception {
        String nullString = null;

        for (int k = 0; k < rows.size(); k++) {
            JSONObject row = rows.getJSONObject(k);
            csv.addValue(row.getString(jsonId), row.getString(jsonId));
            String formId = nullString;
            if (!row.isNull(jsonFormId)) {
                formId = row.getString(jsonFormId);
            }
            csv.addValue(formId, row.getString(jsonId));
            csv.addValue(row.getString(jsonLocale), row.getString(jsonId));
            csv.addValue(row.getString(jsonSavepointType), row.getString(jsonId));
            csv.addValue(row.getString(jsonSavepointTimestamp), row.getString(jsonId));

            String creator = nullString;
            if (!row.isNull(jsonSavepointCreator)) {
                creator = row.getString(jsonSavepointCreator);
            }
            csv.addValue(creator, row.getString(jsonId));

            JSONArray rowsOrderedCols = row.getJSONArray(orderedColumnsDef);

            for (int l = 0; l < rowsOrderedCols.size(); l++) {
                JSONObject col = rowsOrderedCols.getJSONObject(l);
                if (col.isNull("value")) {
                    csv.addValue(nullString, row.getString(jsonId));
                } else {
                    csv.addValue(col.getString("value"), row.getString(jsonId));
                }
            }

            csv.addValue(nullString, row.getString(jsonId));
            JSONObject filterScope = row.getJSONObject(jsonFilterScope);
            csv.addValue(filterScope.getString("type"), row.getString(jsonId));
            if (filterScope.isNull("value")) {
                csv.addValue(nullString, row.getString(jsonId));
            } else {
                csv.addValue(filterScope.getString("value"), row.getString(jsonId));
            }
            csv.writeRow();
        }
    }

    public void downloadAttachments(String dirToSaveDataTo) throws Exception {
        // Get all Instance Files
        // get all rows - check for attachment
        mWinkClient.getAllTableInstanceFilesFromUri(AGGREGATE_URL, APP_ID, TABLE_ID, schemaETag, dirToSaveDataTo);
    }
}
