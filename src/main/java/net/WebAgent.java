package net;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import model.ResponseWrapper;

import java.io.IOException;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/19/15
 * Time: 9:29 AM
 */
public class WebAgent {

    private final OkHttpClient client = new OkHttpClient();

    public ResponseWrapper doGET(Request request) {
        ResponseWrapper response = new ResponseWrapper();
        try {
            response.setResponse(client.newCall(request).execute());
        } catch (IOException e) {
            response.setError(e.getMessage());
        }
        return response;
    }
}
