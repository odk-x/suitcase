package utils;

import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/20/15
 * Time: 5:43 PM
 */
public final class JSONUtils {
    private static final String IMAGE = "_image0";
    private static final String CONTENT_TYPE = "_contentType";
    private static final String URI_FRAGMENT = "_uriFragment";
    private static final String IMG_URI_FRAGMENT = IMAGE + "_uriFragment";
    private static final String RAW_URI_FRAGMENT = "raw_uriFragment";


    private JSONUtils() {
        // Prevent instantiation
    }

    public static <T> T getObj(String json, java.lang.Class<T> afterClass) {
        return new GsonBuilder().serializeNulls().create().fromJson(json, afterClass);
    }

    public static <T> T getObj(String json, Type type) {
        return new GsonBuilder().serializeNulls().create().fromJson(json, type);
    }

    public static boolean isValueColumn(String s) {
        boolean result = false;
        if (!s.endsWith(CONTENT_TYPE) && !s.endsWith(URI_FRAGMENT)) {
            result = true;
        }
        return result;
    }

    public static boolean isImgURIColumn(String s) {
        boolean result = false;
        if (s.endsWith(IMG_URI_FRAGMENT)) {
            result = true;
        }
        return result;
    }

    public static boolean isRawJSONColumn(String s) {
        boolean result = false;
        if (s.equals(RAW_URI_FRAGMENT)) {
            result = true;
        }
        return result;
    }

    public static boolean isValidColumn(String s) {
        boolean result = true;
        if (s.endsWith("_contentType") || s.equals("scan_output_directory")) {
            result = false;
        }
        return result;
    }
}
