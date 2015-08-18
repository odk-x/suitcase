package net;

import com.squareup.okhttp.Request;
import model.FormattedCSV;
import model.ResponseWrapper;
import model.serialization.FieldsWrapper;
import model.serialization.RowsData;
import model.serialization.TableInfo;
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
import utils.JSONUtils;

import javax.swing.*;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/19/15
 * Time: 11:27 AM
 */
public class RESTClient {
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

    private JTextArea outputText;

    private WinkClient mWinkClient;
    private String schemaETag;

    // Server data
    public static String aggregateURL;
    public static String appId;
    public static String tableId;

    /************************************** Legacy *************************************/
    public static final String PREFIX_PATH = "odktables";
    public static final String REF = "/ref";
    public static final String ROWS = "/rows";
    public static final String TABLES = "/tables";

    public static String URL;

    private WebAgent mWebAgent;

    public RESTClient() {
        mWinkClient = new WinkClient();

        // Legacy
        mWebAgent = new WebAgent();
    }

    public void setOutputText(JTextArea outputText) {
        this.outputText = outputText;
    }

    public void resetData(String aggregateURL, String appId, String tableId, String dirToSaveDataTo) throws Exception {
        FileUtils.deleteDirectory(new File(dirToSaveDataTo));

        this.aggregateURL = aggregateURL;
        if (!this.aggregateURL.endsWith(File.separator)) {
            this.aggregateURL += File.separator;
        }
        this.aggregateURL += PREFIX_PATH;

        this.appId = appId;
        this.tableId = tableId;

        // Legacy
        this.URL = aggregateURL + File.separator + PREFIX_PATH + File.separator + appId;

        schemaETag = WinkClient.getSchemaETagForTable(this.aggregateURL, this.appId, this.tableId);
    }

    public void downloadDefinitions(String dirToSaveDataTo) throws Exception {
        outputText.append("\nDownloading definitions");

        // Get all Table Level Files
        mWinkClient.getAllTableLevelFilesFromUri(aggregateURL, appId, tableId, dirToSaveDataTo);

        // Write out Table Definition CSV's
        String tableDefinitionCSVPath = dirToSaveDataTo + separator + "tables" + separator + tableId
                + separator + "definition.csv";
        mWinkClient.writeTableDefinitionToCSV(aggregateURL, appId, tableId, schemaETag, tableDefinitionCSVPath);

        outputText.append("\nDownload complete\n");
    }

    public void downloadRawCSV(String dirToSaveDataTo) throws Exception {
        outputText.append("\nDownloading unformatted CSV file");

        // Write out the Table Data CSV's
        String dataCSVPath = dirToSaveDataTo + separator + "assets" + separator + "csv" + separator
                + tableId + "_raw.csv";

        mWinkClient.writeRowDataToCSV(aggregateURL, appId, tableId, schemaETag, dataCSVPath);

        outputText.append("\nDownload complete\n");
    }

    public void downloadFormattedCSV(String dirToSaveDataTo) throws Exception {
        outputText.append("\nDownloading Excel Formatted CSV file");

        // Write out the Table Data CSV's
        String dataCSVPath = dirToSaveDataTo + separator + "assets" + separator + "csv" + separator
                + tableId + "_formatted.csv";

        RFC4180CsvWriter writer;
        JSONObject rowWrapper;
        String resumeCursor = null;

        outputText.append("\n\tRetrieving column names");

        rowWrapper = mWinkClient.getRows(aggregateURL, appId, tableId, schemaETag, resumeCursor, defaultFetchLimit);

        JSONArray rows = rowWrapper.getJSONArray(jsonRowsString);

        if (rows.size() <= 0) {
            outputText.append("\nwriteRowDataToCSV: There are no rows to write out!");
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
        FormattedCSV csv = new FormattedCSV(numberOfColsToMake, writer, aggregateURL, appId, tableId, schemaETag);

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
                csv.filterCol();
            } else if (colName.contains("uriFragment")) {
                csv.uriCol(colName);
            } else {
                csv.addHeader(colName);
            }
        }

        csv.addHeader(rowDefRowETag);
        csv.addHeader(rowDefFilterType);
        csv.addHeader(rowDefFilterValue);

        outputText.append("\n\tColumn names retrieved");

        outputText.append("\n\tWriting column names to CSV");
        csv.writeHeaders();

        do {
            outputText.append("\n\tRetrieving the next " + defaultFetchLimit + " rows.");

            rowWrapper = mWinkClient.getRows(aggregateURL, appId, tableId, schemaETag, resumeCursor, defaultFetchLimit);

            rows = rowWrapper.getJSONArray(jsonRowsString);

            outputText.append("\n\tWriting fetched rows.");

            writeOutFetchLimitRows(writer, rows, csv);

            resumeCursor = rowWrapper.getString("webSafeResumeCursor");

        } while (rowWrapper.getBoolean("hasMoreResults"));

        outputText.append("\nDownload complete\n");

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
        outputText.append("\nDownloading instance files");
        // Get all Instance Files
        // get all rows - check for attachment
        mWinkClient.getAllTableInstanceFilesFromUri(aggregateURL, appId, tableId, schemaETag, dirToSaveDataTo);
        outputText.append("\nDownload complete\n");
    }


    /*************************************** Legacy **************************************/
    public TableInfo getTableResource() throws IOException, org.json.JSONException {
        Request request = new Request.Builder()
                .url(URL + TABLES + File.separator + tableId)
                .header("User-Agent", "OkHttp Headers.java")
                .addHeader("Accept", "application/json;")
                .build();
        ResponseWrapper responseWrapper = mWebAgent.doGET(request);
        return JSONUtils.getObj(responseWrapper.getResponse().body().string(), TableInfo.class);
    }

    public RowsData getAllDataRows(String schemaTag) throws IOException, org.json.JSONException {
        Request request = new Request.Builder()
                .url(URL + TABLES + File.separator + tableId + REF + File.separator + schemaTag + ROWS)
                .header("User-Agent", "OkHttp Headers.java")
                .addHeader("Accept", "application/json;")
                .build();
        ResponseWrapper responseWrapper = mWebAgent.doGET(request);
        return JSONUtils.getObj(responseWrapper.getResponse().body().string(), RowsData.class);
    }

    public FieldsWrapper getRawJSONValue(String fullURL) throws IOException, org.json.JSONException {
        Request request = new Request.Builder()
                .url(fullURL)
                .header("User-Agent", "OkHttp Headers.java")
                .addHeader("Accept", "application/json;")
                .build();
        ResponseWrapper responseWrapper = mWebAgent.doGET(request);
        String json = responseWrapper.getResponse().body().string();
        FieldsWrapper obj;
        if (JSONUtils.doesJSONExists(json)) {
            obj = JSONUtils.getObj(json, FieldsWrapper.class);
        } else {
            obj = new FieldsWrapper();
        }
        return obj;
    }
}
