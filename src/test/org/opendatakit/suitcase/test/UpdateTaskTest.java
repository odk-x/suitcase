package org.opendatakit.suitcase.test;


import java.net.URL;
import java.util.concurrent.CountDownLatch;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.wink.client.WinkClient;
import org.opendatakit.suitcase.net.LoginTask;
import org.opendatakit.suitcase.net.UpdateTask;
import org.opendatakit.suitcase.model.AggregateInfo;

import junit.framework.TestCase;

public class UpdateTaskTest extends TestCase{
  
  String serverUrl;
  String appId;
  String absolutePathOfTestFiles;
  String host;
  String username;
  String password;
  int batchSize;
  String version;
  
  /*
   * Perform setup for test if necessary
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    //agg_url = System.getProperty("test.aggUrl");
    //appId = System.getProperty("test.appId");
    //absolutePathOfTestFiles = System.getProperty("test.absolutePathOfTestFiles");
    //batchSize = Integer.valueOf(System.getProperty("test.batchSize"));
    
    serverUrl = "https://test.appspot.com";
    appId = "default";
    absolutePathOfTestFiles = "testfiles/";
    batchSize = 1000;
    username = "";
    password = "";
    URL url = new URL(serverUrl);
    host = url.getHost();
    version = "2";
  }
  
  public void testUpdateTaskAdd_ExpectPass() {
    AggregateInfo aggInfo = null;
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String dataPath = absolutePathOfTestFiles + "plot/plot-add.csv";
    String testTableId = "test1";
    String tableSchemaETag = null;
    WinkClient wc = null;

    try {
      aggInfo = new AggregateInfo(serverUrl, appId, username, password);
      wc = new WinkClient();
      wc.init(aggInfo.getHostUrl(), aggInfo.getUserName(), aggInfo.getPassword());

      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();

      JSONObject result = wc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskAdd: result is " + result);

      if (result.containsKey(WinkClient.jsonTableId)) {
        String tableId = result.getString(WinkClient.jsonTableId);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(WinkClient.jsonSchemaETag);
      }

      // Get the table definition
      JSONObject tableDef = wc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      UpdateTask updateTask = new UpdateTask(aggInfo, dataPath, version, testTableId, false);
      updateTask.blockingExecute();

      JSONObject res = wc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag, null, null, null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Now check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      // Now delete the table
      wc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = wc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

      wc.close();

    } catch (Exception e1) {
      e1.printStackTrace();
      System.out.println("testUpdateTaskAdd failed with exception: " + e1);
      fail();
    }
  }
  
  public void testUpdateTaskDelete() {
    AggregateInfo aggInfo = null;
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String dataPathAdd = absolutePathOfTestFiles + "plot/plot-add.csv";
    String dataPathDelete = absolutePathOfTestFiles + "plot/plot-delete.csv";
    String testTableId = "test2";
    String tableSchemaETag = null;
    WinkClient wc = null;

    try {
      aggInfo = new AggregateInfo(serverUrl, appId, username, password);
      wc = new WinkClient();
      wc.init(aggInfo.getHostUrl(), aggInfo.getUserName(), aggInfo.getPassword());

      JSONObject result = wc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskDelete: result is " + result);

      if (result.containsKey(WinkClient.jsonTableId)) {
        String tableId = result.getString(WinkClient.jsonTableId);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(WinkClient.jsonSchemaETag);
      }

      // Get the table definition
      JSONObject tableDef = wc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();

      UpdateTask updateTask = new UpdateTask(aggInfo, dataPathAdd, version, testTableId, false);
      updateTask.blockingExecute();

      JSONObject res = wc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Now check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      UpdateTask taskDelete = new UpdateTask(aggInfo, dataPathDelete, version, testTableId, false);
      taskDelete.blockingExecute();

      res = wc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 0);

      // Now delete the table
      wc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, tableSchemaETag);
      
      // Check that table no longer exists
      JSONObject obj = wc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));
      
      wc.close();
      
    } catch (Exception e1) {
      e1.printStackTrace();
      System.out.println("testUpdateTaskDelete failed with exception: " + e1);
      fail();
    }
  }
  
  public void testUpdateTaskUpdate() {
    AggregateInfo aggInfo = null;
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String dataPathAdd = absolutePathOfTestFiles + "plot/plot-add.csv";
    String dataPathUpdate = absolutePathOfTestFiles + "plot/plot-update.csv";
    String testTableId = "test3";
    String tableSchemaETag = null;
    WinkClient wc = null;

    try {
      aggInfo = new AggregateInfo(serverUrl, appId, username, password);
      wc = new WinkClient();
      wc.init(aggInfo.getHostUrl(), aggInfo.getUserName(), aggInfo.getPassword());

      JSONObject result = wc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskUpdate: result is " + result);

      if (result.containsKey(WinkClient.jsonTableId)) {
        String tableId = result.getString(WinkClient.jsonTableId);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(WinkClient.jsonSchemaETag);
      }

      // Get the table definition
      JSONObject tableDef = wc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();

      UpdateTask updateTask = new UpdateTask(aggInfo, dataPathAdd, version, testTableId, false);
      updateTask.blockingExecute();

      JSONObject res = wc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag, null, null, null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      UpdateTask taskUpdate = new UpdateTask(aggInfo, dataPathUpdate, version, testTableId, false);
      taskUpdate.blockingExecute();

      res = wc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag, null, null, null);

      rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Clarice", jsonRow));

      // Now delete the table
      wc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag);
      
      // Check that table no longer exists
      JSONObject obj = wc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

      wc.close();

    } catch (Exception e1) {
      e1.printStackTrace();
      System.out.println("testUpdateTaskUpdate failed with exception: " + e1);
      fail();
    }
  }
  
  public void testUpdateTaskForceUpdate() {
    AggregateInfo aggInfo = null;
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String dataPathAdd = absolutePathOfTestFiles + "plot/plot-add.csv";
    String dataPathUpdate = absolutePathOfTestFiles + "plot/plot-forceUpdate.csv";
    String testTableId = "test4";
    String tableSchemaETag = null;
    WinkClient wc = null;

    try {
      aggInfo = new AggregateInfo(serverUrl, appId, username, password);
      wc = new WinkClient();
      wc.init(aggInfo.getHostUrl(), aggInfo.getUserName(), aggInfo.getPassword());

      JSONObject result = wc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskForceUpdate: result is " + result);

      if (result.containsKey(WinkClient.jsonTableId)) {
        String tableId = result.getString(WinkClient.jsonTableId);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(WinkClient.jsonSchemaETag);
      }

      // Get the table definition
      JSONObject tableDef = wc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();

      UpdateTask task = new UpdateTask(aggInfo, dataPathAdd, version, testTableId, false);
      task.blockingExecute();

      JSONObject res = wc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      UpdateTask taskUpdate = new UpdateTask(aggInfo, dataPathUpdate, version, testTableId, false);
      taskUpdate.blockingExecute();

      res = wc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, tableSchemaETag, null, null, null);

      rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Clarice", jsonRow));

      // Now delete the table
      wc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = wc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

    } catch (Exception e1) {
      e1.printStackTrace();
      System.out.println("testUpdateTaskForceUpdate failed with exception: " + e1);
      fail();
    }
  }
  
  // TODO:
  public void testUpdateTaskWithOver2000Rows_ExpectPass() {
    
  }
  
  // TODO:
  public void testUpdateTaskAllOperationsInFile_ExpectPass() {
  }
  

}
