package org.opendatakit.suitcase.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.aggregate.odktables.rest.RFC4180CsvReader;
import org.opendatakit.wink.client.WinkClient;

public final class TestUtilities {
  
  public static boolean checkThatTableDefAndCSVDefAreEqual(String csvFile, JSONObject tableDef) {
    boolean same = false;
    InputStream in;

    try {
      in = new FileInputStream(new File(csvFile));

      InputStreamReader inputStream = new InputStreamReader(in);
      RFC4180CsvReader reader = new RFC4180CsvReader(inputStream);

      // Skip the first line - it's just headers
      reader.readNext();

      if (tableDef.containsKey(WinkClient.ORDERED_COLUMNS_DEF)) {
        JSONArray cols = tableDef.getJSONArray(WinkClient.ORDERED_COLUMNS_DEF);
        String[] csvDef;
        while ((csvDef = reader.readNext()) != null) {
          same = false;
          for (int i = 0; i < cols.size(); i++) {
            JSONObject col = cols.getJSONObject(i);
            String testElemKey = col.getString(WinkClient.ELEM_KEY_JSON);
            if (csvDef[0].equals(testElemKey)) {
              same = true;
              // Remove the index so we don't keep
              // comparing old defs
              cols.remove(i);
              break;
            }
          }

          if (!same) {
            return same;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return same;
  }
  
  public static boolean checkThatRowHasId(String RowId, JSONObject rowRes) {
    boolean same = false;

    try {
      if (RowId.equals(rowRes.getString(WinkClient.ID_JSON))) {
        same = true;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return same;
  }
  
  public static boolean checkThatRowHasColumnValue(String column, String columnValue, JSONObject rowRes) {
    String COLUMN_STRING = "column";
    String VALUE_STRING = "value";
    
    if (column == null || columnValue == null) {
      return false;
    }

    try {
      if (rowRes.has(WinkClient.ORDERED_COLUMNS_DEF)) {
        JSONArray ordCols = rowRes.getJSONArray(WinkClient.ORDERED_COLUMNS_DEF);
        for (int i = 0; i < ordCols.length(); i++) {
          JSONObject col = ordCols.getJSONObject(i);
          String colStr = col.has(COLUMN_STRING) && !col.isNull(COLUMN_STRING) ? col.getString(COLUMN_STRING) : null;
          if (colStr == null) {
            return false;
          }
          if (colStr.equals(column)) {
            String recVal = col.has(VALUE_STRING) && !col.isNull(VALUE_STRING) ? col.getString(VALUE_STRING) : null;
            if (recVal == null) {
              return false;
            }
            if (recVal.equals(columnValue)) {
              return true;
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
  
  public static boolean checkTableExistOnServer(JSONObject tablesObj, String tableId, String schemaETag) {
    boolean exists = false;

    try {
      JSONArray tables = tablesObj.getJSONArray("tables");

      for (int i = 0; i < tables.size(); i++) {
        JSONObject table = tables.getJSONObject(i);
        if (tableId.equals(table.getString("tableId"))) {
          if (schemaETag.equals(table.getString("schemaETag"))) {
            exists = true;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return exists;
  }
}
