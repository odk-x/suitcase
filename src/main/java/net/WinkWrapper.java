package net;

import model.AggregateInfo;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.aggregate.odktables.rest.entity.Row;
import org.opendatakit.aggregate.odktables.rest.entity.RowOutcomeList;
import org.opendatakit.wink.client.WinkClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.zip.DataFormatException;

import static org.opendatakit.wink.client.WinkClient.*;

public class WinkWrapper {
  private static final String FETCH_LIMIT = "1000";
  private static final int DELETE_TABLE_DEF_WAIT = 1000;
  private static final int PUSH_DONE_WAIT = 5000;

  private AggregateInfo aggInfo;
  private WinkClient wc;
  private boolean hasInit;

  private WinkWrapper() {
    this.hasInit = false;
  }

  private static class InstanceHolder {
    private static final WinkWrapper INSTANCE = new WinkWrapper();
  }

  public static WinkWrapper getInstance() {
    return InstanceHolder.INSTANCE;
  }

  public void init(AggregateInfo aggInfo) throws IOException, JSONException, Exception {
    if (!hasInit) {
      this.aggInfo = aggInfo;
      this.wc = new WinkClient();

      wc.init(this.aggInfo.getHostUrl(), this.aggInfo.getUserName(), this.aggInfo.getPassword());
      updateTableList();

      hasInit = true;
    }
  }

  public void reset() {
    this.hasInit = false;
  }

  public boolean isInitialized() {
    return hasInit;
  }

  public Set<String> updateTableList() throws IOException, JSONException, Exception {
    JSONArray tables =
        wc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId()).getJSONArray(jsonTables);

    for (int i = 0; i < tables.size(); i++) {
      String tableId = tables.getJSONObject(i).getString(jsonTableId);
      String eTag = tables.getJSONObject(i).getString(jsonSchemaETag);
      aggInfo.addTableId(tableId, eTag);
    }

    return aggInfo.getAllTableId();
  }

  public void pushAllData(String dataPath, String version)
      throws JSONException, IOException, DataFormatException, Exception {
//    System.out.println("pushAllData " + aggInfo.getServerUrl() + " " +aggInfo.getAppId() + " " + dataPath +" "+version);
    wc.pushAllDataToUri(aggInfo.getServerUrl(), aggInfo.getAppId(), dataPath, version);

    try {
      Thread.sleep(PUSH_DONE_WAIT);
    } catch (InterruptedException e) {
      //ignore
    }
  }

  public int deleteFile(String filename, String version) throws IOException, Exception {
    return wc.deleteFile(aggInfo.getServerUrl(), aggInfo.getAppId(), filename, version);
  }

  public JSONObject getManifestForAppLevelFiles(String version) throws IOException, JSONException, Exception {
    return wc.getManifestForAppLevelFiles(aggInfo.getServerUrl(), aggInfo.getAppId(), version);
  }

  public int deleteTableDefinition(String tableId) throws IOException, Exception {
    return deleteTableDefinition(tableId, null);
  }

  public int deleteTableDefinition(String tableId, String schemaETag) throws IOException, Exception {
    if (schemaETag == null && !aggInfo.tableIdExists(tableId)) {
      throw new IllegalArgumentException("tableId: " + tableId + " does not exist");
    }

    if (schemaETag == null) {
      schemaETag = aggInfo.getSchemaETag(tableId);
    }

    int result =
        wc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), tableId, schemaETag);

    try {
      Thread.sleep(DELETE_TABLE_DEF_WAIT);
    } catch (InterruptedException e) {
      //ignore
    }
    return result;
  }

  public JSONObject getManifestForRow(String tableId, String rowId)
      throws IOException, JSONException, Exception {
    if (!aggInfo.tableIdExists(tableId)) {
      throw new IllegalArgumentException("tableId: " + tableId + " does not exist");
    }

    return wc.getManifestForRow(
        aggInfo.getServerUrl(), aggInfo.getAppId(), tableId,
        aggInfo.getSchemaETag(tableId), rowId);
  }

  public JSONObject getRows(String tableId, String cursor) throws IOException, JSONException {
    if (!aggInfo.tableIdExists(tableId)) {
      throw new IllegalArgumentException("tableId: " + tableId + " does not exist");
    }

    return wc.getRows(
        aggInfo.getServerUrl(), aggInfo.getAppId(), tableId, aggInfo.getSchemaETag(tableId),
        cursor, FETCH_LIMIT
    );
  }

  public void getFileForRow(String tableId, String rowId, String savePath, String relPathOnServer)
      throws IOException, Exception {
    if (!aggInfo.tableIdExists(tableId)) {
      throw new IllegalArgumentException("tableId: " + tableId + " does not exist");
    }

    wc.getFileForRow(
        aggInfo.getServerUrl(), aggInfo.getAppId(), tableId, aggInfo.getSchemaETag(tableId),
        rowId, false, savePath, relPathOnServer);
  }

  public void batchGetFilesForRow(String tableId, String rowId, String savePath, JSONObject files)
      throws IOException, JSONException, Exception {
    if (!aggInfo.tableIdExists(tableId)) {
      throw new IllegalArgumentException("tableId: " + tableId + " does not exist");
    }

    wc.batchGetFilesForRow(
        aggInfo.getServerUrl(), aggInfo.getAppId(), tableId, aggInfo.getSchemaETag(tableId),
        rowId, savePath, files, -1);
  }
  
  // CAL: Should this be changed to pull all the tables once?
  // It may be possible that the schemaEtag changes as we may allow someone to 
  // delete a table and create a table
  public String getSchemaETagForTable(String tableId) {
    String schemaETag = null;
    try {
      // Not sure if this will work right?
      if (aggInfo.tableIdExists(tableId)) {
        schemaETag = aggInfo.getSchemaETag(tableId);
      } else {
        JSONObject obj = wc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());

        JSONArray tables = obj.getJSONArray(WinkClient.jsonTables);

        for (int i = 0; i < tables.size(); i++) {
          JSONObject table = tables.getJSONObject(i);
          if (tableId.equals(table.getString(WinkClient.jsonTableId))) {
            schemaETag =  table.getString(WinkClient.jsonSchemaETag);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return schemaETag;
  }
  
  public String getDataETag(String tableId, String tableSchemaETag) {
    String dataETag = null;
    
    try {
      JSONObject res = wc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), tableId, tableSchemaETag, null, null,
          null);
      
      if (res.containsKey(jsonDataETag) && !res.isNull(jsonDataETag)) {
        dataETag = res.getString(jsonDataETag);
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }

    return dataETag;
  }
  
  public RowOutcomeList alterRowsUsingSingleBatch(String tableId, String schemaETag, String dataETagVal, ArrayList<Row> rowArrayList)
    throws Exception {
    return wc.alterRowsUsingSingleBatch(aggInfo.getServerUrl(), aggInfo.getAppId(), tableId, schemaETag, dataETagVal, rowArrayList);
  }
  

  @Override
  protected Object clone() throws CloneNotSupportedException {
    super.clone();
    throw new CloneNotSupportedException();
  }
}
