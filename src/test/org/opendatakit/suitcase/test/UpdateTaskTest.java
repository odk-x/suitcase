package org.opendatakit.suitcase.test;

import java.net.URL;

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
  AggregateInfo aggInfo = null;
  
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
    
    serverUrl = "https://clarlars.appspot.com";
    appId = "default";
    absolutePathOfTestFiles = "testfiles/";
    batchSize = 1000;
    username = "";
    password = "";
    URL url = new URL(serverUrl);
    host = url.getHost();
    version = "2";
    aggInfo = new AggregateInfo(serverUrl, appId, username, password); 
  }
  
  public void testUpdateTaskAdd_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String dataPath = absolutePathOfTestFiles + "plot/plot-add.csv";
    String testTableId = "test1";
    String tableSchemaETag = null;
    WinkClient wc = null;

    try {
      wc = new WinkClient();
      wc.init(aggInfo.getHostUrl(), aggInfo.getUserName(), aggInfo.getPassword());

      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();

      JSONObject result = wc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskAdd: result is " + result);

      if (result.containsKey(WinkClient.TABLE_ID_JSON)) {
        String tableId = result.getString(WinkClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(WinkClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = wc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      UpdateTask updateTask = new UpdateTask(aggInfo, dataPath, version, testTableId, null, false);
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

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskAdd_ExpectPass");
      e.printStackTrace();
      fail();
    }
  }
  
  public void testUpdateTaskDelete_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String dataPathAdd = absolutePathOfTestFiles + "plot/plot-add.csv";
    String dataPathDelete = absolutePathOfTestFiles + "plot/plot-delete.csv";
    String testTableId = "test2";
    String tableSchemaETag = null;
    WinkClient wc = null;

    try {
      wc = new WinkClient();
      wc.init(aggInfo.getHostUrl(), aggInfo.getUserName(), aggInfo.getPassword());

      JSONObject result = wc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskDelete: result is " + result);

      if (result.containsKey(WinkClient.TABLE_ID_JSON)) {
        String tableId = result.getString(WinkClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(WinkClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = wc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();

      UpdateTask updateTask = new UpdateTask(aggInfo, dataPathAdd, version, testTableId, null, false);
      updateTask.blockingExecute();

      JSONObject res = wc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Now check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      UpdateTask taskDelete = new UpdateTask(aggInfo, dataPathDelete, version, testTableId, null, false);
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
      
    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskDelete_ExpectPass");
      e.printStackTrace();
      fail();
    }
  }
  
  public void testUpdateTaskUpdate_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String dataPathAdd = absolutePathOfTestFiles + "plot/plot-add.csv";
    String dataPathUpdate = absolutePathOfTestFiles + "plot/plot-update.csv";
    String testTableId = "test3";
    String tableSchemaETag = null;
    WinkClient wc = null;

    try {
      wc = new WinkClient();
      wc.init(aggInfo.getHostUrl(), aggInfo.getUserName(), aggInfo.getPassword());

      JSONObject result = wc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskUpdate: result is " + result);

      if (result.containsKey(WinkClient.TABLE_ID_JSON)) {
        String tableId = result.getString(WinkClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(WinkClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = wc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();

      UpdateTask updateTask = new UpdateTask(aggInfo, dataPathAdd, version, testTableId, null, false);
      updateTask.blockingExecute();

      JSONObject res = wc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag, null, null, null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      UpdateTask taskUpdate = new UpdateTask(aggInfo, dataPathUpdate, version, testTableId, null, false);
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

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskUpdate_ExpectPass");
      e.printStackTrace();
      fail();
    }
  }
  
  public void testUpdateTaskForceUpdate_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String dataPathAdd = absolutePathOfTestFiles + "plot/plot-add.csv";
    String dataPathUpdate = absolutePathOfTestFiles + "plot/plot-forceUpdate.csv";
    String testTableId = "test4";
    String tableSchemaETag = null;
    WinkClient wc = null;

    try {
      wc = new WinkClient();
      wc.init(aggInfo.getHostUrl(), aggInfo.getUserName(), aggInfo.getPassword());

      JSONObject result = wc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskForceUpdate: result is " + result);

      if (result.containsKey(WinkClient.TABLE_ID_JSON)) {
        String tableId = result.getString(WinkClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(WinkClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = wc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();

      UpdateTask task = new UpdateTask(aggInfo, dataPathAdd, version, testTableId, null, false);
      task.blockingExecute();

      JSONObject res = wc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      UpdateTask taskUpdate = new UpdateTask(aggInfo, dataPathUpdate, version, testTableId, null, false);
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

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskForceUpdate_ExpectPass");
      e.printStackTrace();
      fail();
    }
  }
  
  public void testUpdateTaskWith1000Rows_ExpectPass() {
    String testTableId = "test5";
    String defPath = "testfiles/cookstoves/data_definition.csv";
    String dataPath = "testfiles/cookstoves/data_small.csv";
    
    try {
      WinkClient wc = new WinkClient();
      wc.init(aggInfo.getHostUrl(), aggInfo.getUserName(), aggInfo.getPassword());
      
      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();
      
      //AggregateInfo aggInfo, String tableId, String dataPath, String operation
      wc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, null, defPath);
      
      String schemaETag = wc.getSchemaETagForTable(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId);
      JSONObject tableDefObj = wc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag);
    
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(defPath, tableDefObj));
      
      // Need to add rows
      UpdateTask updateTask = new UpdateTask(aggInfo, dataPath, version, testTableId, null, false);
      updateTask.blockingExecute();
      
      JSONObject rowsObj = wc.getRows(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag, null, null);
      JSONArray rows = rowsObj.getJSONArray(WinkClient.ROWS_STR_JSON);
      
      assertEquals(rows.size(), 1000);
      
      // Then delete table definition
      wc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag);
      
      JSONObject obj = wc.getTable(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId);
      assertNull(obj);
      
      wc.close();
      
    } catch (Exception e) {
      System.out.println("TableTaskTest: Exception thrown in testUpdateTaskWith1000Rows_ExpectPass");
      e.printStackTrace();
      fail();
    } 
  }
  
  public void testUpdateTaskAllOperationsInFile_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String dataPathAdd = absolutePathOfTestFiles + "plot/plot-add5.csv";
    String dataPathUpdate = absolutePathOfTestFiles + "plot/plot-allops.csv";
    String testTableId = "test6";
    String tableSchemaETag = null;
    WinkClient wc = null;

    try {
      wc = new WinkClient();
      wc.init(aggInfo.getHostUrl(), aggInfo.getUserName(), aggInfo.getPassword());

      JSONObject result = wc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, null, csvFile);

      if (result.containsKey(WinkClient.TABLE_ID_JSON)) {
        String tableId = result.getString(WinkClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(WinkClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = wc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();

      UpdateTask task = new UpdateTask(aggInfo, dataPathAdd, version, testTableId, null, false);
      task.blockingExecute();

      JSONObject res = wc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 5);

      UpdateTask taskUpdate = new UpdateTask(aggInfo, dataPathUpdate, version, testTableId, null, false);
      taskUpdate.blockingExecute();

      res = wc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, tableSchemaETag, null, null, null);

      rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 4);

      JSONObject jsonRow = null;
      for (int i = 0; i < rows.size(); i++) {
        jsonRow = rows.getJSONObject(i);
        String rowId = jsonRow.getString(WinkClient.ID_JSON);
        if (!rowId.equals("2") &&
            !rowId.equals("3") &&
            !rowId.equals("5") &&
            !rowId.equals("10")) {
          System.out
          .println("UpdateTaskTest: unexpected row id " + rowId + " in testUpdateTaskAllOperationsInFile_ExpectPass");
          fail();
        } else if (rowId.equals("2")) {
          assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Clarice", jsonRow));
        } else if (rowId.equals("3")) {
          assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Clarlars", jsonRow));
        } else if (rowId.equals("5")) {
          assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Ungoni", jsonRow));
        } else if (rowId.equals("10")) {
          assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Lars", jsonRow));
        }
      }

      // Now delete the table
      wc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = wc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskAllOperationsInFile_ExpectPass");
      e.printStackTrace();
      fail();
    }
  }
}
