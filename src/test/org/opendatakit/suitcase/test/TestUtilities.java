package org.opendatakit.suitcase.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.aggregate.odktables.rest.RFC4180CsvReader;
import org.opendatakit.sync.client.SyncClient;

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

      if (tableDef.containsKey(SyncClient.ORDERED_COLUMNS_DEF)) {
        JSONArray cols = tableDef.getJSONArray(SyncClient.ORDERED_COLUMNS_DEF);
        String[] csvDef;
        while ((csvDef = reader.readNext()) != null) {
          same = false;
          for (int i = 0; i < cols.size(); i++) {
            JSONObject col = cols.getJSONObject(i);
            String testElemKey = col.getString(SyncClient.ELEM_KEY_JSON);
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
      if (RowId.equals(rowRes.getString(SyncClient.ID_JSON))) {
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
      if (rowRes.has(SyncClient.ORDERED_COLUMNS_DEF)) {
        JSONArray ordCols = rowRes.getJSONArray(SyncClient.ORDERED_COLUMNS_DEF);
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
  
  public static int getCSVIndexForCloudEndpointMetadata(String[] headers, String cloudEndpointId) {
    int idx = -1;
    
    if (cloudEndpointId == null || cloudEndpointId.length() == 0) {
      return idx;
    }
    
    String csvString = null;
    switch (cloudEndpointId) {
      case SyncClient.ID_JSON:
        csvString = SyncClient.ID_ROW_DEF;
        break;
      
      case SyncClient.FORM_ID_JSON:
        csvString = SyncClient.FORM_ID_ROW_DEF;
        break;
        
      case SyncClient.LOCALE_JSON:
        csvString = SyncClient.LOCALE_ROW_DEF;
        break;
        
      case SyncClient.SAVEPOINT_TYPE_JSON:
        csvString = SyncClient.SAVEPOINT_TYPE_ROW_DEF;
        break;
      
      case SyncClient.SAVEPOINT_TIMESTAMP_JSON:
        csvString = SyncClient.SAVEPOINT_TIMESTAMP_ROW_DEF;
        break;
      
      case SyncClient.SAVEPOINT_CREATOR_JSON:
        csvString = SyncClient.SAVEPOINT_CREATOR_ROW_DEF;
        break;
        
      case SyncClient.ROW_ETAG_JSON:
        csvString = SyncClient.ROW_ETAG_ROW_DEF;
        break;
      
      case SyncClient.DEFAULT_ACCESS_JSON:
        csvString = SyncClient.DEFAULT_ACCESS_ROW_DEF;
        break;
        
      case SyncClient.ROW_OWNER_JSON:
        csvString = SyncClient.ROW_OWNER_ROW_DEF;
        break;
        
      case SyncClient.GROUP_MODIFY_JSON:
        csvString = SyncClient.GROUP_MODIFY_ROW_DEF;
        break;
      
      case SyncClient.GROUP_PRIVILEGED_JSON:
        csvString = SyncClient.GROUP_PRIVILEGED_ROW_DEF;
        break;
        
      case SyncClient.GROUP_READ_ONLY_JSON:
        csvString = SyncClient.GROUP_READ_ONLY_ROW_DEF;
        break;
    
      default:
        break;
    }
    
    if (csvString != null) {
      idx = findIdxFromHeader(headers, csvString);
    }
    
    return idx;
  }
  
  public static String convertCSVMetadataToJsonMetadata(String csvMetadata) {
    if (csvMetadata == null || csvMetadata.length() == 0) {
      return null;
    }
    
    if (csvMetadata.charAt(0) != '_') {
      return null;
    }
    
    StringBuilder bld = new StringBuilder();
    
    int charToCap = -1;
    for (int i = 1; i < csvMetadata.length(); i++) {
      if (csvMetadata.charAt(i) == '_') {
        charToCap = i+1;
      } else {
        if (charToCap == i) {
          bld.append(Character.toUpperCase(csvMetadata.charAt(i)));
        } else {
          bld.append(csvMetadata.charAt(i));
        }
      }
    }
    return bld.toString();
  }
  
  public static int findIdxFromHeader(String[] headers, String csvCol) {
    int idx = -1;
    
    for (int i = 0; i < headers.length; i++) {
      if (headers[i].equals(csvCol)) {
        return i;
      }
    }
    
    return idx;
  }
  
  public static boolean isMetadataCSVField(String fieldName) {
    ArrayList<String> metadataFields = new ArrayList<String>();
    metadataFields.add(SyncClient.ID_ROW_DEF);
    metadataFields.add(SyncClient.FORM_ID_ROW_DEF);
    metadataFields.add(SyncClient.LOCALE_ROW_DEF);
    metadataFields.add(SyncClient.SAVEPOINT_TYPE_ROW_DEF);
    metadataFields.add(SyncClient.SAVEPOINT_TIMESTAMP_ROW_DEF);
    metadataFields.add(SyncClient.SAVEPOINT_CREATOR_ROW_DEF);
    metadataFields.add(SyncClient.ROW_ETAG_ROW_DEF);
    
    if (fieldName == null || fieldName.length() == 0) {
      return false;
    }
    
    if (metadataFields.contains(fieldName)) {
      return true;
    }
    
    return false;
  }
  
  public static boolean isPermissionMetadataCSVField(String fieldName) {
    ArrayList<String> metadataFields = new ArrayList<String>();
    metadataFields.add(SyncClient.DEFAULT_ACCESS_ROW_DEF);
    metadataFields.add(SyncClient.ROW_OWNER_ROW_DEF);
    metadataFields.add(SyncClient.GROUP_MODIFY_ROW_DEF);
    metadataFields.add(SyncClient.GROUP_PRIVILEGED_ROW_DEF);
    metadataFields.add(SyncClient.GROUP_READ_ONLY_ROW_DEF);
    
    if (fieldName == null || fieldName.length() == 0) {
      return false;
    }
    
    if (metadataFields.contains(fieldName)) {
      return true;
    }
    
    return false;
  }
  
  public static boolean verifyServerRowsMatchCSV(JSONArray rows, String csvFilePath) {
    boolean same = false;
    InputStream in;
    
    try {
      in = new FileInputStream(new File(csvFilePath));

      InputStreamReader inputStream = new InputStreamReader(in);
      RFC4180CsvReader reader = new RFC4180CsvReader(inputStream);

      // Skip the first line - it's just headers
      String[] headers = reader.readNext();
      String[] csvData;
      
      while ((csvData = reader.readNext()) != null) {
        int idx = findIdxFromHeader(headers, SyncClient.ID_ROW_DEF);
        
        if (idx == -1) {
          return false;
        }
        
        for (int i = 0; i < rows.length(); i++) {
          JSONObject row = rows.getJSONObject(i);
          String serverRowId = row.containsKey(SyncClient.ID_JSON) ? row.getString(SyncClient.ID_JSON) : null;
          if (serverRowId != null && serverRowId.equals(csvData[idx])) {
            same =  verifyServerRowAndCsvRowMatch(headers, csvData, row);
            if (same == false) {
              return false;
            }
            break;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return same;
  }
  
  public static boolean verifyServerRowAndCsvRowMatch(String[] headers, String[] csvRow, JSONObject serverRow) {
    try {  
      int start = 0;
      
      if (headers[0].equals("operation")) {
        start = 1;
      }
      
      // Get this value here so we can remove values as we find them
      // to speed up search
      JSONArray orderedColumns = serverRow.getJSONArray(SyncClient.ORDERED_COLUMNS_DEF);
      
      for (int i = start; i < csvRow.length; i++) {
        if (isMetadataCSVField(headers[i])) {
          // We don't care if the rowETag is the same
          if (!headers[i].equals(SyncClient.ROW_ETAG_ROW_DEF)) {
            String jsonKey = convertCSVMetadataToJsonMetadata(headers[i]);
            if (!checkThatJsonValEqualsCsvVal(serverRow, jsonKey, csvRow[i])) {
              System.out.println("csvRow[" + i + "] and serverRow don't match for key: " + jsonKey);
              return false;
            }
          }
        } else if (isPermissionMetadataCSVField(headers[i])) {
          // Have to get filterScope
          JSONObject filterScope = serverRow.getJSONObject(SyncClient.FILTER_SCOPE_JSON);
          String jsonKey = convertCSVMetadataToJsonMetadata(headers[i]);
          
          if (!checkThatJsonValEqualsCsvVal(filterScope, jsonKey, csvRow[i])) {
            System.out.println("csvRow[" + i + "] and serverRow don't match for key: " + jsonKey);
            return false;
          }
              
        } else {
          // Have to check every orderedColumn
          for (int j = 0; j < orderedColumns.length(); j++) {
            JSONObject ordCol = orderedColumns.getJSONObject(j);
            String colName = ordCol.getString("column");
            if (colName.equals(headers[i])) {
              // Don't bother checking geopoint as the precision will vary
              if (colName.endsWith("_accuracy") || colName.endsWith("_altitude") || 
                  colName.endsWith("_longitude") || colName.endsWith("_latitude")) {
                orderedColumns.remove(j);
                break;
              }
              if (checkThatJsonValEqualsCsvVal(ordCol, "value", csvRow[i])) {
                // Remove this orderedCol to speed up search
                orderedColumns.remove(j);
                break;
              } else {
                System.out.println("csvRow[" + i + "] and serverRow don't match for colName: " + colName);
                return false;
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return true;
  }
  
  private static boolean checkThatJsonValEqualsCsvVal(JSONObject jObj, String jsonKey, String csvValue) 
      throws JSONException {
    String jValue = jObj.isNull(jsonKey) ? null : jObj.getString(jsonKey);    
    return jValue == null ? csvValue == null : jValue.equals(csvValue);
  }
  
}
