package net;

import model.AggregateInfo;
import model.CsvConfig;
import model.ODKCsv;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.aggregate.odktables.rest.RFC4180CsvWriter;
import org.opendatakit.wink.client.WinkClient;
import utils.FileUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.SortedMap;
import java.util.zip.DataFormatException;

import static org.opendatakit.wink.client.WinkClient.*;

public class WinkSingleton {
  private static final String FETCH_LIMIT = "1000";
  private static final int DELETE_TABLE_DEF_WAIT = 1000;
  private static final int PUSH_DONE_WAIT = 5000;

  private AggregateInfo aggInfo;
  private WinkClient wc;
  private boolean hasInit;

  private WinkSingleton() {
    this.hasInit = false;
  }

  private static class InstanceHolder {
    private static final WinkSingleton INSTANCE = new WinkSingleton();
  }

  public static WinkSingleton getInstance() {
    return InstanceHolder.INSTANCE;
  }

  public void init(AggregateInfo aggInfo) throws IOException, JSONException {
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

  public Set<String> updateTableList() throws IOException, JSONException {
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
      throws JSONException, IOException, DataFormatException {
    wc.pushAllDataToUri(aggInfo.getServerUrl(), aggInfo.getAppId(), dataPath, version);

    try {
      Thread.sleep(PUSH_DONE_WAIT);
    } catch (InterruptedException e) {
      //ignore
    }
  }

  public int deleteFile(String filename, String version) throws IOException {
    return wc.deleteFile(aggInfo.getServerUrl(), aggInfo.getAppId(), filename, version);
  }

  public JSONObject getManifestForAppLevelFiles(String version) throws IOException, JSONException {
    return wc.getManifestForAppLevelFiles(aggInfo.getServerUrl(), aggInfo.getAppId(), version);
  }

  public int deleteTableDefinition(String tableId) throws IOException {
    return deleteTableDefinition(tableId, null);
  }

  public int deleteTableDefinition(String tableId, String schemaETag) throws IOException {
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
      throws IOException, JSONException {
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
      throws IOException {
    if (!aggInfo.tableIdExists(tableId)) {
      throw new IllegalArgumentException("tableId: " + tableId + " does not exist");
    }

    wc.getFileForRow(
        aggInfo.getServerUrl(), aggInfo.getAppId(), tableId, aggInfo.getSchemaETag(tableId),
        rowId, false, savePath, relPathOnServer);
  }

  public void batchGetFilesForRow(String tableId, String rowId, String savePath, JSONObject files)
      throws IOException, JSONException {
    if (!aggInfo.tableIdExists(tableId)) {
      throw new IllegalArgumentException("tableId: " + tableId + " does not exist");
    }

    wc.batchGetFilesForRow(
        aggInfo.getServerUrl(), aggInfo.getAppId(), tableId, aggInfo.getSchemaETag(tableId),
        rowId, savePath, files, -1);
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    super.clone();
    throw new CloneNotSupportedException();
  }
}
