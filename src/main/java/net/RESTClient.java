package net;

import model.AggregateTableInfo;
import model.ODKCsv;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.aggregate.odktables.rest.RFC4180CsvWriter;
import org.opendatakit.wink.client.WinkClient;
import utils.FileUtils;

import javax.swing.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

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

  private static final int FETCH_LIMIT = 1000;

  //!!!ATTENTION!!! One per table
  public RESTClient(AggregateTableInfo tableInfo) {
    this.tableInfo = tableInfo;

    tableInfo.setSchemaETag(WinkClient
        .getSchemaETagForTable(this.tableInfo.getServerUrl(), this.tableInfo.getAppId(),
            this.tableInfo.getTableId()));

    this.odkWinkClient = new WinkClient();
    AttachmentManager attMngr = new AttachmentManager(this.tableInfo, this.odkWinkClient);
    this.csv = new ODKCsv(attMngr, this.tableInfo);
  }

  /**
   * Retrieve formatted rows from ODKCsv and write to file
   *
   * @param scanFormatting  True to apply scan formatting
   * @param localLink       True to hyperlink to local files
   * @throws IOException
   * @throws JSONException
   */
  public void writeCSVToFile(final boolean scanFormatting, final boolean localLink)
      throws IOException, JSONException {
    if (this.csv.getSize() == 0) {
      //Download json if not downloaded
      retrieveRows();
    }

    this.pb.setIndeterminate(false);
    this.pb.setString("Processing and writing data");

    RFC4180CsvWriter csvWriter =
        new RFC4180CsvWriter(
            new FileWriter(
                FileUtils.getCSVPath(
                    this.tableInfo, scanFormatting, localLink
                ).toAbsolutePath().toString()
            )
        );

    ODKCsv.ODKCSVIterator csvIt = this.csv.getODKCSVIterator();

    //Write header and rows
    csvWriter.writeNext(this.csv.getHeader(scanFormatting));
    while (csvIt.hasNext()) {
      csvWriter.writeNext(csvIt.next(scanFormatting, localLink));

      //Set value of progress bar with number of rows done
      this.pb.setValue(
          (int) ((double) csvIt.getIndex() / this.csv.getSize() * this.pb.getMaximum())
      );
    }

    csvWriter.close();
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
   * Set a JProgressBar
   *
   * @param pb JProgressBar
   */
  public void setProgressBar(JProgressBar pb) {
    this.pb = pb;
  }
}
