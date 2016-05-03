package net;

import model.AggregateTableInfo;
import model.ODKCsv;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.aggregate.odktables.rest.RFC4180CsvWriter;
import org.opendatakit.wink.client.WinkClient;
import utils.FileUtils;

import javax.swing.*;
import java.io.FileWriter;
import java.io.IOException;

import static org.opendatakit.wink.client.WinkClient.*;

/**
 * Handles most communication to OdkWinkClient
 *
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/19/15
 * Time: 11:27 AM
 */
public class RESTClient {
  private JProgressBar pb;

  private final WinkClient odkWinkClient;

  private final AggregateTableInfo tableInfo;
  private final ODKCsv csv;
  private String dataPath;

  private static final int FETCH_LIMIT = 1000;

  //!!!ATTENTION!!! One per table
  public RESTClient(AggregateTableInfo tableInfo, String dataPath) {
    this.tableInfo = tableInfo;
    /*tableInfo.setSchemaETag(WinkClient
        .getSchemaETagForTable(this.tableInfo.getServerUrl(), this.tableInfo.getAppId(),
            this.tableInfo.getTableId()));*/

    this.odkWinkClient = new WinkClient();
    this.pb = null;
    this.dataPath =
        dataPath == null ? FileUtils.getDefaultSavePath().toAbsolutePath().toString() : dataPath;
    
    // Debugging stuff
//    System.out.println("REST Client: username " + this.tableInfo.getUserName() + " password " + this.tableInfo.getPassword());
//    System.out.println("server url " + this.tableInfo.getServerUrl());
//    System.out.println("app id " + this.tableInfo.getAppId());
//    System.out.println("table id " + this.tableInfo.getTableId());
//    System.out.println("Host = " + this.tableInfo.getHostUrl());
    
    this.odkWinkClient.init(
        this.tableInfo.getHostUrl(), this.tableInfo.getUserName(), this.tableInfo.getPassword()
    );
    tableInfo.setSchemaETag(
        this.odkWinkClient.getSchemaETagForTable(
            this.tableInfo.getServerUrl(), this.tableInfo.getAppId(), this.tableInfo.getTableId()
        )
    );
    
    AttachmentManager attMngr = new AttachmentManager(
        this.tableInfo, this.odkWinkClient, this.dataPath
    );
    this.csv = new ODKCsv(attMngr, this.tableInfo);
  }

  /**
   * Retrieve formatted rows from ODKCsv and write to file
   *
   * @param scanFormatting  True to apply scan formatting
   * @param localLink       True to hyperlink to local files
   * @param extraMeta       True to include extra metadata
   * @throws IOException
   * @throws JSONException
   */
  public void writeCSVToFile(boolean scanFormatting, boolean localLink, boolean extraMeta)
      throws IOException, JSONException {
    if (this.csv.getSize() == 0) {
      //Download json if not downloaded
      retrieveRows();
    }

    pbSetValue(null, "Processing and writing data", false);

    RFC4180CsvWriter csvWriter =
        new RFC4180CsvWriter(
            new FileWriter(
                FileUtils.getCSVPath(
                    this.tableInfo, scanFormatting, localLink, extraMeta, dataPath
                ).toAbsolutePath().toString()
            )
        );

    ODKCsv.ODKCSVIterator csvIt = this.csv.getODKCSVIterator();

    //Write header and rows
    csvWriter.writeNext(this.csv.getHeader(scanFormatting, extraMeta));
    while (csvIt.hasNext()) {
      csvWriter.writeNext(csvIt.next(scanFormatting, localLink, extraMeta));

      //Set value of progress bar with percentage of rows done
      pbSetValue(
          (int) ((double) csvIt.getIndex() / this.csv.getSize() * this.pb.getMaximum()),
          null, null
      );
    }

    csvWriter.close();
  }

  public void pushAllData() {
    try {
      odkWinkClient.pushAllDataToUri(tableInfo.getServerUrl(), tableInfo.getAppId(), dataPath);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void deleteAllRemote() {
    try {
      // Delete all files on the server
      JSONObject appFiles =
          odkWinkClient.getManifestForAppLevelFiles(tableInfo.getServerUrl(), tableInfo.getAppId());
      JSONArray files = appFiles.getJSONArray("files");

      for (int j = 0; j < files.size(); j++) {
        odkWinkClient.deleteFile(
            tableInfo.getServerUrl(), tableInfo.getAppId(),
            files.getJSONObject(j).getString("filename")
        );
      }

      // Delete all tables
      JSONObject tablesObj =
          odkWinkClient.getTables(tableInfo.getServerUrl(), tableInfo.getAppId());
      JSONArray tables = tablesObj.getJSONArray("tables");

      for (int i = 0; i < tables.size(); i++) {
        odkWinkClient.deleteTableDefinition(
            tableInfo.getServerUrl(), tableInfo.getAppId(),
            tables.getJSONObject(i).getString("tableId"),
            tables.getJSONObject(i).getString("schemaETag")
        );
        Thread.sleep(10000);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Set a JProgressBar
   *
   * @param pb JProgressBar
   */
  public void setProgressBar(JProgressBar pb) {
    this.pb = pb;
  }

  /**
   * Download JSON of rows using WinkClient
   *
   * @throws JSONException
   */
  private void retrieveRows() throws JSONException {
    this.pb.setString("Retrieving rows");

    String cursor = null;
    JSONObject rows;

    do {
      rows = this.odkWinkClient.getRows(this.tableInfo.getServerUrl(), this.tableInfo.getAppId(),
          this.tableInfo.getTableId(), this.tableInfo.getSchemaETag(), cursor,
          Integer.toString(RESTClient.FETCH_LIMIT));

      cursor = rows.optString("webSafeResumeCursor");
      this.csv.tryAdd(rows.getJSONArray(jsonRowsString));
    } while (rows.getBoolean("hasMoreResults"));
  }

  /**
   * Wrapper for some JProgressBar setters to better handle GUI/CLI hybrid
   *
   * @param value
   * @param status
   * @param isIndeterminate
   */
  private void pbSetValue(Integer value, String status, Boolean isIndeterminate) {
    if (this.pb == null)
      return;

    if (value != null) {
      this.pb.setValue(value);
    }
    if (status != null) {
      this.pb.setString(status);
    }
    if (isIndeterminate != null) {
      this.pb.setIndeterminate(isIndeterminate);
    }
  }
}
