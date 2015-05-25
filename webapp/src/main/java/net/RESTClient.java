package net;

import com.squareup.okhttp.Request;
import model.ResponseWrapper;
import model.serialization.RowsData;
import model.serialization.TableInfo;
import org.json.JSONException;
import utils.JSONUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/19/15
 * Time: 11:27 AM
 */
public class RESTClient {
    public static final String AGGREGATE_URL = "https://vraggregate2.appspot.com";
    public static final String APP_ID = "tables";
    public static final String TABLE_ID = "scan_MNH_Register1";

    public static final String PREFIX_PATH = "odktables";
    public static final String REF = "/ref";
    public static final String ROWS = "/rows";
    public static final String TABLES = "/tables";
    public static final String ATTACHMENTS = "/attachments/";
    public static final String MANIFEST = "/manifest";

    public static final String URL = AGGREGATE_URL + File.separator + PREFIX_PATH + File.separator + APP_ID;

    private WebAgent mWebAgent;

    public RESTClient() {
        mWebAgent = new WebAgent();
    }

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

    public String getRawJSONValue(String fullURL) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(fullURL)
                .header("User-Agent", "OkHttp Headers.java")
                .addHeader("Accept", "application/json;")
                .build();
        ResponseWrapper responseWrapper = mWebAgent.doGET(request);
        return responseWrapper.getResponse().body().string();
    }
}
