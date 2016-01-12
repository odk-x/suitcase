package model;

import javax.swing.border.EtchedBorder;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class AggregateTableInfo {
    private static final String[] HTTPS_DOMAIN = new String[] {
        "appspot.com"
    };
    private static final String SERVER_URL_POSTFIX = "odktables";
    private static final String NULL = "null";
    
    private String serverUrl;
    private String appId;
    private String tableId;
    private String schemaETag;

    public AggregateTableInfo(String serverUrl, String appId, String tableId) throws MalformedURLException {
        if (serverUrl == null || serverUrl.isEmpty()) 
            throw new IllegalArgumentException("Invalid server URL");

        if (appId == null || appId.isEmpty())
            throw new IllegalArgumentException("Invalid app id");

        if (tableId == null || tableId.isEmpty())
            throw new IllegalArgumentException("Invalid table id");
        
        this.serverUrl = processUrl(serverUrl);
        this.appId = appId;
        this.tableId = tableId;
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

    public void setSchemaETag(String ETag) {
        if (this.schemaETag != null) {
            throw new IllegalStateException("ETag has already been set!");
        }
        this.schemaETag = ETag;
    }

    public String getSchemaETag() {
        return this.schemaETag == null ? "" : this.schemaETag;
    }

    private String processUrl(String serverUrl) throws MalformedURLException {
        if (!serverUrl.endsWith("/")) {
            serverUrl += "/";
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregateTableInfo that = (AggregateTableInfo) o;
        return Objects.equals(getServerUrl(), that.getServerUrl()) &&
                Objects.equals(getAppId(), that.getAppId()) &&
                Objects.equals(getTableId(), that.getTableId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServerUrl(), getAppId(), getTableId());
    }
}
