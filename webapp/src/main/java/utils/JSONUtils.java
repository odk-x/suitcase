package utils;

import com.google.gson.Gson;

import java.lang.reflect.Type;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/20/15
 * Time: 5:43 PM
 */
public final class JSONUtils {
    private JSONUtils() {
        // Prevent instantiation
    }

    public static <T> T getObj(String json, java.lang.Class<T> afterClass) {
        return new Gson().fromJson(json, afterClass);
    }

    public static <T> T getObj(String json, Type type) {
        return new Gson().fromJson(json, type);
    }
}
