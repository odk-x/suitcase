package org.opendatakit.suitcase.test;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.suitcase.model.AggregateInfo;
import org.opendatakit.suitcase.net.LoginTask;
import org.opendatakit.suitcase.net.TableTask;
import org.opendatakit.suitcase.net.UpdateTask;
import org.opendatakit.wink.client.WinkClient;

import junit.framework.TestCase;

public class TableTaskTest extends TestCase{

  AggregateInfo aggInfo = null;
  String serverUrl = null;
  String appId = null;
  String userName = null;
  String password = null;
  String version = null;
  
  @Override
  protected void setUp() throws MalformedURLException {
    serverUrl = "https://test.appspot.com";
    appId = "default";
    userName = "";
    password = "";
    version = "2";
    aggInfo = new AggregateInfo(serverUrl, appId, userName, password); 
  }
  
  public void testCreateTable_ExpectPass() {
    String testTableId = "test1";
    String operation = "create";
    String dataPath = "testfiles/plot/definition.csv";
    
    try {
      WinkClient wc = new WinkClient();
      
      String agg_url = aggInfo.getHostUrl();
      agg_url = agg_url.substring(0, agg_url.length()-1);
      
      URL url = new URL(agg_url);
      String host = url.getHost();
      
      wc.init(host, aggInfo.getUserName(), aggInfo.getPassword());
      
      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();
      
      //AggregateInfo aggInfo, String tableId, String dataPath, String operation
      TableTask tTask = new TableTask(aggInfo, testTableId, dataPath, version, operation, false);
      tTask.blockingExecute();
      
      String schemaETag = wc.getSchemaETagForTable(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId);
      JSONObject tableDefObj = wc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag);
    
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(dataPath, tableDefObj));
    
