package org.opendatakit.suitcase.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.suitcase.model.AggregateInfo;
import org.opendatakit.suitcase.model.CsvConfig;
import org.opendatakit.suitcase.model.ODKCsv;
import org.opendatakit.suitcase.net.AttachmentManager;
import org.opendatakit.suitcase.net.DownloadTask;
import org.opendatakit.suitcase.net.LoginTask;
import org.opendatakit.suitcase.net.UpdateTask;
import org.opendatakit.wink.client.WinkClient;

public class DownloadTaskTest extends TestCase{
  
  AggregateInfo aggInfo = null;
  String serverUrl = null;
  String appId = null;
  String absolutePathOfTestFiles = null;
  String userName = null;
  String password = null;
  String version = null;
  
  @Override
  protected void setUp() throws MalformedURLException {
    serverUrl = "";
    appId = "default";
    absolutePathOfTestFiles = "testfiles/";
    userName = "";
    password = "";
    version = "2";
    aggInfo = new AggregateInfo(serverUrl, appId, userName, password); 
  }
  
//  public void testDownloadTaskWithEmptyTable_ExpectPass() {
//    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
//    String savePath = this.absolutePathOfTestFiles + "downloadedData/plot-output.csv";
//    String testTableId = "test1";
//    String tableSchemaETag = null;
//    WinkClient wc = null;
//
//    try {
//      wc = new WinkClient();
//      
//      String agg_url = aggInfo.getHostUrl();
//      agg_url = agg_url.substring(0, agg_url.length()-1);
//      
//      URL url = new URL(agg_url);
//      String host = url.getHost();
//      
//      wc.init(host, aggInfo.getUserName(), aggInfo.getPassword());
//
//      LoginTask lTask = new LoginTask(aggInfo, false);
//      lTask.blockingExecute();
//
//      JSONObject result = wc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
//          testTableId, null, csvFile);
//      System.out.println("testDownloadTaskWithEmptyTable_ExpectPass: result is " + result);
//
//      if (result.containsKey(WinkClient.TABLE_ID_JSON)) {
//        String tableId = result.getString(WinkClient.TABLE_ID_JSON);
//        assertEquals(tableId, testTableId);
//        tableSchemaETag = result.getString(WinkClient.SCHEMA_ETAG_JSON);
//      }
//
//      // Get the table definition
//      JSONObject tableDef = wc.getTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(),
//          testTableId, tableSchemaETag);
//
//      // Make sure it is the same as the csv definition
//      assertTrue(TestUtilities.checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));
//
//      AttachmentManager attMgr = new AttachmentManager(aggInfo, testTableId, savePath);
//      ODKCsv csv = new ODKCsv(attMgr, aggInfo, testTableId);
//      CsvConfig csvConfig = new CsvConfig(false, false,false);
//      
//      DownloadTask dTask = new DownloadTask(aggInfo, csv, csvConfig, savePath, false);
//      dTask.blockingExecute();
//      
//      // Now delete the table
//      wc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
//          tableSchemaETag);
//
//      // Check that table no longer exists
//      JSONObject obj = wc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
//      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));
//
//      wc.close();
//
//    } catch (Exception e) {
//      System.out.println("DownloadTaskTest: Exception thrown in testDownloadTaskWithEmptyTable_ExpectPass");
//      e.printStackTrace();
//      fail();
//    }
//  }
  
  public void testDownloadTaskAddWithNonEmptyTable_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String dataPath = absolutePathOfTestFiles + "plot/plot-add.csv";
    String savePath = this.absolutePathOfTestFiles + "downloadedData/plot-output.csv";
    String testTableId = "test2";
    String tableSchemaETag = null;
    WinkClient wc = null;

    try {
      wc = new WinkClient();
      
      String agg_url = aggInfo.getHostUrl();
      agg_url = agg_url.substring(0, agg_url.length()-1);
      
      URL url = new URL(agg_url);
      String host = url.getHost();
      
      wc.init(host, aggInfo.getUserName(), aggInfo.getPassword());

      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();

      JSONObject result = wc.createTableWithCSV(aggInfo.getServerUrl(), aggInfo.getAppId(),
          testTableId, null, csvFile);
      System.out.println("testDownloadTaskAddWithNonEmptyTable_ExpectPass: result is " + result);

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

      File f = new File(savePath);
      if(f.exists()) {
        f.delete();
      }
      
      AttachmentManager attMgr = new AttachmentManager(aggInfo, testTableId, savePath);
      ODKCsv csv = new ODKCsv(attMgr, aggInfo, testTableId);
      CsvConfig csvConfig = new CsvConfig(false, false,false);
      
      DownloadTask dTask = new DownloadTask(aggInfo, csv, csvConfig, savePath, false);
      dTask.blockingExecute();
      
      f = new File(savePath);
      assertTrue(f.exists());
      
      // Now delete the table
      wc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = wc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

      wc.close();

    } catch (Exception e) {
      System.out.println("DownloadTaskTest: Exception thrown in testDownloadTaskAddWithNonEmptyTable_ExpectPass");
      e.printStackTrace();
      fail();
    }
  }
}
