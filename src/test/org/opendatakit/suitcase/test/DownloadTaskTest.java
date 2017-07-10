package org.opendatakit.suitcase.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.aggregate.odktables.rest.RFC4180CsvReader;
import org.opendatakit.suitcase.model.AggregateInfo;
import org.opendatakit.suitcase.model.CsvConfig;
import org.opendatakit.suitcase.model.ODKCsv;
import org.opendatakit.suitcase.net.AttachmentManager;
import org.opendatakit.suitcase.net.DownloadTask;
import org.opendatakit.suitcase.net.LoginTask;
import org.opendatakit.suitcase.net.SuitcaseSwingWorker;
import org.opendatakit.suitcase.net.UpdateTask;
import org.opendatakit.sync.client.SyncClient;

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
  
  public void testDownloadTaskWithEmptyTable_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String savePath = this.absolutePathOfTestFiles + "downloadedData/plot-output1";
    String testTableId = "test1";
    String fileName = "link_unformatted.csv";
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
      System.out.println("testDownloadTaskWithEmptyTable_ExpectPass: result is " + result);

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
      
      // Check the entire file path
      String fullSavePath = savePath + File.separator + appId + File.separator + 
          testTableId + File.separator + fileName;
      
      // First delete any existing file
      File f = new File(fullSavePath);
      if(f.exists()) {
        f.delete();
      }

      AttachmentManager attMgr = new AttachmentManager(aggInfo, testTableId, savePath);
      ODKCsv csv = new ODKCsv(attMgr, aggInfo, testTableId);
      CsvConfig csvConfig = new CsvConfig(false, false,false);
      
      DownloadTask dTask = new DownloadTask(aggInfo, csv, csvConfig, savePath, false);
      retCode = dTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);
      
      f = new File(savePath);
      assertTrue(f.exists());
      
      File fullFilePath = new File(fullSavePath);
      assertTrue(fullFilePath.exists());
      
      // Check that file has two rows
      FileInputStream in = new FileInputStream(fullFilePath);
      InputStreamReader inReader = new InputStreamReader(in);
      RFC4180CsvReader csvReader = new RFC4180CsvReader(inReader);
      
      String[] lineIn;
      int numOfLines = 0;
      String rowId = null;
      while ((lineIn = csvReader.readNext()) != null) {
        rowId = lineIn[0];
        
        numOfLines++;
      }
      
      assertEquals(numOfLines, 1);
      assertEquals(rowId, "_id");
      
      // Now delete the table
      sc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = sc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

      sc.close();

    } catch (Exception e) {
      System.out.println("DownloadTaskTest: Exception thrown in testDownloadTaskWithEmptyTable_ExpectPass");
      e.printStackTrace();
      fail();
    }
  }
  
  public void testDownloadTaskAddWithNonEmptyTable_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String dataPath = absolutePathOfTestFiles + "plot/plot-add.csv";
    String savePath = this.absolutePathOfTestFiles + "downloadedData/plot-output2";
    String testTableId = "test2";
    String fileName = "link_unformatted.csv";
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
      System.out.println("testDownloadTaskAddWithNonEmptyTable_ExpectPass: result is " + result);

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
      
      // Check the entire file path
      String fullSavePath = savePath + File.separator + appId + File.separator + 
          testTableId + File.separator + fileName;

      // First delete any existing file
      File f = new File(fullSavePath);
      if(f.exists()) {
        f.delete();
      }
      
      AttachmentManager attMgr = new AttachmentManager(aggInfo, testTableId, savePath);
      ODKCsv csv = new ODKCsv(attMgr, aggInfo, testTableId);
      CsvConfig csvConfig = new CsvConfig(false, false,false);
      
      DownloadTask dTask = new DownloadTask(aggInfo, csv, csvConfig, savePath, false);
      retCode = dTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);
      
      f = new File(savePath);
      assertTrue(f.exists());
      
      File fullFilePath = new File(fullSavePath);
      assertTrue(fullFilePath.exists());
      
      // Check that file has two rows
      FileInputStream in = new FileInputStream(fullFilePath);
      InputStreamReader inReader = new InputStreamReader(in);
      RFC4180CsvReader csvReader = new RFC4180CsvReader(inReader);
      
      String[] lineIn;
      int numOfLines = 0;
      String rowId = null;
      while ((lineIn = csvReader.readNext()) != null) {
        rowId = lineIn[0];
        
        numOfLines++;
      }
      
      assertEquals(numOfLines, 2);
      assertEquals(rowId, "12");
      
      // Now delete the table
      sc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = sc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

      sc.close();

    } catch (Exception e) {
      System.out.println("DownloadTaskTest: Exception thrown in testDownloadTaskAddWithNonEmptyTable_ExpectPass");
      e.printStackTrace();
      fail();
    }
  }
  
  public void testDownloadTaskAddWithNonEmptyTableFromGeneratedCSV_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "geotagger/definition.csv";
    String dataPath = absolutePathOfTestFiles + "geotagger/geotagger-add.csv";
    String savePath = this.absolutePathOfTestFiles + "downloadedData/geotagger-output3";
    String testTableId = "test3";
    String fileName = "link_unformatted.csv";
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
      System.out.println("testDownloadTaskAddWithNonEmptyTableFromGeneratedCSV_ExpectPass: result is " + result);

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

      assertEquals(rows.size(), 14);

      JSONObject jsonRow = rows.getJSONObject(13);

      // Now check that the row was created with the right rowId
      assertTrue(TestUtilities.checkThatRowHasId("uuid:9551714f-66fa-4f85-ac02-1cbe3a66a61a", jsonRow));
      
      // Check the entire file path
      String fullSavePath = savePath + File.separator + appId + File.separator + 
          testTableId + File.separator + fileName;

      // First delete any existing file
      File f = new File(fullSavePath);
      if(f.exists()) {
        f.delete();
      }
      
      AttachmentManager attMgr = new AttachmentManager(aggInfo, testTableId, savePath);
      ODKCsv csv = new ODKCsv(attMgr, aggInfo, testTableId);
      CsvConfig csvConfig = new CsvConfig(false, false,false);
      
      DownloadTask dTask = new DownloadTask(aggInfo, csv, csvConfig, savePath, false);
      retCode = dTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);
      
      f = new File(savePath);
      assertTrue(f.exists());
      
      File fullFilePath = new File(fullSavePath);
      assertTrue(fullFilePath.exists());
      
      // Check that file has two rows
      FileInputStream in = new FileInputStream(fullFilePath);
      InputStreamReader inReader = new InputStreamReader(in);
      RFC4180CsvReader csvReader = new RFC4180CsvReader(inReader);
      
      String[] lineIn;
      int numOfLines = 0;
      String rowId = null;
      while ((lineIn = csvReader.readNext()) != null) {
        rowId = lineIn[0];
        
        numOfLines++;
      }
      
      assertEquals(numOfLines, 15);
      assertEquals(rowId, "uuid:9551714f-66fa-4f85-ac02-1cbe3a66a61a");
      
      // Now delete the table
      sc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = sc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

      sc.close();

    } catch (Exception e) {
      System.out.println("DownloadTaskTest: Exception thrown in testDownloadTaskAddWithNonEmptyTable_ExpectPass");
      e.printStackTrace();
      fail();
    }
  }
  
  public void testDownloadTaskAddWithNonEmptyTableFromGeneratedCSVAndGroups_ExpectPass() {
    String csvFile = absolutePathOfTestFiles + "geotagger/definition.csv";
    String dataPath = absolutePathOfTestFiles + "geotagger/geotagger-groups.csv";
    String savePath = this.absolutePathOfTestFiles + "downloadedData/geotagger-output4";
    String testTableId = "test4";
    String fileName = "link_unformatted.csv";
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
      System.out.println("testDownloadTaskAddWithNonEmptyTableFromGeneratedCSVAndGroups_ExpectPass: result is " + result);

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

      assertEquals(rows.size(), 3);

      assertEquals(TestUtilities.verifyServerRowsMatchCSV(rows, dataPath), true);
      
      // Check the entire file path
      String fullSavePath = savePath + File.separator + appId + File.separator + 
          testTableId + File.separator + fileName;

      // First delete any existing file
      File f = new File(fullSavePath);
      if(f.exists()) {
        f.delete();
      }
      
      AttachmentManager attMgr = new AttachmentManager(aggInfo, testTableId, savePath);
      ODKCsv csv = new ODKCsv(attMgr, aggInfo, testTableId);
      CsvConfig csvConfig = new CsvConfig(false, false,false);
      
      DownloadTask dTask = new DownloadTask(aggInfo, csv, csvConfig, savePath, false);
      retCode = dTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);
      
      f = new File(savePath);
      assertTrue(f.exists());
      
      File fullFilePath = new File(fullSavePath);
      assertTrue(fullFilePath.exists());
      
      res = sc.getRowsSince(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag, null, null, null);

      rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 3);
      
      assertEquals(TestUtilities.verifyServerRowsMatchCSV(rows, fullSavePath), true);
      
      // Now delete the table
      sc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), testTableId,
          tableSchemaETag);

      // Check that table no longer exists
      JSONObject obj = sc.getTables(aggInfo.getServerUrl(), aggInfo.getAppId());
      assertFalse(TestUtilities.checkTableExistOnServer(obj, testTableId, tableSchemaETag));

      sc.close();

    } catch (Exception e) {
      System.out.println("DownloadTaskTest: Exception thrown in testDownloadTaskAddWithNonEmptyTable_ExpectPass");
      e.printStackTrace();
      fail();
    }
  }
}
