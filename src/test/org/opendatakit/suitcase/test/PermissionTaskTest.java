package org.opendatakit.suitcase.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import junit.framework.TestCase;

import org.opendatakit.suitcase.model.AggregateInfo;
import org.opendatakit.suitcase.net.LoginTask;
import org.opendatakit.suitcase.net.PermissionTask;
import org.opendatakit.wink.client.WinkClient;

public class PermissionTaskTest extends TestCase {
  
  AggregateInfo aggInfo = null;
  String serverUrl = null;
  String appId = null;
  String userName = null;
  String password = null;
  String version = null;
  
  @Override
  protected void setUp() throws MalformedURLException {
    serverUrl = "";
    appId = "default";
    userName = "";
    password = "";
    version = "2";
    aggInfo = new AggregateInfo(serverUrl, appId, userName, password); 
  }
  
  public void testCreateUserPermission_ExpectPass() {
    String dataPath = "testfiles/permissions/perm-file.csv";
    boolean foundUser = false;
    String testUserName = "mailto:testerodk@gmail.com";
    String userIdStr = "user_id";
    
    try {
      WinkClient wc = new WinkClient();
      
      String agg_url = aggInfo.getHostUrl();
      agg_url = agg_url.substring(0, agg_url.length()-1);
      
      URL url = new URL(agg_url);
      String host = url.getHost();
      
      wc.init(host, aggInfo.getUserName(), aggInfo.getPassword());
      
      LoginTask lTask = new LoginTask(aggInfo, false);
      lTask.blockingExecute();
      
      PermissionTask pTask = new PermissionTask(aggInfo, dataPath, version, false);
      pTask.blockingExecute();
      
      // Check that user exists
      ArrayList<Map<String, Object>> result = wc.getUsers(agg_url);

      if (result != null) {

        for (int i = 0; i < result.size(); i++) {
          Map<String, Object> userMap = result.get(i);
          if (userMap.containsKey(userIdStr) && testUserName.equals(userMap.get(userIdStr))) {
            foundUser = true;
            break;
          }
        }

        assertTrue(foundUser);
      }

      wc.close();
    } catch (Exception e) {
      System.out.println("PermissionTaskTest: Exception thrown in testCreateUserPermission_ExpectPass");
      e.printStackTrace();
      fail(); 
    }
  }
  
}
