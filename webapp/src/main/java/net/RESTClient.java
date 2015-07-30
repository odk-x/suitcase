package net;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.client.RestClient;
import org.apache.wink.client.Resource;

import org.opendatakit.wink.client.WinkClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/19/15
 * Time: 11:27 AM
 */
public class RESTClient {
    public static final String AGGREGATE_URL = "https://vraggregate2.appspot.com/odktables";
    public static final String APP_ID = "tables";
    public static final String TABLE_ID = "scan_MNH_Register1";

    public static String separator = "/";

    private WinkClient mWinkClient;

    public RESTClient() {
        mWinkClient = new WinkClient();
    }

    public void downloadData(String dirToSaveDataTo) throws Exception {
        JSONObject table = mWinkClient.getTable(AGGREGATE_URL, APP_ID, TABLE_ID);
        String schemaETag = table.getString("schemaETag");

        // Get all Table Level Files
        mWinkClient.getAllTableLevelFilesFromUri(AGGREGATE_URL, APP_ID, TABLE_ID, dirToSaveDataTo);

        // Write out Table Definition CSV's
        String tableDefinitionCSVPath = dirToSaveDataTo + separator + "tables" + separator + TABLE_ID
                + separator + "definition.csv";
        mWinkClient.writeTableDefinitionToCSV(AGGREGATE_URL, APP_ID, TABLE_ID, schemaETag, tableDefinitionCSVPath);

        // Write out the Table Data CSV's
        String dataCSVPath = dirToSaveDataTo + separator + "assets" + separator + "csv" + separator
                + TABLE_ID + ".csv";
        mWinkClient.writeRowDataToCSV(AGGREGATE_URL, APP_ID, TABLE_ID, schemaETag, dataCSVPath);

        // Get all Instance Files
        // get all rows - check for attachment
        //mWinkClient.getAllTableInstanceFilesFromUri(AGGREGATE_URL, APP_ID, TABLE_ID, schemaETag, dirToSaveDataTo);
    }

    /* TODO
    public TableInfo getTableResource() throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(URL + TABLES + File.separator + TABLE_ID)
                .header("User-Agent", "OkHttp Headers.java")
                .addHeader("Accept", "application/json;")
                .build();
        ResponseWrapper responseWrapper = mWebAgent.doGET(request);
        return JSONUtils.getObj(responseWrapper.getResponse().body().string(), TableInfo.class);
    }

    public RowsData getAllDataRows(String schemaTag) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(URL + TABLES + File.separator + TABLE_ID + REF + File.separator + schemaTag + ROWS)
                .header("User-Agent", "OkHttp Headers.java")
                .addHeader("Accept", "application/json;")
                .build();
        ResponseWrapper responseWrapper = mWebAgent.doGET(request);
        return JSONUtils.getObj(responseWrapper.getResponse().body().string(), RowsData.class);
    }

    public FieldsWrapper getRawJSONValue(String fullURL) throws IOException, JSONException {
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
    */
}
