package org.opendatakit.suitcase.test;

import java.io.File;
import java.net.URL;
import java.util.Scanner;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.aggregate.odktables.rest.SavepointTypeManipulator;
import org.opendatakit.sync.client.SyncClient;
import org.opendatakit.suitcase.net.LoginTask;
import org.opendatakit.suitcase.net.SuitcaseSwingWorker;
import org.opendatakit.suitcase.net.UpdateTask;
import org.opendatakit.suitcase.utils.SuitcaseConst;
import org.opendatakit.suitcase.model.CloudEndpointInfo;

import junit.framework.TestCase;

public class UpdateTaskTest extends TestCase{
  
  String serverUrl;
  String appId;
  String absolutePathOfTestFiles;
  String host;
  String userName;
  String password;
  int batchSize;
  String version;
  CloudEndpointInfo cloudEndpointInfo = null;
  
  /*
   * Perform setup for test if necessary
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    serverUrl = System.getProperty("test.aggUrl");
    appId = System.getProperty("test.appId");
    absolutePathOfTestFiles = System.getProperty("test.absolutePathOfTestFiles");
    batchSize = Integer.valueOf(System.getProperty("test.batchSize"));
    userName = System.getProperty("test.userName");
    password = System.getProperty("test.password");
  
//    serverUrl = "";
//    appId = "";
//    absolutePathOfTestFiles = "testfiles" + File.separator;
//    batchSize = 1000;
//    userName = "";
//    password = "";

    URL url = new URL(serverUrl);
    host = url.getHost();
    version = "2";
    cloudEndpointInfo = new CloudEndpointInfo(serverUrl, appId, userName, password);
  }
  
  public void testUpdateTaskAdd_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot" + File.separator + "definition.csv";
    String dataPath = absolutePathOfTestFiles + "plot" + File.separator + "plot-add.csv";
    String testTableId = "test1";
    String tableSchemaETag = null;
    SyncClient sc = null;
    int retCode;

    try {
      sc = new SyncClient();
      
      String cloud_endpoint_url = cloudEndpointInfo.getHostUrl();
      cloud_endpoint_url = cloud_endpoint_url.substring(0, cloud_endpoint_url.length()-1);
      
      URL url = new URL(cloud_endpoint_url);
      String host = url.getHost();
      
      sc.init(host, cloudEndpointInfo.getUserName(), cloudEndpointInfo.getPassword());

      LoginTask lTask = new LoginTask(cloudEndpointInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject result = sc.createTableWithCSV(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskAdd: result is " + result);

      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = sc.getTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      UpdateTask updateTask = new UpdateTask(cloudEndpointInfo, dataPath, version, testTableId, null, false);
      retCode = updateTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId,
          tableSchemaETag, null, null, null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Now check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      // Now delete the table
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = sc.getTables(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

      sc.close();

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskAdd_ExpectPass");
      e.printStackTrace();
      fail();
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }
  
  public void testUpdateTaskAddCustomLog_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot" + File.separator + "definition.csv";
    String dataPath = absolutePathOfTestFiles + "plot" + File.separator + "plot-add.csv";
    String logDataPath = absolutePathOfTestFiles + "plot" + File.separator + "log" + File.separator + "customOutcomeFile.txt";
    String testTableId = "test11";
    String tableSchemaETag = null;
    SyncClient sc = null;
    int retCode;

    try {
      sc = new SyncClient();
      
      String cloud_endpoint_url = cloudEndpointInfo.getHostUrl();
      cloud_endpoint_url = cloud_endpoint_url.substring(0, cloud_endpoint_url.length()-1);
      
      URL url = new URL(cloud_endpoint_url);
      String host = url.getHost();
      
      sc.init(host, cloudEndpointInfo.getUserName(), cloudEndpointInfo.getPassword());

      LoginTask lTask = new LoginTask(cloudEndpointInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject result = sc.createTableWithCSV(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskAdd: result is " + result);

      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = sc.getTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      UpdateTask updateTask = new UpdateTask(cloudEndpointInfo, dataPath, version, testTableId, logDataPath, false);
      retCode = updateTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId,
          tableSchemaETag, null, null, null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Now check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));
      
      // Check that the log file was created as expected
      Scanner scanner = null;
      File logFile = null;
      try {
        logFile = new File(logDataPath);
        assertTrue(logFile.exists());
        
        scanner = new Scanner(logFile);
        String line = scanner.nextLine();
      
        assertTrue(line.toLowerCase().contains("success"));
      } finally {
        if (scanner != null) {
          scanner.close();
        }
        if (logFile.exists()) {
          logFile.delete();
        }
      }

      // Now delete the table
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = sc.getTables(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

      sc.close();

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskAdd_ExpectPass");
      e.printStackTrace();
      fail();
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }
  
  public void testUpdateTaskDelete_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot" + File.separator + "definition.csv";
    String dataPathAdd = absolutePathOfTestFiles + "plot" + File.separator + "plot-add.csv";
    String dataPathDelete = absolutePathOfTestFiles + "plot" + File.separator + "plot-delete.csv";
    String testTableId = "test2";
    String tableSchemaETag = null;
    SyncClient sc = null;
    int retCode;

    try {
      sc = new SyncClient();
      String cloud_endpoint_url = cloudEndpointInfo.getHostUrl();
      cloud_endpoint_url = cloud_endpoint_url.substring(0, cloud_endpoint_url.length()-1);
      
      URL url = new URL(cloud_endpoint_url);
      String host = url.getHost();
      
      sc.init(host, cloudEndpointInfo.getUserName(), cloudEndpointInfo.getPassword());

      JSONObject result = sc.createTableWithCSV(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskDelete: result is " + result);

      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = sc.getTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(cloudEndpointInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      UpdateTask updateTask = new UpdateTask(cloudEndpointInfo, dataPathAdd, version, testTableId, null, false);
      retCode = updateTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Now check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      UpdateTask taskDelete = new UpdateTask(cloudEndpointInfo, dataPathDelete, version, testTableId, null, false);
      retCode = taskDelete.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 0);

      // Now delete the table
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, tableSchemaETag);
      
      // Check that table no longer exists
      JSONObject obj = sc.getTables(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));
      
      sc.close();
      
    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskDelete_ExpectPass");
      e.printStackTrace();
      fail();
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }
  
  public void testUpdateTaskUpdate_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot" + File.separator + "definition.csv";
    String dataPathAdd = absolutePathOfTestFiles + "plot" + File.separator + "plot-add.csv";
    String dataPathUpdate = absolutePathOfTestFiles + "plot" + File.separator + "plot-update.csv";
    String testTableId = "test3";
    String tableSchemaETag = null;
    SyncClient sc = null;
    int retCode;

    try {
      sc = new SyncClient();
      String cloud_endpoint_url = cloudEndpointInfo.getHostUrl();
      cloud_endpoint_url = cloud_endpoint_url.substring(0, cloud_endpoint_url.length()-1);
      
      URL url = new URL(cloud_endpoint_url);
      String host = url.getHost();
      
      sc.init(host, cloudEndpointInfo.getUserName(), cloudEndpointInfo.getPassword());

      JSONObject result = sc.createTableWithCSV(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskUpdate: result is " + result);

      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = sc.getTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(cloudEndpointInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      UpdateTask updateTask = new UpdateTask(cloudEndpointInfo, dataPathAdd, version, testTableId, null, false);
      retCode = updateTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId,
          tableSchemaETag, null, null, null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      UpdateTask taskUpdate = new UpdateTask(cloudEndpointInfo, dataPathUpdate, version, testTableId, null, false);
      retCode = taskUpdate.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId,
          tableSchemaETag, null, null, null);

      rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Clarice", jsonRow));

      // Now delete the table
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId,
          tableSchemaETag);
      
      // Check that table no longer exists
      JSONObject obj = sc.getTables(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

      sc.close();

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskUpdate_ExpectPass");
      e.printStackTrace();
      fail();
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }
  
  public void testUpdateTaskForceUpdate_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot" + File.separator + "definition.csv";
    String dataPathAdd = absolutePathOfTestFiles + "plot" + File.separator + "plot-add.csv";
    String dataPathUpdate = absolutePathOfTestFiles + "plot" + File.separator + "plot-forceUpdate.csv";
    String testTableId = "test4";
    String tableSchemaETag = null;
    SyncClient sc = null;
    int retCode;

    try {
      sc = new SyncClient();
      String cloud_endpoint_url = cloudEndpointInfo.getHostUrl();
      cloud_endpoint_url = cloud_endpoint_url.substring(0, cloud_endpoint_url.length()-1);
      
      URL url = new URL(cloud_endpoint_url);
      String host = url.getHost();
      
      sc.init(host, cloudEndpointInfo.getUserName(), cloudEndpointInfo.getPassword());

      JSONObject result = sc.createTableWithCSV(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskForceUpdate: result is " + result);

      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = sc.getTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(cloudEndpointInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      UpdateTask task = new UpdateTask(cloudEndpointInfo, dataPathAdd, version, testTableId, null, false);
      retCode = task.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      UpdateTask taskUpdate = new UpdateTask(cloudEndpointInfo, dataPathUpdate, version, testTableId, null, false);
      retCode = taskUpdate.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, tableSchemaETag, null, null, null);

      rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Clarice", jsonRow));

      // Now delete the table
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = sc.getTables(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskForceUpdate_ExpectPass");
      e.printStackTrace();
      fail();
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }
  
  public void testUpdateTaskForceUpdateToAddRow_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot" + File.separator + "definition.csv";
    String dataPathUpdate = absolutePathOfTestFiles + "plot" + File.separator + "plot-forceUpdate.csv";
    String testTableId = "test4";
    String tableSchemaETag = null;
    SyncClient sc = null;
    int retCode;

    try {
      sc = new SyncClient();
      String cloud_endpoint_url = cloudEndpointInfo.getHostUrl();
      cloud_endpoint_url = cloud_endpoint_url.substring(0, cloud_endpoint_url.length()-1);
      
      URL url = new URL(cloud_endpoint_url);
      String host = url.getHost();
      
      sc.init(host, cloudEndpointInfo.getUserName(), cloudEndpointInfo.getPassword());

      JSONObject result = sc.createTableWithCSV(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskForceUpdate: result is " + result);

      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = sc.getTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(cloudEndpointInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      UpdateTask task = new UpdateTask(cloudEndpointInfo, dataPathUpdate, version, testTableId, null, false);
      retCode = task.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      // Now delete the table
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = sc.getTables(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskForceUpdate_ExpectPass");
      e.printStackTrace();
      fail();
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }
  
  public void testUpdateTaskForceUpdateDeleteAndRecreateIt_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot" + File.separator + "definition.csv";
    String dataPathUpdate = absolutePathOfTestFiles + "plot" + File.separator + "plot-forceUpdate.csv";
    String dataPathDelete = absolutePathOfTestFiles + "plot" + File.separator + "plot-forceUpdateDelete.csv";
    String testTableId = "test4";
    String tableSchemaETag = null;
    SyncClient sc = null;
    int retCode;

    try {
      sc = new SyncClient();
      String cloud_endpoint_url = cloudEndpointInfo.getHostUrl();
      cloud_endpoint_url = cloud_endpoint_url.substring(0, cloud_endpoint_url.length()-1);
      
      URL url = new URL(cloud_endpoint_url);
      String host = url.getHost();
      
      sc.init(host, cloudEndpointInfo.getUserName(), cloudEndpointInfo.getPassword());

      JSONObject result = sc.createTableWithCSV(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testUpdateTaskForceUpdate: result is " + result);

      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = sc.getTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(cloudEndpointInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      UpdateTask task = new UpdateTask(cloudEndpointInfo, dataPathUpdate, version, testTableId, null, false);
      retCode = task.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      JSONObject jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));
      
      UpdateTask taskDelete = new UpdateTask(cloudEndpointInfo, dataPathDelete, version, testTableId, null, false);
      retCode = taskDelete.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);
      
      res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, tableSchemaETag, null, null, null);

      rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 0);

      UpdateTask taskUpdate = new UpdateTask(cloudEndpointInfo, dataPathUpdate, version, testTableId, null, false);
      retCode = taskUpdate.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, tableSchemaETag, null, null, null);

      rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);

      jsonRow = rows.getJSONObject(0);

      // Check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));

      assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Clarice", jsonRow));

      // Now delete the table
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = sc.getTables(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskForceUpdate_ExpectPass");
      e.printStackTrace();
      fail();
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }
  
  public void testUpdateTaskWith1000Rows_ExpectPass() {
    String testTableId = "test5";
    String defPath =absolutePathOfTestFiles + "cookstoves" + File.separator + "data_definition.csv";
    String dataPath = absolutePathOfTestFiles + "cookstoves" + File.separator + "data_small.csv";
    int retCode;
    SyncClient sc = null;
    
    try {
      sc = new SyncClient();
      String cloud_endpoint_url = cloudEndpointInfo.getHostUrl();
      cloud_endpoint_url = cloud_endpoint_url.substring(0, cloud_endpoint_url.length()-1);
      
      URL url = new URL(cloud_endpoint_url);
      String host = url.getHost();
      
      sc.init(host, cloudEndpointInfo.getUserName(), cloudEndpointInfo.getPassword());
      
      LoginTask lTask = new LoginTask(cloudEndpointInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);
      
      sc.createTableWithCSV(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, null, defPath);
      
      String schemaETag = sc.getSchemaETagForTable(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId);
      JSONObject tableDefObj = sc.getTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, schemaETag);
    
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(defPath, tableDefObj));
      
      // Need to add rows
      UpdateTask updateTask = new UpdateTask(cloudEndpointInfo, dataPath, version, testTableId, null, false);
      retCode = updateTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);
      
      JSONObject rowsObj = sc.getRows(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, schemaETag, null, null);
      JSONArray rows = rowsObj.getJSONArray(SyncClient.ROWS_STR_JSON);
      
      assertEquals(rows.size(), 1000);
      
      // Then delete table definition
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, schemaETag);
      
      JSONObject obj = sc.getTable(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId);
      assertNull(obj);
      
      sc.close();
      
    } catch (Exception e) {
      System.out.println("TableTaskTest: Exception thrown in testUpdateTaskWith1000Rows_ExpectPass");
      e.printStackTrace();
      fail();
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }
  
  public void testUpdateTaskAllOperationsInFile_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot" + File.separator + "definition.csv";
    String dataPathAdd = absolutePathOfTestFiles + "plot" + File.separator + "plot-add5.csv";
    String dataPathUpdate = absolutePathOfTestFiles + "plot" + File.separator + "plot-allops.csv";
    String testTableId = "test6";
    String tableSchemaETag = null;
    SyncClient sc = null;
    int retCode;

    try {
      sc = new SyncClient();
      String cloud_endpoint_url = cloudEndpointInfo.getHostUrl();
      cloud_endpoint_url = cloud_endpoint_url.substring(0, cloud_endpoint_url.length()-1);
      
      URL url = new URL(cloud_endpoint_url);
      String host = url.getHost();
      
      sc.init(host, cloudEndpointInfo.getUserName(), cloudEndpointInfo.getPassword());

      JSONObject result = sc.createTableWithCSV(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, null, csvFile);

      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = sc.getTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(cloudEndpointInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      UpdateTask task = new UpdateTask(cloudEndpointInfo, dataPathAdd, version, testTableId, null, false);
      retCode = task.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 5);

      UpdateTask taskUpdate = new UpdateTask(cloudEndpointInfo, dataPathUpdate, version, testTableId, null, false);
      retCode = taskUpdate.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, tableSchemaETag, null, null, null);

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
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = sc.getTables(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskAllOperationsInFile_ExpectPass");
      e.printStackTrace();
      fail();
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }
  
  public void testUpdateTaskAddUpdateForceUpdateInFile_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot" + File.separator + "definition.csv";
    String dataPathAdd = absolutePathOfTestFiles + "plot" + File.separator + "plot-add5.csv";
    String dataPathUpdate = absolutePathOfTestFiles + "plot" + File.separator + "plot-addUpdateForceUpdate.csv";
    String testTableId = "test6";
    String tableSchemaETag = null;
    SyncClient sc = null;
    int retCode;

    try {
      sc = new SyncClient();
      String cloud_endpoint_url = cloudEndpointInfo.getHostUrl();
      cloud_endpoint_url = cloud_endpoint_url.substring(0, cloud_endpoint_url.length()-1);
      
      URL url = new URL(cloud_endpoint_url);
      String host = url.getHost();
      
      sc.init(host, cloudEndpointInfo.getUserName(), cloudEndpointInfo.getPassword());

      JSONObject result = sc.createTableWithCSV(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, null, csvFile);

      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = sc.getTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(cloudEndpointInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      UpdateTask task = new UpdateTask(cloudEndpointInfo, dataPathAdd, version, testTableId, null, false);
      retCode = task.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 5);

      UpdateTask taskUpdate = new UpdateTask(cloudEndpointInfo, dataPathUpdate, version, testTableId, null, false);
      retCode = taskUpdate.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, tableSchemaETag, null, null, null);

      rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 7);

      JSONObject jsonRow = null;
      for (int i = 0; i < rows.size(); i++) {
        jsonRow = rows.getJSONObject(i);
        String rowId = jsonRow.getString(SyncClient.ID_JSON);
        if (!rowId.equals("1") && !rowId.equals("2") &&
            !rowId.equals("3") && !rowId.equals("4") &&
            !rowId.equals("5") && !rowId.equals("7") &&
            !rowId.equals("8") && !rowId.equals("10")) {
          System.out
          .println("UpdateTaskTest: unexpected row id " + rowId + " in testUpdateTaskAllOperationsInFile_ExpectPass");
          fail();
        } else {
          verifyMetadataValues(jsonRow);
          
          if (rowId.equals("2")) {
            assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Clarice", jsonRow));
          } else if (rowId.equals("3")) {
            assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Clarlars", jsonRow));
          } else if (rowId.equals("10")) {
            assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Lars", jsonRow));
          } else if (rowId.equals("8")) {
            assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Quincy", jsonRow));
          }
        }
      }

      // Now delete the table
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = sc.getTables(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskAllOperationsInFile_ExpectPass");
      e.printStackTrace();
      fail();
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }

  private void verifyMetadataValues(JSONObject jsonRow) throws JSONException {
    assertEquals(jsonRow.getString(SyncClient.LOCALE_JSON), SuitcaseConst.DEFAULT_LOCALE);
    assertEquals(jsonRow.getString(SyncClient.SAVEPOINT_TYPE_JSON), SavepointTypeManipulator.complete());
    
    String creator = jsonRow.getString(SyncClient.SAVEPOINT_CREATOR_JSON);
    String expectedCreator = SuitcaseConst.ANONYMOUS_USER;
    if (creator.startsWith(SuitcaseConst.MAIL_TO_PREFIX)) {
      expectedCreator = SuitcaseConst.MAIL_TO_PREFIX + userName;
    } else if (creator.startsWith(SuitcaseConst.USERNAME_PREFIX)) {
      expectedCreator = SuitcaseConst.USERNAME_PREFIX + userName;
    }
    
    assertEquals(creator, expectedCreator);
    
    assertNotNull(jsonRow.getString(SyncClient.SAVEPOINT_TIMESTAMP_JSON));
  }
  
  public void testUpdateTaskAddUpdateForceUpdateInFileDiffColOrder_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot" + File.separator + "definition.csv";
    String dataPathAdd = absolutePathOfTestFiles + "plot" + File.separator + "plot-add5.csv";
    String dataPathUpdate = absolutePathOfTestFiles + "plot" + File.separator + "plot-addUpdateForceUpdate2.csv";
    String testTableId = "test6";
    String tableSchemaETag = null;
    SyncClient sc = null;
    int retCode;

    try {
      sc = new SyncClient();
      String cloud_endpoint_url = cloudEndpointInfo.getHostUrl();
      cloud_endpoint_url = cloud_endpoint_url.substring(0, cloud_endpoint_url.length()-1);
      
      URL url = new URL(cloud_endpoint_url);
      String host = url.getHost();
      
      sc.init(host, cloudEndpointInfo.getUserName(), cloudEndpointInfo.getPassword());

      JSONObject result = sc.createTableWithCSV(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, null, csvFile);

      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
        assertEquals(tableId, testTableId);
        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
      }

      // Get the table definition
      JSONObject tableDef = sc.getTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, tableSchemaETag);

      // Make sure it is the same as the csv definition
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

      LoginTask lTask = new LoginTask(cloudEndpointInfo, false);
      retCode = lTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      UpdateTask task = new UpdateTask(cloudEndpointInfo, dataPathAdd, version, testTableId, null, false);
      retCode = task.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, tableSchemaETag, null, null,
          null);

      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 5);

      UpdateTask taskUpdate = new UpdateTask(cloudEndpointInfo, dataPathUpdate, version, testTableId, null, false);
      retCode = taskUpdate.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, tableSchemaETag, null, null, null);

      rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 7);

      JSONObject jsonRow = null;
      for (int i = 0; i < rows.size(); i++) {
        jsonRow = rows.getJSONObject(i);
        String rowId = jsonRow.getString(SyncClient.ID_JSON);
        if (!rowId.equals("1") && !rowId.equals("2") &&
            !rowId.equals("3") && !rowId.equals("4") &&
            !rowId.equals("5") && !rowId.equals("7") &&
            !rowId.equals("8") && !rowId.equals("10")) {
          System.out
          .println("UpdateTaskTest: unexpected row id " + rowId + " in testUpdateTaskAllOperationsInFile_ExpectPass");
          fail();
        } else { 
          verifyMetadataValues(jsonRow);
          
          if (rowId.equals("2")) {
            assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Clarice", jsonRow));
          } else if (rowId.equals("3")) {
            assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Clarlars", jsonRow));
          } else if (rowId.equals("10")) {
            assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Lars", jsonRow));
          } else if (rowId.equals("8")) {
            assertTrue(TestUtilities.checkThatRowHasColumnValue("plot_name", "Quincy", jsonRow));
          }
        }
      }

      // Now delete the table
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = sc.getTables(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskAllOperationsInFile_ExpectPass");
      e.printStackTrace();
      fail();
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }
  
  // This test must be run with admin privileges!
//  public void testUpdateTaskAddWithUsers_ExpectPass() {
//    String csvFile = absolutePathOfTestFiles + "plot" + File.separator + "definition.csv";
//    String dataPath = absolutePathOfTestFiles + "plot" + File.separator + "plot-add-user.csv";
//    String userPath = absolutePathOfTestFiles + "permissions" + File.separator + "perm-file.csv";
//    String testTableId = "test7";
//    String tableSchemaETag = null;
//    SyncClient sc = null;
//    int retCode;
//
//    try {
//      sc = new SyncClient();
//      
//      String cloud_endpoint_url = cloudEndpointInfo.getHostUrl();
//      cloud_endpoint_url = cloud_endpoint_url.substring(0, cloud_endpoint_url.length()-1);
//      
//      URL url = new URL(cloud_endpoint_url);
//      String host = url.getHost();
//      
//      sc.init(host, cloudEndpointInfo.getUserName(), cloudEndpointInfo.getPassword());
//
//      LoginTask lTask = new LoginTask(cloudEndpointInfo, false);
//      retCode = lTask.blockingExecute();
//      assertEquals(retCode, SuitcaseSwingWorker.okCode);
//      
//      // First check if this server allows permissions
//      int rspCode = sc.uploadPermissionCSV(cloud_endpoint_url, appId, userPath);
//      
//      // This server does not use user permissions
//      if (rspCode != 200) {
//        return;
//      }
//
//      JSONObject result = sc.createTableWithCSV(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
//          testTableId, null, csvFile);
//      System.out.println("testUpdateTaskAddWithUsers_ExpectPass: result is " + result);
//
//      if (result.containsKey(SyncClient.TABLE_ID_JSON)) {
//        String tableId = result.getString(SyncClient.TABLE_ID_JSON);
//        assertEquals(tableId, testTableId);
//        tableSchemaETag = result.getString(SyncClient.SCHEMA_ETAG_JSON);
//      }
//
//      // Get the table definition
//      JSONObject tableDef = sc.getTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
//          testTableId, tableSchemaETag);
//
//      // Make sure it is the same as the csv definition
//      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));
//
//      UpdateTask updateTask = new UpdateTask(cloudEndpointInfo, dataPath, version, testTableId, null, false);
//      retCode = updateTask.blockingExecute();
//      assertEquals(retCode, SuitcaseSwingWorker.okCode);
//
//      JSONObject res = sc.getRowsSince(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId,
//          tableSchemaETag, null, null, null);
//
//      JSONArray rows = res.getJSONArray("rows");
//
//      assertEquals(rows.size(), 1);
//
//      JSONObject jsonRow = rows.getJSONObject(0);
//
//      // Now check that the row was created with the right rowId
//      assertTrue(TestUtilities.checkThatRowHasId("12", jsonRow));
//
//      // Now delete the table
//      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId,
//          tableSchemaETag);
//
//      // Check that table no longer exists
//      JSONObject obj = sc.getTables(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId());
//      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));
//
//      sc.close();
//
//    } catch (Exception e) {
//      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskAddWithUsers_ExpectPass");
//      e.printStackTrace();
//      fail();
//    } 
//    finally {
//      if (sc != null) {
//        sc.close();
//      }
//    }
//  }
}
