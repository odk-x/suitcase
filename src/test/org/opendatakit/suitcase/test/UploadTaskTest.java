package org.opendatakit.suitcase.test;

import java.io.File;
import java.net.URL;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.sync.client.SyncClient;
import org.opendatakit.suitcase.net.LoginTask;
import org.opendatakit.suitcase.net.SuitcaseSwingWorker;
import org.opendatakit.suitcase.net.UploadTask;
import org.opendatakit.suitcase.model.CloudEndpointInfo;

import junit.framework.TestCase;

public class UploadTaskTest extends TestCase{
  
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
    String dataPathToAppFile = absolutePathOfTestFiles + "dataToUpload" + File.separator + 
    		"assets" + File.separator + "img" + File.separator + "spaceNeedle_CCLicense_goCardUSA.jpg";
    String relativeServerPath = "assets/img/spaceNeedle_CCLicense_goCardUSA.jpg";
    
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

      // Push the file up to the server
      UploadTask uploadTask = new UploadTask(cloudEndpointInfo, dataPathToAppFile, version, false, "FILE", relativeServerPath);
      retCode = uploadTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      // Now check that the file was created
      JSONObject manifest = sc.getManifestForAppLevelFiles(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), version);
      assertTrue(checkThatFileExists(manifest, relativeServerPath));
      
      // Now delete the file
      sc.deleteFile(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), relativeServerPath, version);

      // Check that file no longer exists
      manifest = sc.getManifestForAppLevelFiles(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), version);
      assertFalse(checkThatFileExists(manifest, relativeServerPath));

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
  
  public void testUploadTaskResetApp_ExpectPass() {
    String dataPathToAppFile = absolutePathOfTestFiles + "dataToUpload";
    String tableId = "geotagger";
    
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

      // Push the file up to the server
      UploadTask uploadTask = new UploadTask(cloudEndpointInfo, dataPathToAppFile, version, false, "RESET_APP", null);
      retCode = uploadTask.blockingExecute();
      assertEquals(retCode, SuitcaseSwingWorker.okCode);

      // Now check that the file was created
      JSONObject manifest = sc.getManifestForAppLevelFiles(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), version);
      
      // Check that the number of app files is correct
      JSONArray files = manifest.getJSONArray("files");
      assertEquals(files.length(), 10);
      
      JSONObject table = sc.getTable(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), tableId);
      assertEquals(tableId, table.getString("tableId"));
      
      // Now delete the app level file
      for (int i = 0; i < files.length(); i++) {
        JSONObject file = files.getJSONObject(i);
        String filePath = file.getString("filename");
        sc.deleteFile(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), filePath, version);
      }
      
      // Check that file no longer exists
      manifest = sc.getManifestForAppLevelFiles(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), version);
      assertEquals(files.length(), 10);
      
      // Check the table level manifest
      JSONObject tableManifest = sc.getManifestForTableId(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), tableId, version);
      JSONArray tableFiles = tableManifest.getJSONArray("files");
      assertEquals(tableFiles.length(), 14);
      
      sc.deleteTableDefinition(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), tableId, table.getString("schemaETag"));
      
      JSONObject tables = sc.getTable(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), tableId);
      assertNull(tables);

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
}
