package org.opendatakit.suitcase.net;

import org.opendatakit.suitcase.model.AggregateInfo;
import org.apache.wink.json4j.JSONException;
import org.opendatakit.suitcase.ui.DialogUtils;
import org.opendatakit.suitcase.ui.SuitcaseProgressBar;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.zip.DataFormatException;

import static org.opendatakit.suitcase.ui.MessageString.*;

public class UploadTask extends SuitcaseSwingWorker<Void> {
  private static final String IN_PROGRESS_STRING = "Uploading...";
  private static final int PUSH_FINISH_WAIT = 5000;

  private AggregateInfo aggInfo;
  private String dataPath;
  private String version;
  private boolean isGUI;

  public UploadTask(AggregateInfo aggInfo, String dataPath, String version, boolean isGUI) {
    super();

    this.aggInfo = aggInfo;
    this.dataPath = dataPath;
    this.version = version;
    this.isGUI = isGUI;
  }

  @Override
  protected Void doInBackground() throws JSONException, IOException, 
    DataFormatException, InterruptedException {
    setString(IN_PROGRESS_STRING);

    SyncWrapper syncWrapper = SyncWrapper.getInstance();
    syncWrapper.pushAllData(dataPath, version);

    Thread.sleep(PUSH_FINISH_WAIT);
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
        errMsg = IO_READ_ERR;
      } else if (cause instanceof JSONException) {
        errMsg = VISIT_WEB_ERROR;
      } else if (cause instanceof DataFormatException) {
        errMsg = INVALID_CSV;
      } else {
        errMsg = GENERIC_ERR;
      }

      DialogUtils.showError(errMsg, isGUI);
      setString(SuitcaseProgressBar.PB_ERROR);
      cause.printStackTrace();
      returnCode = SuitcaseSwingWorker.errorCode;  
    } finally {
      setIndeterminate(false);
    }
  }
}
