package org.opendatakit.suitcase.test;

import junit.framework.TestCase;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.suitcase.model.CloudEndpointInfo;
import org.opendatakit.suitcase.net.DeleteTask;
import org.opendatakit.suitcase.net.LoginTask;
import org.opendatakit.suitcase.net.SuitcaseSwingWorker;
import org.opendatakit.suitcase.net.UploadTask;
import org.opendatakit.sync.client.SyncClient;

import java.net.URL;

public class DeleteTaskTest extends TestCase {

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

        URL url = new URL(serverUrl);
        host = url.getHost();
        version = "2";
        cloudEndpointInfo = new CloudEndpointInfo(serverUrl, appId, userName, password);
    }

    public void testUploadTaskAndDelete_ExpectPass() {
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
            UploadTask uploadTask = new UploadTask(cloudEndpointInfo, dataPathToAppFile, version, false, null, null);
            retCode = uploadTask.blockingExecute();
            assertEquals(retCode, SuitcaseSwingWorker.okCode);


            JSONObject table = sc.getTable(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), tableId);
            assertEquals(tableId, table.getString("tableId"));

            // Now delete the table
            DeleteTask deleteTask = new DeleteTask(tableId,version);
            retCode = deleteTask.blockingExecute();
            assertEquals(retCode, SuitcaseSwingWorker.okCode);
            // Check that table no longer exists
            JSONObject tables = sc.getTable(cloudEndpointInfo.getServerUrl(), cloudEndpointInfo.getAppId(), tableId);
            assertNull(tables);

        } catch (Exception e) {
            System.out.println("DeleteTaskTest: Exception thrown in testUploadTaskAndDelete_ExpectPass");
            e.printStackTrace();
            fail();
        } finally {
            if (sc != null) {
                sc.close();
            }
        }
    }
}
