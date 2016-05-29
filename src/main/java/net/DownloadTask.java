package net;

import model.AggregateInfo;
import model.CsvConfig;
import model.ODKCsv;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.aggregate.odktables.rest.RFC4180CsvWriter;
import org.opendatakit.wink.client.WinkClient;
import ui.DialogUtils;
import ui.ProgressBarStatus;
import ui.SuitcaseProgressBar;
import utils.FileUtils;

import javax.swing.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static ui.MessageString.*;

public class DownloadTask extends SuitcaseSwingWorker<Void> {
  private static final String RETRIEVING_ROW = "Retrieving rows";
  private static final String PROCESSING_ROW = "Processing and writing data";

  private AggregateInfo aggInfo;
  private ODKCsv csv;
  private CsvConfig csvConfig;
  private String savePath;
  private boolean isGUI;

  public DownloadTask(AggregateInfo aggInfo, ODKCsv csv, CsvConfig csvConfig, String savePath,
      boolean isGUI) {
    this.aggInfo = aggInfo;
    this.csv = csv;
    this.csvConfig = csvConfig;
    this.savePath = savePath;
    this.isGUI = isGUI;
  }

  @Override
  protected Void doInBackground() throws Exception {
    //assume csv has already been initialized by caller of this worker

    // check existing data
    if (FileUtils.isDownloaded(aggInfo, csv.getTableId(), csvConfig, savePath) &&
        DialogUtils.promptConfirm(OVERWRITE_CSV, true)) {
      FileUtils.deleteCsv(aggInfo, csvConfig, csv.getTableId(), savePath);
    }

    // then create directory structure when needed
    FileUtils.createDirectory(aggInfo, csvConfig, csv.getTableId(), savePath);

    WinkSingleton wink = WinkSingleton.getInstance();

    // retrieve data from Aggregate and store in csv
    if (csv.getSize() == 0) {
      publish(new ProgressBarStatus(0, RETRIEVING_ROW, true));

      JSONObject rows;
      String cursor = null;

      do {
        rows = wink.getRows(csv.getTableId(), cursor);
        cursor = rows.optString(WinkClient.jsonWebSafeResumeCursor);
        csv.tryAdd(rows.getJSONArray(WinkClient.jsonRowsString));
      } while (rows.getBoolean(WinkClient.jsonHasMoreResults));
    }

    // write out csv to file
    publish(new ProgressBarStatus(0, PROCESSING_ROW, false));
    RFC4180CsvWriter csvWriter = null;
    try {
      csvWriter = new RFC4180CsvWriter(new FileWriter(
          FileUtils.getCSVPath(aggInfo, csv.getTableId(), csvConfig, savePath).toString()
      ));

      //Write header then rows
      csvWriter.writeNext(csv.getHeader(csvConfig));

      ODKCsv.ODKCSVIterator csvIt = csv.getODKCSVIterator();
      while (csvIt.hasNext()) {
        csvWriter.writeNext(csvIt.next(csvConfig));

        //Set value of progress bar with percentage of rows done
        int progress = (int) ((double) csvIt.getIndex() / csv.getSize() * 100);
        publish(new ProgressBarStatus(progress, null, null));
      }
    } finally {
      if (csvWriter != null) {
        csvWriter.close();
      }
    }

    return null;
  }

  @Override
  protected void finished() {
    try {
      get();

      setString(SuitcaseProgressBar.PB_DONE);
    } catch (InterruptedException e) {
      e.printStackTrace();
      DialogUtils.showError(GENERIC_ERR, isGUI);
      setString(SuitcaseProgressBar.PB_ERROR);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();

      String errMsg;
      if (cause instanceof IOException) {
        errMsg = IO_WRITE_ERR;
      } else if (cause instanceof JSONException) {
        errMsg = VISIT_WEB_ERROR;
      } else {
        errMsg = GENERIC_ERR;
      }

      cause.printStackTrace();
      DialogUtils.showError(errMsg, isGUI);
      setString(SuitcaseProgressBar.PB_ERROR);
    } finally {
      setIndeterminate(false);
    }
  }
}
