package org.opendatakit.suitcase.test;

import java.io.File;
import java.net.URL;
import java.util.Scanner;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.sync.client.SyncClient;
import org.opendatakit.suitcase.net.LoginTask;
import org.opendatakit.suitcase.net.SuitcaseSwingWorker;
import org.opendatakit.suitcase.net.UpdateTask;
import org.opendatakit.suitcase.net.UploadTask;
import org.opendatakit.suitcase.model.AggregateInfo;

import junit.framework.TestCase;

public class UploadTaskTest extends TestCase{
  
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
  
  private boolean checkThatFileExists(JSONObject manifest, String relativeServerPath) {
    try {
      JSONArray files = manifest.getJSONArray("files");
      
      for (int i = 0; i < files.length(); i++) {
        JSONObject file = files.getJSONObject(i);
        String fileName = file.getString("filename");
        if (fileName.equals(relativeServerPath)) {
          return true;
        }
      }
      
    } catch (JSONException je) {
      System.out.println(je);
      return false;
    }
    
    return false;
  }
  
  public void testUploadTaskAddOneAppFile_ExpectPass() {
    String dataPathToAppFile = absolutePathOfTestFiles + "dataToUpload/assets/img/spaceNeedle_CCLicense_goCardUSA.jpg";
    String relativeServerPath = "assets/img/spaceNeedle_CCLicense_goCardUSA.jpg";
    
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

      // Push the file up to the server
      UploadTask uploadTask = new UploadTask(aggInfo, dataPathToAppFile, version, false, "FILE", relativeServerPath);
      retCode = uploadTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      // Now check that the file was created
      JSONObject manifest = sc.getManifestForAppLevelFiles(aggInfo.getServerUrl(), aggInfo.getAppId(), version);
      assertTrue(checkThatFileExists(manifest, relativeServerPath));
      
      // Now delete the file
      sc.deleteFile(aggInfo.getServerUrl(), aggInfo.getAppId(), relativeServerPath, version);

      // Check that file no longer exists
      manifest = sc.getManifestForAppLevelFiles(aggInfo.getServerUrl(), aggInfo.getAppId(), version);
      assertFalse(checkThatFileExists(manifest, relativeServerPath));

      sc.close();

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskAdd_ExpectPass");
      e.printStackTrace();
      fail();
    }
  }
  
  public void testUploadTaskResetApp_ExpectPass() {
    String dataPathToAppFile = absolutePathOfTestFiles + "dataToUpload";
    String tableId = "geotagger";
    
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

      // Push the file up to the server
      UploadTask uploadTask = new UploadTask(aggInfo, dataPathToAppFile, version, false, "RESET_APP", null);
      retCode = uploadTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      // Now check that the file was created
      JSONObject manifest = sc.getManifestForAppLevelFiles(aggInfo.getServerUrl(), aggInfo.getAppId(), version);
      
      // Check that the number of app files is correct
      JSONArray files = manifest.getJSONArray("files");
      assertEquals(files.length(), 10);
      
      JSONObject table = sc.getTable(aggInfo.getServerUrl(), aggInfo.getAppId(), tableId);
      assertEquals(tableId, table.getString("tableId"));
      
      // Now delete the app level file
      for (int i = 0; i < files.length(); i++) {
        JSONObject file = files.getJSONObject(i);
        String filePath = file.getString("filename");
        sc.deleteFile(aggInfo.getServerUrl(), aggInfo.getAppId(), filePath, version);
      }
      
      // Check that file no longer exists
      manifest = sc.getManifestForAppLevelFiles(aggInfo.getServerUrl(), aggInfo.getAppId(), version);
      assertEquals(files.length(), 10);
      
      // Check the table level manifest
      JSONObject tableManifest = sc.getManifestForTableId(aggInfo.getServerUrl(), aggInfo.getAppId(), tableId, version);
      JSONArray tableFiles = tableManifest.getJSONArray("files");
      assertEquals(tableFiles.length(), 14);
      
      sc.deleteTableDefinition(aggInfo.getServerUrl(), aggInfo.getAppId(), tableId, table.getString("schemaETag"));
      
      JSONObject tables = sc.getTable(aggInfo.getServerUrl(), aggInfo.getAppId(), tableId);
      assertNull(tables);

      sc.close();

    } catch (Exception e) {
      System.out.println("UpdateTaskTest: Exception thrown in testUpdateTaskAdd_ExpectPass");
      e.printStackTrace();
      fail();
    }
  }
}