      // Then delete table definition
      wc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag);
      
      JSONObject obj = wc.getTable(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId);
      assertNull(obj);
      
      wc.close();
    } catch (Exception e) {
      System.out.println("TableTaskTest: Exception thrown in createTableTest");
      e.printStackTrace();
      fail(); 
    }
  }
  
  public void testDeleteTable_ExpectPass() {
    String testTableId = "test2";
    String operation = "delete";
    String dataPath = "testfiles/plot/definition.csv";

    try {
      WinkClient wc = new WinkClient();
      
      String agg_url = aggInfo.getHostUrl();
      agg_url = agg_url.substring(0, agg_url.length()-1);
      
      URL url = new URL(agg_url);
      String host = url.getHost();
      
      wc.init(host, aggInfo.getUserName(), aggInfo.getPassword());

      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();

      String schemaETag = null;
      wc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag,
          dataPath);
      schemaETag = wc
          .getSchemaETagForTable(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId);

      JSONObject tableDefObj = wc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, schemaETag);
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(dataPath, tableDefObj));

      // AggregateInfo aggInfo, String tableId, String dataPath, String
      // operation
      TableTask tTask = new TableTask(aggInfo, testTableId, dataPath, version, operation, false);

      tTask.blockingExecute();

      JSONObject obj = wc.getTable(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId);
      assertNull(obj);

      wc.close();

    } catch (Exception e) {
      System.out.println("TableTaskTest: Exception thrown in testDeleteTable_ExpectPass");
      e.printStackTrace();
      fail();
    }
  }
  
  public void testClearTable_ExpectPass() {
    String testTableId = "test3";
    String operation = "clear";
    String defPath = "testfiles/plot/definition.csv";
    String dataPath = "testfiles/plot/plot-add5.csv";
    
    try {
      WinkClient wc = new WinkClient();
      
      String agg_url = aggInfo.getHostUrl();
      agg_url = agg_url.substring(0, agg_url.length()-1);
      
      URL url = new URL(agg_url);
      String host = url.getHost();
      
      wc.init(host, aggInfo.getUserName(), aggInfo.getPassword());
      
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
      
      assertEquals(rows.size(), 5);
      
      TableTask tTask = new TableTask(aggInfo, testTableId, dataPath, version, operation, false);
      tTask.blockingExecute();
      
      rowsObj = wc.getRows(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag, null, null);
      rows = rowsObj.getJSONArray(WinkClient.ROWS_STR_JSON);
      
      assertEquals(rows.size(), 0);
      
      // Then delete table definition
      wc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag);
      
      JSONObject obj = wc.getTable(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId);
      assertNull(obj);
      
      wc.close();
      
    } catch (Exception e) {
      System.out.println("TableTaskTest: Exception thrown in testClearTable_ExpectPass");
      e.printStackTrace();
      fail();
    } 
  }
  
  public void testClearTableNoFilePath_ExpectPass() {
    String testTableId = "test4";
    String operation = "clear";
    String defPath = "testfiles/plot/definition.csv";
    String dataPath = "testfiles/plot/plot-add5.csv";
    
    try {
      WinkClient wc = new WinkClient();
      
      String agg_url = aggInfo.getHostUrl();
      agg_url = agg_url.substring(0, agg_url.length()-1);
      
      URL url = new URL(agg_url);
      String host = url.getHost();
      
      wc.init(host, aggInfo.getUserName(), aggInfo.getPassword());
      
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
      
      assertEquals(rows.size(), 5);
      
      TableTask tTask = new TableTask(aggInfo, testTableId, null, version, operation, false);
      tTask.blockingExecute();
      
      rowsObj = wc.getRows(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag, null, null);
      rows = rowsObj.getJSONArray(WinkClient.ROWS_STR_JSON);
      
      assertEquals(rows.size(), 0);
      
      // Then delete table definition
      wc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag);
      
      JSONObject obj = wc.getTable(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId);
      assertNull(obj);
      
      wc.close();
      
    } catch (Exception e) {
      System.out.println("TableTaskTest: Exception thrown in testClearTableNoFilePath_ExpectPass");
      e.printStackTrace();
      fail();
    } 
  }
  
  public void testClearLargeTableData_ExpectPass() {
    String testTableId = "test5";
    String operation = "clear";
    String defPath = "testfiles/cookstoves/data_definition.csv";
    String dataPath = "testfiles/cookstoves/data_small.csv";
    
    try {
      WinkClient wc = new WinkClient();
      
      String agg_url = aggInfo.getHostUrl();
      agg_url = agg_url.substring(0, agg_url.length()-1);
      
      URL url = new URL(agg_url);
      String host = url.getHost();
      
      wc.init(host, aggInfo.getUserName(), aggInfo.getPassword());
      
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
      
      TableTask tTask = new TableTask(aggInfo, testTableId, null, version, operation, false);
      tTask.blockingExecute();
      
      rowsObj = wc.getRows(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag, null, null);
      rows = rowsObj.getJSONArray(WinkClient.ROWS_STR_JSON);
      
      assertEquals(rows.size(), 0);
      
      // Then delete table definition
      wc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag);
      
      JSONObject obj = wc.getTable(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId);
      assertNull(obj);
      
      wc.close();
      
    } catch (Exception e) {
      System.out.println("TableTaskTest: Exception thrown in testClearLargeTableData_ExpectPass");
      e.printStackTrace();
      fail();
    } 
  }
  
  public void testClearEmptyTable_ExpectPass() {
    String testTableId = "test6";
    String operation = "clear";
    String defPath = "testfiles/cookstoves/data_definition.csv";
    
    try {
      WinkClient wc = new WinkClient();
      
      String agg_url = aggInfo.getHostUrl();
      agg_url = agg_url.substring(0, agg_url.length()-1);
      
      URL url = new URL(agg_url);
      String host = url.getHost();
      
      wc.init(host, aggInfo.getUserName(), aggInfo.getPassword());
      
      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();
      
      //AggregateInfo aggInfo, String tableId, String dataPath, String operation
      wc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, null, defPath);
      
      String schemaETag = wc.getSchemaETagForTable(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId);
      JSONObject tableDefObj = wc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag);
    
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(defPath, tableDefObj));
      
      JSONObject rowsObj = wc.getRows(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag, null, null);
      JSONArray rows = rowsObj.getJSONArray(WinkClient.ROWS_STR_JSON);
      
      assertEquals(rows.size(), 0);
      
      TableTask tTask = new TableTask(aggInfo, testTableId, null, version, operation, false);
      tTask.blockingExecute();
      
      rowsObj = wc.getRows(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag, null, null);
      rows = rowsObj.getJSONArray(WinkClient.ROWS_STR_JSON);
      
      assertEquals(rows.size(), 0);
      
      // Then delete table definition
      wc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag);
      
      JSONObject obj = wc.getTable(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId);
      assertNull(obj);
      
      wc.close();
      
    } catch (Exception e) {
      System.out.println("TableTaskTest: Exception thrown in testClearLargeTableData_ExpectPass");
      e.printStackTrace();
      fail();
    } 
  }
}
