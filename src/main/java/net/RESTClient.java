package net;

//import com.squareup.okhttp.Request;
import model.AggregateTableInfo;
//import model.FormattedCSV;
//import model.ResponseWrapper;
//import model.serialization.FieldsWrapper;
//import model.serialization.RowsData;
//import model.serialization.TableInfo;
import model.ODKCSV;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;

import org.opendatakit.wink.client.WinkClient;

import org.opendatakit.aggregate.odktables.rest.RFC4180CsvWriter;
//import utils.JSONUtils;

import javax.swing.*;

import static org.opendatakit.wink.client.WinkClient.*;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/19/15
 * Time: 11:27 AM
 */
public class RESTClient {
    // Formatting and protocol constants
//    public static String separator = "/";
//    public static String jsonRowsString = "rows";
//    public static String jsonId = "id";
//    public static String jsonFormId = "formId";
//    public static String jsonLocale = "locale";
//    public static String jsonSavepointType = "savepointType";
//    public static String jsonSavepointTimestamp = "savepointTimestamp";
//    public static String jsonSavepointCreator = "savepointCreator";
//    public static String jsonFilterScope = "filterScope";
//    public static String orderedColumnsDef = "orderedColumns";
//    public static String rowDefId = "_id";
//    public static String rowDefFormId = "_form_id";
//    public static String rowDefLocale = "_locale";
//    public static String rowDefSavepointType = "_savepoint_type";
//    public static String rowDefSavepointTimestamp = "_savepoint_timestamp";
//    public static String rowDefSavepointCreator = "_savepoint_creator";
//    public static String rowDefRowETag = "_row_etag";
//    public static String rowDefFilterType = "_filter_type";
//    public static String rowDefFilterValue = "_filter_value";
//    public static String uriTablesFragment = "/tables";
//    public static String uriRefFragment = "/ref/";
//    public static String uriAttachmentsFragment = "/attachments/";
//    public static String uriFileFragment = "/file/";
//
//    public static String defaultFetchLimit = "1000";

    private JTextArea outputText;

    private WinkClient odkWinkClient;
    private String csvPath;

    /* MY FIELDS */
    private AggregateTableInfo tableInfo;
    private AttachmentManager attMngr;
    private ODKCSV csv;

    public static final String CSV_NAME = "data.csv";
    private static final int FETCH_LIMIT = 1000;

    /************************************** Legacy *************************************/
//    public static final String REF = "/ref";
//    public static final String ROWS = "/rows";
//    public static final String TABLES = "/tables";
//    public static final String FETCH = "?fetchLimit=";

//    public static String URL;

//    private WebAgent mWebAgent;

    //!!!ATTENTION!!! One per table
    public RESTClient(AggregateTableInfo tableInfo) throws JSONException {
        this.tableInfo = tableInfo;
        tableInfo.setSchemaETag(WinkClient.getSchemaETagForTable(
                this.tableInfo.getServerUrl(),
                this.tableInfo.getAppId(),
                this.tableInfo.getTableId()));

        this.odkWinkClient = new WinkClient();
        this.attMngr = new AttachmentManager(this.tableInfo, this.odkWinkClient);
        this.csv = new ODKCSV(this.attMngr, this.tableInfo);
        retrieveRows(FETCH_LIMIT);
    }

    /* NEW */
    public void writeCSVToFile(boolean scanFormatting, boolean localLink) throws IOException {
        String csvFilename =
                (localLink ? "data" : "link") +
                (scanFormatting ? "_formatted" : "") + ".csv";

        RFC4180CsvWriter csvWriter = new RFC4180CsvWriter(new FileWriter(
                utils.FileUtils.getCSVPath(this.tableInfo, scanFormatting, localLink).toAbsolutePath().toString()
        ));

        ODKCSV.ODKCSVIterator csvIt = this.csv.getODKCSVIterator();
        csvWriter.writeNext(this.csv.getHeader(scanFormatting));
        int i = 0;
        while (csvIt.hasNext()) {
            String[] nextline = csvIt.next(scanFormatting, localLink);
//            System.out.println("RESTClient: Index is " + i++);
//            System.out.println("RESTClient: nextline size is " + nextline.length);
//            System.out.println("RESTClient: First item is " + nextline[0]);
            csvWriter.writeNext(nextline);
        }
        csvWriter.close();
    }

