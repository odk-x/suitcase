package org.opendatakit.suitcase.model;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles Scan's raw JSON
 *
 * !!!ATTENTION!!! One per row
 */
public class ScanJson {
  private Map<String, String> labelValuePair;

  public ScanJson(InputStream jsonStream) throws JSONException {
    this.labelValuePair = null;

    if (jsonStream != null) {
      this.labelValuePair = buildLabelValuePair(jsonStream);
    }
  }

  private Map<String, String> buildLabelValuePair(InputStream jsonStream) throws JSONException {
    Map<String, String> map = new HashMap<>();

    JSONArray fields = new JSONObject(jsonStream).getJSONArray("fields");

    for (int i = 0; i < fields.size(); i++) {
      JSONObject field = fields.getJSONObject(i);
      map.put(field.optString("name"), field.has("value") ? field.optString("value") : "null");
    }

    return map;
  }

  public String getValue(String label) {
    if (this.labelValuePair == null) {
      return "null";
    }

    return this.labelValuePair.get(label);
  }
}
