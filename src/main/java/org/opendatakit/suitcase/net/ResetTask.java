package org.opendatakit.suitcase.net;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.opendatakit.suitcase.model.SyncClientException;
import org.opendatakit.suitcase.ui.DialogUtils;
import org.opendatakit.suitcase.ui.ProgressBarStatus;
import org.opendatakit.suitcase.ui.SuitcaseProgressBar;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.opendatakit.suitcase.ui.MessageString.*;

public class ResetTask extends SuitcaseSwingWorker<Void> {
  private static final String IN_PROGRESS_STRING = "Deleting...";
  private static final int RESET_FINISH_WAIT = 5000;

  private String version;
  private boolean isGUI;

  public ResetTask(String version, boolean isGUI) {
    super();

    this.version = version;
    this.isGUI = isGUI;
  }

  @Override
  protected Void doInBackground() throws JSONException, IOException, InterruptedException, SyncClientException {
    setString(IN_PROGRESS_STRING);

    SyncWrapper syncWrapper = SyncWrapper.getInstance();

    // first delete all app level files
    publish(new ProgressBarStatus(0, "Stage 1/3: Delete app level files", false));
    JSONArray appFiles = syncWrapper.getManifestForAppLevelFiles(version).getJSONArray("files");
    for (int i = 0; i < appFiles.size(); i++) {
      String filename = appFiles.getJSONObject(i).getString("filename");
      int ret = syncWrapper.deleteFile(filename, version);
      if(ret!=200) {
        throw new IOException("Unknown Error occurred");    // If operation fails the stop the reset process.
      }
      int progress = (int) ((double) i / appFiles.size() * 100);
      publish(new ProgressBarStatus(progress, "Stage 1/3: Deleted " + filename, null));
    }

    // then delete all table definitions
    publish(new ProgressBarStatus(0, "Stage 2/3: Delete table definitions", false));
    Set<String> tables = syncWrapper.updateTableList();
    int tableCounter = 0;
    for (String table : tables) {
      // for large data sets deletion might timeout
      // so tables must be repeatedly deleted

      int retryCounter = 1;
      int status;
      while ((status = syncWrapper.deleteTableDefinition(table)) == 500) {
        int progress = (int) ((double) tableCounter / tables.size() * 100);
        String msg = "Stage 2/3: Deleting " + table + " retry #" + retryCounter++;
        publish(new ProgressBarStatus(progress, msg, null));
      }
      if (status < 200 || status > 299) {
        throw new IllegalStateException("Unexpected status code: " + status);
      }
      tableCounter++;
    }

    publish(new ProgressBarStatus(0, "Stage 3/3: Delete tables that are in bad states", true));
    // the table id and schemaETag can be anything
    while ((syncWrapper.deleteTableDefinition("table", "etag")) == 500);

    Thread.sleep(RESET_FINISH_WAIT);
    syncWrapper.updateTableList();

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
      returnCode = SuitcaseSwingWorker.errorCode;
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();

      String errMsg;
      if (cause instanceof IOException) {
        errMsg = HTTP_IO_ERROR;
      } else if (cause instanceof JSONException || cause instanceof IllegalStateException) {
        errMsg = VISIT_WEB_ERROR;
      } else {
        errMsg = GENERIC_ERR;
      }

      cause.printStackTrace();
      DialogUtils.showError(errMsg, isGUI);
      setString(SuitcaseProgressBar.PB_ERROR);
      returnCode = SuitcaseSwingWorker.errorCode;
    } finally {
      setIndeterminate(false);
    }
  }
}
