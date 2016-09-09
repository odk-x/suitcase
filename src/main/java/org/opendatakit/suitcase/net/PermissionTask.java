package org.opendatakit.suitcase.net;

import static org.opendatakit.suitcase.ui.MessageString.GENERIC_ERR;
import static org.opendatakit.suitcase.ui.MessageString.INVALID_CSV;
import static org.opendatakit.suitcase.ui.MessageString.IO_READ_ERR;
import static org.opendatakit.suitcase.ui.MessageString.VISIT_WEB_ERROR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.zip.DataFormatException;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.aggregate.odktables.rest.entity.Row;
import org.opendatakit.aggregate.odktables.rest.entity.RowResource;
import org.opendatakit.aggregate.odktables.rest.entity.RowResourceList;
import org.opendatakit.suitcase.model.AggregateInfo;
import org.opendatakit.suitcase.ui.DialogUtils;
import org.opendatakit.suitcase.ui.SuitcaseProgressBar;
import org.opendatakit.wink.client.WinkClient;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PermissionTask extends SuitcaseSwingWorker<Void> {
  
  private static final String IN_PROGRESS_STRING = "Updating Permissions...";
  private static final int PUSH_FINISH_WAIT = 5000;

  public final static String CREATE_OP = "CREATE";
  public final static String DELETE_OP = "DELETE";
  public final static String CLEAR_OP = "CLEAR";

  WinkWrapper wrapper = WinkWrapper.getInstance();
  String intermediateTemp = "temp";
  String csvExt = ".csv";

  private AggregateInfo aggInfo = null;
  private String dataPath = null;
  private String version = null;
  private boolean isGUI;

  public PermissionTask(AggregateInfo aggInfo, String dataPath, String version, boolean isGUI) {
    super();
    this.aggInfo = aggInfo;
    this.dataPath = dataPath;
    this.version = version;
    this.isGUI = isGUI;
  }

  @Override
  protected Void doInBackground() throws Exception {
    setString(IN_PROGRESS_STRING);

    WinkWrapper winkWrapper = WinkWrapper.getInstance();

    String className = this.getClass().getSimpleName();
    if (aggInfo == null) {
      System.out.println("aggInfo must be specified " + className);
    }

    if (dataPath == null || dataPath.length() == 0) {
      System.out.println("dataPath must be specified for " + CREATE_OP + "operation with "
          + className);
      return null;
    }

    wrapper.uploadPermissionCSV(dataPath);

    Thread.sleep(PUSH_FINISH_WAIT);
    winkWrapper.updateTableList();

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
    } finally {
      setIndeterminate(false);
    }
  }


}