    /* NEW */
    private void retrieveRows(int limit) throws JSONException {
        String cursor = null;
        JSONObject rows;

        do {
            rows = this.odkWinkClient.getRows(
                    this.tableInfo.getServerUrl(),
                    this.tableInfo.getAppId(),
                    this.tableInfo.getTableId(),
                    this.tableInfo.getSchemaETag(),
                    cursor, Integer.toString(limit));

            cursor = rows.optString("webSafeResumeCursor");
            this.csv.tryAdd(rows.getJSONArray(jsonRowsString));
        } while (rows.getBoolean("hasMoreResults"));
    }

//
//    public void setOutputText(JTextArea outputText) {
//        this.outputText = outputText;
//    }
//
//    public void resetData(String aggregateURL, String appId, String tableId, String dirToSaveDataTo) throws Exception {
//        FileUtils.deleteDirectory(new File(dirToSaveDataTo));
//
////        this.aggregateURL = aggregateURL;
////        if (!this.aggregateURL.endsWith(File.separator)) {
////            this.aggregateURL += File.separator;
////        }
////        this.aggregateURL += AGGREGATE_URL_POSTFIX;
////
////        this.appId = appId;
////        this.tableId = tableId;
////
////        // Legacy
//////        this.URL = aggregateURL + File.separator + AGGREGATE_URL_POSTFIX + File.separator + appId;
////
////        schemaETag = WinkClient.getSchemaETagForTable(this.aggregateURL, this.appId, this.tableId);
//    }
//
//    public void downloadDefinitions(String dirToSaveDataTo) throws Exception {
////        outputText.append("\nDownloading definitions");
////
////        // Get all Table Level Files
////        odkWinkClient.getAllTableLevelFilesFromUri(aggregateURL, appId, tableId, dirToSaveDataTo);
////
////        // Write out Table Definition CSV's
////        String tableDefinitionCSVPath = dirToSaveDataTo + separator + "tables" + separator + tableId
////                + separator + "definition.csv";
////        odkWinkClient.writeTableDefinitionToCSV(aggregateURL, appId, tableId, schemaETag, tableDefinitionCSVPath);
////
////        outputText.append("\nDownload complete\n");
//    }
//
//    @Deprecated //TODO: Remove
//    public void downloadRawCSV() throws Exception {
//        outputText.append("\nDownloading unformatted CSV file");
//
//        // Write out the Table Data CSV's
////        String dataCSVPath = dirToSaveDataTo + "/" + tableId + "/" + CSV_NAME;
//
//        odkWinkClient.writeRowDataToCSV(aggregateURL, appId, tableId, schemaETag, this.csvPath);
//
//        outputText.append("\nDownload complete\n");
//    }
//
//    public void downloadFormattedCSV() throws Exception {
//        outputText.append("\nDownloading Excel Formatted CSV file");
//
//        // Write out the Table Data CSV's
////        String dataCSVPath = dirToSaveDataTo + "/" + "assets" + "/" + "csv" + "/" + tableId + "_formatted.csv";
//
//        RFC4180CsvWriter writer;
//        JSONObject rowWrapper;
//        String resumeCursor = null;
//
//        outputText.append("\n\tRetrieving column names");
//
//        rowWrapper = odkWinkClient.getRows(aggregateURL, appId, tableId, schemaETag, resumeCursor, defaultFetchLimit);
//
//        JSONArray rows = rowWrapper.getJSONArray(jsonRowsString);
//
//        if (rows.size() <= 0) {
//            outputText.append("\nwriteRowDataToCSV: There are no rows to write out!");
//            return;
//        }
//
//        File file = new File(dataCSVPath);
//        file.getParentFile().mkdirs();
//        if (!file.exists()) {
//            file.createNewFile();
//        }
//
//        // This fileWriter could be causing the issue with
//        // UTF-8 characters - should probably use an OutputStream
//        // here instead
//        FileWriter fw = new FileWriter(file.getAbsoluteFile());
//        writer = new RFC4180CsvWriter(fw);
//
//        JSONObject repRow = rows.getJSONObject(0);
//        JSONArray orderedColumnsRep = repRow.getJSONArray(orderedColumnsDef);
//        int numberOfColsToMake = 9 + orderedColumnsRep.size();
//        FormattedCSV csv = new FormattedCSV(numberOfColsToMake, writer, aggregateURL, appId, tableId, schemaETag);
//
//        csv.addHeader(rowDefId);
//        csv.addHeader(rowDefFormId);
//        csv.addHeader(rowDefLocale);
//        csv.addHeader(rowDefSavepointType);
//        csv.addHeader(rowDefSavepointTimestamp);
//        csv.addHeader(rowDefSavepointCreator);
//
//        for (int j = 0; j < orderedColumnsRep.size(); j++) {
//            JSONObject obj = orderedColumnsRep.getJSONObject(j);
//            csv.addHeader(obj.getString("column"));
//        }
//
//        csv.addHeader(rowDefRowETag);
//        csv.addHeader(rowDefFilterType);
//        csv.addHeader(rowDefFilterValue);
//
//        outputText.append("\n\tColumn names retrieved");
//
//        outputText.append("\n\tWriting column names to CSV");
//        csv.finishedHeaders();
//
//        do {
//            outputText.append("\n\tRetrieving the next " + defaultFetchLimit + " rows.");
//
//            rowWrapper = odkWinkClient.getRows(aggregateURL, appId, tableId, schemaETag, resumeCursor, defaultFetchLimit);
//
//            rows = rowWrapper.getJSONArray(jsonRowsString);
//
//            outputText.append("\n\tWriting fetched rows.");
//
//            writeOutFetchLimitRows(writer, rows, csv);
//
//            resumeCursor = rowWrapper.getString("webSafeResumeCursor");
//
//        } while (rowWrapper.getBoolean("hasMoreResults"));
//
//        outputText.append("\nDownload complete\n");
//
//        writer.close();
//    }
//
//    private void writeOutFetchLimitRows(RFC4180CsvWriter writer, JSONArray rows, FormattedCSV csv) throws Exception {
//        String nullString = "null";
//
//        for (int k = 0; k < rows.size(); k++) {
//            JSONObject row = rows.getJSONObject(k);
//            csv.addValue(row.getString(jsonId), row.getString(jsonId));
//            String formId = nullString;
//            if (!row.isNull(jsonFormId)) {
//                formId = row.getString(jsonFormId);
//            }
//            csv.addValue(formId, row.getString(jsonId));
//            csv.addValue(row.getString(jsonLocale), row.getString(jsonId));
//            csv.addValue(row.getString(jsonSavepointType), row.getString(jsonId));
//            csv.addValue(row.getString(jsonSavepointTimestamp), row.getString(jsonId));
//
//            String creator = nullString;
//            if (!row.isNull(jsonSavepointCreator)) {
//                creator = row.getString(jsonSavepointCreator);
//            }
//            csv.addValue(creator, row.getString(jsonId));
//
//            JSONArray rowsOrderedCols = row.getJSONArray(orderedColumnsDef);
//
//            for (int l = 0; l < rowsOrderedCols.size(); l++) {
//                JSONObject col = rowsOrderedCols.getJSONObject(l);
//                if (col.isNull("value")) {
//                    csv.addValue(nullString, row.getString(jsonId));
//                } else {
//                    csv.addValue(col.getString("value"), row.getString(jsonId));
//                }
//            }
//
//            csv.addValue(nullString, row.getString(jsonId));
//            JSONObject filterScope = row.getJSONObject(jsonFilterScope);
//            csv.addValue(filterScope.getString("type"), row.getString(jsonId));
//            if (filterScope.isNull("value")) {
//                csv.addValue(nullString, row.getString(jsonId));
//            } else {
//                csv.addValue(filterScope.getString("value"), row.getString(jsonId));
//            }
//            csv.finishedRow();
//        }
//
//        csv.writeCSV();
//    }
//
//    public void downloadAttachments(String dirToSaveDataTo) throws Exception {
//        //TODO: use executorservice to download in parallel
//        outputText.append("\nDownloading instance files");
//        // Get all Instance Files
//        // get all rows - check for attachment
//        odkWinkClient.getAllTableInstanceFilesFromUri(aggregateURL, appId, tableId, schemaETag, dirToSaveDataTo);
//        outputText.append("\nDownload complete\n");
//    }
//
//
//    /*************************************** Legacy **************************************/
////    public TableInfo getTableResource() throws IOException, org.json.JSONException {
////        Request request = new Request.Builder()
////                .url(URL + TABLES + File.separator + tableId)
////                .header("User-Agent", "OkHttp Headers.java")
////                .addHeader("Accept", "application/json;")
////                .build();
////        ResponseWrapper responseWrapper = mWebAgent.doGET(request);
////        return JSONUtils.getObj(responseWrapper.getResponse().body().string(), TableInfo.class);
////    }
////
////    public RowsData getAllDataRows(String schemaTag, int numRowsToFetch) throws IOException, org.json.JSONException {
////        Request request = new Request.Builder()
////                .url(URL + TABLES + File.separator + tableId + REF + File.separator + schemaTag + ROWS + FETCH + numRowsToFetch)
////                .header("User-Agent", "OkHttp Headers.java")
////                .addHeader("Accept", "application/json;")
////                .build();
////        ResponseWrapper responseWrapper = mWebAgent.doGET(request);
////        return JSONUtils.getObj(responseWrapper.getResponse().body().string(), RowsData.class);
////    }
////
////    public FieldsWrapper getRawJSONValue(String fullURL) throws IOException, org.json.JSONException {
////        Request request = new Request.Builder()
////                .url(fullURL)
////                .header("User-Agent", "OkHttp Headers.java")
////                .addHeader("Accept", "application/json;")
////                .build();
////        ResponseWrapper responseWrapper = mWebAgent.doGET(request);
////        String json = responseWrapper.getResponse().body().string();
////        FieldsWrapper obj;
////        if (JSONUtils.doesJSONExists(json)) {
////            obj = JSONUtils.getObj(json, FieldsWrapper.class);
////        } else {
////            obj = new FieldsWrapper();
////        }
////        return obj;
////    }
}
