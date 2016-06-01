package net;

import model.AggregateInfo;
import org.apache.wink.json4j.JSONException;
import ui.DialogUtils;
import ui.SuitcaseProgressBar;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static ui.MessageString.*;

public class LoginTask extends SuitcaseSwingWorker<Void> {
  private AggregateInfo aggInfo;
  private boolean isGUI;

  public LoginTask(AggregateInfo aggInfo, boolean isGUI) {
    this.aggInfo = aggInfo;
    this.isGUI = isGUI;
  }

  @Override
  protected Void doInBackground() throws Exception {
    WinkWrapper wink = WinkWrapper.getInstance();

    wink.reset();
    wink.init(aggInfo);

    return null;
  }

  @Override
  protected void finished() {
    try {
      get();

      setString(SuitcaseProgressBar.PB_IDLE);
      setIndeterminate(false);
    } catch (InterruptedException e) {
      e.printStackTrace();
      DialogUtils.showError(GENERIC_ERR, isGUI);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();

      String errMsg;
      if (cause instanceof JSONException) {
        errMsg = BAD_CRED;
      } else if (cause instanceof IOException) {
        errMsg = HTTP_IO_ERROR;
      } else {
        errMsg = GENERIC_ERR;
      }

      DialogUtils.showError(errMsg, isGUI);
      cause.printStackTrace();
    }
  }
}
