package org.opendatakit.suitcase.net;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.suitcase.ui.DialogUtils;
import org.opendatakit.suitcase.ui.ProgressBarStatus;
import org.opendatakit.suitcase.ui.SuitcaseProgressBar;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.opendatakit.suitcase.ui.MessageString.*;
import static org.opendatakit.suitcase.ui.MessageString.GENERIC_ERR;

public class DeleteTask extends SuitcaseSwingWorker<Void>{
    private static final String IN_PROGRESS_STRING = "Deleting...";
    private String tableID;
    private String version;

    public DeleteTask(String tableID, String version){
        this.tableID = tableID;
    }

    @Override
    protected Void doInBackground() throws IOException, JSONException {
        setString(IN_PROGRESS_STRING);

        SyncWrapper syncWrapper = SyncWrapper.getInstance();

        publish(new ProgressBarStatus(0, "Deleting Table " + tableID, true));

        // First we delete Files
        JSONObject manifest = syncWrapper.getManifestForAppLevelFiles(version);
        JSONArray files = manifest.getJSONArray("files");
        for (int i = 0; i < files.length(); i++) {
            JSONObject file = files.getJSONObject(i);
            String filePath = file.getString("filename");
            syncWrapper.deleteFile(filePath, version);
        }

        int result = syncWrapper.deleteTableDefinition(tableID);

        if(result != 200) {
            throw new IOException("Unknown Error Occurred");  // If operation has failed throw Exception
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
