package org.opendatakit.suitcase.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.suitcase.model.CloudEndpointInfo;
import org.opendatakit.suitcase.net.LoginTask;
import org.opendatakit.suitcase.net.SuitcaseSwingWorker;
import org.opendatakit.suitcase.net.TableTask;
import org.opendatakit.suitcase.net.UpdateTask;
import org.opendatakit.sync.client.SyncClient;

import junit.framework.TestCase;

public class TableTaskTest extends TestCase{

  CloudEndpointInfo cloudEndpointInfo = null;
  String serverUrl = null;
  String appId = null;
  String absolutePathOfTestFiles = null;
  String userName = null;
  String password = null;
  String version = null;
  
  @Override
  protected void setUp() throws MalformedURLException {
//    serverUrl = "";
//    appId = "";
//    absolutePathOfTestFiles = "testfiles" + File.separator;
//    userName = "";
//    password = "";
	  
    serverUrl = System.getProperty("test.aggUrl");
    appId = System.getProperty("test.appId");
    absolutePathOfTestFiles = System.getProperty("test.absolutePathOfTestFiles");
    userName = System.getProperty("test.userName");
    password = System.getProperty("test.password");
    version = "2";
    cloudEndpointInfo = new CloudEndpointInfo(serverUrl, appId, userName, password);
  }
  
  public void testCreateTable_ExpectPass() {
    String testTableId = "test1";
    String operation = "create";
    String dataPath = absolutePathOfTestFiles + "plot" + File.separator + "definition.csv";
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
      
      TableTask tTask = new TableTask(cloudEndpointInfo, testTableId, dataPath, version, operation, false);
      retCode = tTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);
      
      String schemaETag = sc.getSchemaETagForTable(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId);
      JSONObject tableDefObj = sc.getTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, schemaETag);
    
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(dataPath, tableDefObj));
    
      // Then delete table definition
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, schemaETag);
      
      JSONObject obj = sc.getTable(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId);
      assertNull(obj);
      
    } catch (Exception e) {
      System.out.println("TableTaskTest: Exception thrown in createTableTest");
      e.printStackTrace();
      fail(); 
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }
  
  public void testDeleteTable_ExpectPass() {
    String testTableId = "test2";
    String operation = "delete";
    String dataPath = absolutePathOfTestFiles + "plot" + File.separator + "definition.csv";
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

      String schemaETag = null;
      sc.createTableWithCSV(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, schemaETag,
          dataPath);
      schemaETag = sc
          .getSchemaETagForTable(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId);

      JSONObject tableDefObj = sc.getTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(),
          testTableId, schemaETag);
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(dataPath, tableDefObj));

      // operation
      TableTask tTask = new TableTask(cloudEndpointInfo, testTableId, dataPath, version, operation, false);
      retCode = tTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      JSONObject obj = sc.getTable(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId);
      assertNull(obj);

      sc.close();

    } catch (Exception e) {
      System.out.println("TableTaskTest: Exception thrown in testDeleteTable_ExpectPass");
      e.printStackTrace();
      fail();
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }
  
  public void testClearTable_ExpectPass() {
    String testTableId = "test3";
    String operation = "clear";
    String defPath = absolutePathOfTestFiles + "plot" + File.separator + "definition.csv";
    String dataPath = absolutePathOfTestFiles + "plot" + File.separator + "plot-add5.csv";
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
      
      assertEquals(rows.size(), 5);
      
      TableTask tTask = new TableTask(cloudEndpointInfo, testTableId, dataPath, version, operation, false);
      retCode = tTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);
      
      rowsObj = sc.getRows(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, schemaETag, null, null);
      rows = rowsObj.getJSONArray(SyncClient.ROWS_STR_JSON);
      
      assertEquals(rows.size(), 0);
      
      // Then delete table definition
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, schemaETag);
      
      JSONObject obj = sc.getTable(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId);
      assertNull(obj);
      
    } catch (Exception e) {
      System.out.println("TableTaskTest: Exception thrown in testClearTable_ExpectPass");
      e.printStackTrace();
      fail();
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }
  
  public void testClearTableNoFilePath_ExpectPass() {
    String testTableId = "test4";
    String operation = "clear";
    String defPath = absolutePathOfTestFiles + "plot" + File.separator + "definition.csv";
    String dataPath = absolutePathOfTestFiles + "plot" + File.separator + "plot-add5.csv";
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
      
      assertEquals(rows.size(), 5);
      
      TableTask tTask = new TableTask(cloudEndpointInfo, testTableId, null, version, operation, false);
      retCode = tTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);
      
      rowsObj = sc.getRows(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, schemaETag, null, null);
      rows = rowsObj.getJSONArray(SyncClient.ROWS_STR_JSON);
      
      assertEquals(rows.size(), 0);
      
      // Then delete table definition
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, schemaETag);
      
      JSONObject obj = sc.getTable(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId);
      assertNull(obj);
      
    } catch (Exception e) {
      System.out.println("TableTaskTest: Exception thrown in testClearTableNoFilePath_ExpectPass");
      e.printStackTrace();
      fail();
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }
  
  public void testClearLargeTableData_ExpectPass() {
    String testTableId = "test5";
    String operation = "clear";
    String defPath = absolutePathOfTestFiles + "cookstoves" + File.separator + "data_definition.csv";
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
      
      TableTask tTask = new TableTask(cloudEndpointInfo, testTableId, null, version, operation, false);
      retCode = tTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);
      
      rowsObj = sc.getRows(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, schemaETag, null, null);
      rows = rowsObj.getJSONArray(SyncClient.ROWS_STR_JSON);
      
      assertEquals(rows.size(), 0);
      
      // Then delete table definition
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, schemaETag);
      
      JSONObject obj = sc.getTable(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId);
      assertNull(obj);
      
    } catch (Exception e) {
      System.out.println("TableTaskTest: Exception thrown in testClearLargeTableData_ExpectPass");
      e.printStackTrace();
      fail();
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }
  
  public void testClearEmptyTable_ExpectPass() {
    String testTableId = "test6";
    String operation = "clear";
    String defPath = absolutePathOfTestFiles + "cookstoves" + File.separator + "data_definition.csv";
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
      
      JSONObject rowsObj = sc.getRows(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, schemaETag, null, null);
      JSONArray rows = rowsObj.getJSONArray(SyncClient.ROWS_STR_JSON);
      
      assertEquals(rows.size(), 0);
      
      TableTask tTask = new TableTask(cloudEndpointInfo, testTableId, null, version, operation, false);
      retCode = tTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);
      
      rowsObj = sc.getRows(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, schemaETag, null, null);
      rows = rowsObj.getJSONArray(SyncClient.ROWS_STR_JSON);
      
      assertEquals(rows.size(), 0);
      
      // Then delete table definition
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId, schemaETag);
      
      JSONObject obj = sc.getTable(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), testTableId);
      assertNull(obj);
      
    } catch (Exception e) {
      System.out.println("TableTaskTest: Exception thrown in testClearLargeTableData_ExpectPass");
      e.printStackTrace();
      fail();
    } finally {
      if (sc != null) {
        sc.close();
      }
    }
  }
}
