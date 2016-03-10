package model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class AggregateTableInfo {
  //These domains are only accessible through https
  private static final String[] HTTPS_DOMAIN = new String[] { "appspot.com" };
  private static final String SERVER_URL_POSTFIX = "odktables";

  private String serverUrl;
  private String appId;
  private String tableId;
  private String schemaETag;
  private String userName;
  private String password;
  
  public AggregateTableInfo(String serverUrl, String appId, String tableId, String userName, String password)
      throws MalformedURLException {
    if (serverUrl == null || serverUrl.isEmpty())
      throw new IllegalArgumentException("Invalid server URL");

    if (appId == null || appId.isEmpty())
      throw new IllegalArgumentException("Invalid app id");

    if (tableId == null || tableId.isEmpty())
      throw new IllegalArgumentException("Invalid table id");

    this.serverUrl = processUrl(serverUrl);
    this.appId = appId;
    this.tableId = tableId;
    this.userName = userName;
    this.password = password;
    this.schemaETag = null;
  }

  public String getServerUrl() {
    return serverUrl + SERVER_URL_POSTFIX;
  }

  public String getAppId() {
    return appId;
  }

  public String getTableId() {
    return tableId;
  }
  
  public String getUserName() {
    return userName;
  }
  
  public String getPassword() {
    return password;
  }

  public void setSchemaETag(String ETag) {
    if (this.schemaETag != null) {
      throw new IllegalStateException("ETag has already been set!");
    }
    this.schemaETag = ETag;
  }

  public String getSchemaETag() {
    return this.schemaETag == null ? "" : this.schemaETag;
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
      serverUrl += "http://";
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
    return "AggregateTableInfo{" +
        "serverUrl='" + serverUrl + '\'' +
        ", appId='" + appId + '\'' +
        ", tableId='" + tableId + '\'' +
        ", schemaETag='" + getSchemaETag() + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    AggregateTableInfo that = (AggregateTableInfo) o;
    return Objects.equals(getServerUrl(), that.getServerUrl()) &&
        Objects.equals(getAppId(), that.getAppId()) &&
        Objects.equals(getTableId(), that.getTableId()) &&
        Objects.equals(getUserName(), that.getUserName()) &&
        Objects.equals(getPassword(), that.getPassword());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getServerUrl(), getAppId(), getTableId());
  }
}
