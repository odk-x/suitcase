package org.opendatakit.suitcase.net;
import org.apache.wink.json4j.JSONException;
import org.opendatakit.suitcase.ui.DialogUtils;
import org.opendatakit.suitcase.ui.ProgressBarStatus;
import org.opendatakit.suitcase.ui.SuitcaseProgressBar;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.opendatakit.suitcase.ui.MessageString.*;
import static org.opendatakit.suitcase.ui.MessageString.GENERIC_ERR;

public class RefreshTask extends SuitcaseSwingWorker<Void>{
    private static final String IN_PROGRESS_STRING = "Refreshing...";

    @Override
    protected Void doInBackground() throws JSONException, IOException, InterruptedException {
        setString(IN_PROGRESS_STRING);

        SyncWrapper syncWrapper = SyncWrapper.getInstance();

        publish(new ProgressBarStatus(0, "Fetching List of tables", true));

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
            DialogUtils.showError(GENERIC_ERR, true);
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
            DialogUtils.showError(errMsg, true);
            setString(SuitcaseProgressBar.PB_ERROR);
            returnCode = SuitcaseSwingWorker.errorCode;
        } finally {
            setIndeterminate(false);
        }
    }
}
