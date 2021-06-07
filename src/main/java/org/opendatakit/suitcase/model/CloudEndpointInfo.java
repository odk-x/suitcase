package org.opendatakit.suitcase.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class CloudEndpointInfo {
  //These domains are only accessible through https
  private static final String[] HTTPS_DOMAIN = new String[] { "appspot.com" };
  private static final String SERVER_URL_POSTFIX = "odktables";

  private String serverUrl;
  private String appId;
  private String userName;
  private String password;
  private SortedMap<String, String> tableIdSchemaETag;
  private String privilegedUserName;
  
  public CloudEndpointInfo(String serverUrl, String appId, String userName, String password)
      throws MalformedURLException {
    if (serverUrl == null || serverUrl.isEmpty())
      throw new IllegalArgumentException("Invalid server URL");

    if (appId == null || appId.isEmpty())
      throw new IllegalArgumentException("Invalid app id");

    this.serverUrl = processUrl(serverUrl);
    this.appId = appId;
    this.userName = userName == null ? "" : userName;
    this.password = password == null ? "" : password;
    this.tableIdSchemaETag = new TreeMap<>();
  }

  public String getServerUrl() {
    return serverUrl + SERVER_URL_POSTFIX;
  }

  public String getHostUrl() {
    return serverUrl;
  }

  public String getAppId() {
    return appId;
  }

  public Set<String> getAllTableId() {
    return tableIdSchemaETag.keySet();
  }

  public SortedMap<String, String> getAllSchemaETag() {
    return Collections.unmodifiableSortedMap(tableIdSchemaETag);
  }
  
  public String getUserName() {
    return userName;
  }
  
  public void setPrivilegedUserName(String privUserName) {
    privilegedUserName = privUserName;
  }
  
  public String getPrivilegedUserName() {
    return privilegedUserName;
  }
  
  public String getPassword() {
    return password;
  }

  public String getSchemaETag(String tableId) {
    if (!tableIdExists(tableId)) {
      throw new IllegalArgumentException("Invalid Table ID");
    }

    return tableIdSchemaETag.get(tableId);
  }

  public void addTableId(String tableId, String schemaETag) {
    tableIdSchemaETag.put(tableId, schemaETag);
  }

  public boolean tableIdExists(String tableId) {
    return tableIdSchemaETag.containsKey(tableId);
  }

  public void resetTableIdSchemaETag() {
  	tableIdSchemaETag.clear();
  }
  /**
   * Attempts to fix some problems with serverUrl
   *
   * @param serverUrl
   * @return
   * @throws MalformedURLException
   */
  private String processUrl(String serverUrl) throws MalformedURLException {
    if (!serverUrl.endsWith("/")) {
      serverUrl += "/";
    }

    if (!serverUrl.startsWith("http")) {
      serverUrl = "http://" + serverUrl;
    }

    URL url = new URL(serverUrl);

    for (String domain : HTTPS_DOMAIN) {
      //check if url needs to be https, and make it https
      if (url.getHost().toLowerCase().endsWith(domain) && url.getDefaultPort() != 443) {
        return serverUrl.replace("http", "https");
      }
    }

    return url.toString();
  }

  @Override
  public String toString() {
    return "CloudEndpointInfo{" +
        "serverUrl='" + serverUrl + '\'' +
        ", appId='" + appId + '\'' +
        ", userName='" + userName + '\'' +
        ", privilegedUserName='" + privilegedUserName + '\'' +
        ", password='" + password + '\'' +
        ", tableIdSchemaETag=" + tableIdSchemaETag +
        '}';
  }
}
