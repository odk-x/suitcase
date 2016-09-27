package org.opendatakit.suitcase.test;

import java.net.URL;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.sync.client.SyncClient;
import org.opendatakit.suitcase.net.LoginTask;
import org.opendatakit.suitcase.net.SuitcaseSwingWorker;
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
    
    serverUrl = "";
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
    SyncClient sc = null;
    int retCode;

    try {
      sc = new SyncClient();
      
      String agg_url = aggInfo.getHostUrl();
      agg_url = agg_url.substring(0, agg_url.length()-1);
      
      URL url = new URL(agg_url);
      String host = url.getHost();
      
      sc.init(host, aggInfo.getUserName(), aggInfo.getPassword());

      LoginTask lTask = new LoginTask(aggInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject result = sc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskAdd: result is " + result);

      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = sc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      UpdateTask updateTask = new UpdateTask(aggInfo, dataPath, version, testTableId, null, false);
      retCode = updateTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject res = sc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag, null, null, null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Now check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      // Now delete the table
      sc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = sc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

      sc.close();

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
    SyncClient sc = null;
    int retCode;

    try {
      sc = new SyncClient();
      String agg_url = aggInfo.getHostUrl();
      agg_url = agg_url.substring(0, agg_url.length()-1);
      
      URL url = new URL(agg_url);
      String host = url.getHost();
      
      sc.init(host, aggInfo.getUserName(), aggInfo.getPassword());

      JSONObject result = sc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskDelete: result is " + result);

      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = sc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(aggInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      UpdateTask updateTask = new UpdateTask(aggInfo, dataPathAdd, version, testTableId, null, false);
      retCode = updateTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject res = sc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Now check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      UpdateTask taskDelete = new UpdateTask(aggInfo, dataPathDelete, version, testTableId, null, false);
      retCode = taskDelete.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      res = sc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 0);

      // Now delete the table
      sc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, tableSchemaETag);
      
      // Check that table no longer exists
      JSONObject obj = sc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));
      
      sc.close();
      
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
    SyncClient sc = null;
    int retCode;

    try {
      sc = new SyncClient();
      String agg_url = aggInfo.getHostUrl();
      agg_url = agg_url.substring(0, agg_url.length()-1);
      
      URL url = new URL(agg_url);
      String host = url.getHost();
      
      sc.init(host, aggInfo.getUserName(), aggInfo.getPassword());

      JSONObject result = sc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskUpdate: result is " + result);

      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = sc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(aggInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      UpdateTask updateTask = new UpdateTask(aggInfo, dataPathAdd, version, testTableId, null, false);
      retCode = updateTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject res = sc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag, null, null, null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      UpdateTask taskUpdate = new UpdateTask(aggInfo, dataPathUpdate, version, testTableId, null, false);
      retCode = taskUpdate.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      res = sc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag, null, null, null);

      rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Clarice", jsonRow));

      // Now delete the table
      sc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag);
      
      // Check that table no longer exists
      JSONObject obj = sc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

      sc.close();

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
    SyncClient sc = null;
    int retCode;

    try {
      sc = new SyncClient();
      String agg_url = aggInfo.getHostUrl();
      agg_url = agg_url.substring(0, agg_url.length()-1);
      
      URL url = new URL(agg_url);
      String host = url.getHost();
      
      sc.init(host, aggInfo.getUserName(), aggInfo.getPassword());

      JSONObject result = sc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskForceUpdate: result is " + result);

      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = sc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(aggInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      UpdateTask task = new UpdateTask(aggInfo, dataPathAdd, version, testTableId, null, false);
      retCode = task.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject res = sc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      UpdateTask taskUpdate = new UpdateTask(aggInfo, dataPathUpdate, version, testTableId, null, false);
      retCode = taskUpdate.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      res = sc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, tableSchemaETag, null, null, null);

      rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Clarice", jsonRow));

      // Now delete the table
      sc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = sc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
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
    int retCode;
    
    try {
      SyncClient sc = new SyncClient();
      String agg_url = aggInfo.getHostUrl();
      agg_url = agg_url.substring(0, agg_url.length()-1);
      
      URL url = new URL(agg_url);
      String host = url.getHost();
      
      sc.init(host, aggInfo.getUserName(), aggInfo.getPassword());
      
      LoginTask lTask = new LoginTask(aggInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);
      
      //AggregateInfo aggInfo, String tableId, String dataPath, String operation
      sc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, null, defPath);
      
      String schemaETag = sc.getSchemaETagForTable(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId);
      JSONObject tableDefObj = sc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag);
    
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(defPath, tableDefObj));
      
      // Need to add rows
      UpdateTask updateTask = new UpdateTask(aggInfo, dataPath, version, testTableId, null, false);
      retCode = updateTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);
      
      JSONObject rowsObj = sc.getRows(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag, null, null);
      JSONArray rows = rowsObj.getJSONArray(SyncClient.ROWS_STR_JSON);
      
      assertEquals(rows.size(), 1000);
      
      // Then delete table definition
      sc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag);
      
      JSONObject obj = sc.getTable(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId);
      assertNull(obj);
      
      sc.close();
      
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
    SyncClient sc = null;
    int retCode;

    try {
      sc = new SyncClient();
      String agg_url = aggInfo.getHostUrl();
      agg_url = agg_url.substring(0, agg_url.length()-1);
      
      URL url = new URL(agg_url);
      String host = url.getHost();
      
      sc.init(host, aggInfo.getUserName(), aggInfo.getPassword());

      JSONObject result = sc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, null, csvFile);

      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = sc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(aggInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      UpdateTask task = new UpdateTask(aggInfo, dataPathAdd, version, testTableId, null, false);
      retCode = task.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject res = sc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 5);

      UpdateTask taskUpdate = new UpdateTask(aggInfo, dataPathUpdate, version, testTableId, null, false);
      retCode = taskUpdate.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      res = sc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, tableSchemaETag, null, null, null);

      rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 4);

      JSONObject jsonRow = null;
      for (int i = 0; i < rows.size(); i++) {
        jsonRow = rows.getJSONObject(i);
        String rowId = jsonRow.getString(SyncClient.ID_JSON);
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
      sc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = sc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskAllOperationsInFile_ExpectPass");
      e.printStackTrace();
      fail();
    }
  }
  
  // This test must be run with admin privileges!
  public void testUpdateTaskAddWithUsers_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String dataPath = absolutePathOfTestFiles + "plot/plot-add-user.csv";
    String userPath = absolutePathOfTestFiles + "permissions/perm-file.csv";
    String testTableId = "test7";
    String tableSchemaETag = null;
    SyncClient sc = null;
    int retCode;

    try {
      sc = new SyncClient();
      
      String agg_url = aggInfo.getHostUrl();
      agg_url = agg_url.substring(0, agg_url.length()-1);
      
      URL url = new URL(agg_url);
      String host = url.getHost();
      
      sc.init(host, aggInfo.getUserName(), aggInfo.getPassword());

      LoginTask lTask = new LoginTask(aggInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);
      
      // First check if this server allows permissions
      int rspCode = sc.uploadPermissionCSV(agg_url, appId, userPath);
      
      // This server does not use user permissions
      if (rspCode != 200) {
        return;
      }

      JSONObject result = sc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskAddWithUsers_ExpectPass: result is " + result);

      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = sc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      UpdateTask updateTask = new UpdateTask(aggInfo, dataPath, version, testTableId, null, false);
      retCode = updateTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject res = sc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag, null, null, null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Now check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      // Now delete the table
      sc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = sc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

      sc.close();

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskAddWithUsers_ExpectPass");
      e.printStackTrace();
      fail();
    }
  }
}
