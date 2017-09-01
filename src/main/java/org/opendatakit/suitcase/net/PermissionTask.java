package org.opendatakit.suitcase.net;

import static org.opendatakit.suitcase.ui.MessageString.GENERIC_ERR;
import static org.opendatakit.suitcase.ui.MessageString.INVALID_CSV;
import static org.opendatakit.suitcase.ui.MessageString.IO_READ_ERR;
import static org.opendatakit.suitcase.ui.MessageString.VISIT_WEB_ERROR;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.zip.DataFormatException;

import org.apache.wink.json4j.JSONException;
import org.opendatakit.suitcase.model.CloudEndpointInfo;
import org.opendatakit.suitcase.ui.DialogUtils;
import org.opendatakit.suitcase.ui.SuitcaseProgressBar;

public class PermissionTask extends SuitcaseSwingWorker<Void> {
  
  private static final String IN_PROGRESS_STRING = "Updating Permissions...";
  private static final int PUSH_FINISH_WAIT = 5000;

  public final static String CREATE_OP = "CREATE";
  public final static String DELETE_OP = "DELETE";
  public final static String CLEAR_OP = "CLEAR";

  SyncWrapper wrapper = SyncWrapper.getInstance();
  String intermediateTemp = "temp";
  String csvExt = ".csv";

  private CloudEndpointInfo cloudEndpointInfo = null;
  private String dataPath = null;
  private String version = null;
  private boolean isGUI;

  public PermissionTask(CloudEndpointInfo cloudEndpointInfo, String dataPath, String version, boolean isGUI) {
    super();
    this.cloudEndpointInfo = cloudEndpointInfo;
    this.dataPath = dataPath;
    this.version = version;
    this.isGUI = isGUI;
  }

  @Override
  protected Void doInBackground() throws IOException, InterruptedException, JSONException {
    setString(IN_PROGRESS_STRING);

    SyncWrapper syncWrapper = SyncWrapper.getInstance();

    String className = this.getClass().getSimpleName();
    if (cloudEndpointInfo == null) {
      System.out.println("cloudEndpointInfo must be specified " + className);
    }

    if (dataPath == null || dataPath.length() == 0) {
      System.out.println("dataPath must be specified for " + CREATE_OP + "operation with "
          + className);
      return null;
    }

    wrapper.uploadPermissionCSV(dataPath);

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
