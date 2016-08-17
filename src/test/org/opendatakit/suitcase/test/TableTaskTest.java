package org.opendatakit.suitcase.test;

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
  protected void setUp() throws Exception {
    serverUrl = "https://clarlars.appspot.com";
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
      wc.init(aggInfo.getHostUrl(), aggInfo.getUserName(), aggInfo.getPassword());
      
      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();
      
      //AggregateInfo aggInfo, String tableId, String dataPath, String operation
      TableTask tTask = new TableTask(aggInfo, testTableId, dataPath, version, operation);
      tTask.blockingExecute();
      
      String schemaETag = wc.getSchemaETagForTable(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId);
      JSONObject tableDefObj = wc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag);
    
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(dataPath, tableDefObj));
    
      // Then delete table definition
      wc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag);
      
      JSONObject obj = wc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, schemaETag));
      
      wc.close();
    } catch (Exception e) {
      System.out.println("TableTaskTest: exception thrown in createTableTest");
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
      wc.init(aggInfo.getHostUrl(), aggInfo.getUserName(), aggInfo.getPassword());
      
      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();
      
      String schemaETag = null;
      wc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag, dataPath);
      schemaETag = wc.getSchemaETagForTable(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId);

      JSONObject tableDefObj = wc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag);
      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(dataPath, tableDefObj));
      
      //AggregateInfo aggInfo, String tableId, String dataPath, String operation
      TableTask tTask = new TableTask(aggInfo, testTableId, dataPath, version, operation);
      
      tTask.blockingExecute();
      
      // CAL: getTable should be fixed to return null if the table does not
      // exist and not an exception TODO
      JSONObject obj = wc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      
      // CAL: Should this just become a WinkClient call?
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, schemaETag));

      wc.close();
    
    } catch (Exception e) {
      System.out.println("TableTaskTest: exception thrown in deleteTableTest");
      e.printStackTrace();
    }
  }
  
  public void testClearTable_ExpectPass() {
    String testTableId = "test3";
    String operation = "clear";
    String defPath = "testfiles/plot/definition.csv";
    String dataPath = "testfiles/plot/plot-add6.csv";
    
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
      UpdateTask updateTask = new UpdateTask(aggInfo, dataPath, version, testTableId, false);
      updateTask.blockingExecute();
      
      TableTask tTask = new TableTask(aggInfo, testTableId, dataPath, version, operation);
      tTask.blockingExecute();
      
      JSONObject rowsObj = wc.getRows(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag, null, null);
      JSONArray rows = rowsObj.getJSONArray(WinkClient.ROWS_STR_JSON);
      
      assertEquals(rows.size(), 0);
      
      // Then delete table definition
      wc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId, schemaETag);
      
      JSONObject obj = wc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, schemaETag));
      
      wc.close();
      
    } catch (Exception e) {
      System.out.println("TableTaskTest: exception thrown in clearTableTest");
      e.printStackTrace();
    }
  }
  
  // TODO: Clear a large table
  public void testClearLargeTableData_ExpectPass() {
  }
}
