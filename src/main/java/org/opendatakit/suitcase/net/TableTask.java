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
import org.opendatakit.suitcase.model.CloudEndpointInfo;
import org.opendatakit.suitcase.ui.DialogUtils;
import org.opendatakit.suitcase.ui.SuitcaseProgressBar;
import org.opendatakit.sync.client.SyncClient;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TableTask extends SuitcaseSwingWorker<Void> {
  private static final String IN_PROGRESS_STRING = "Updating Table...";
  private static final int PUSH_FINISH_WAIT = 5000;

  public final static String CREATE_OP = "CREATE";
  public final static String DELETE_OP = "DELETE";
  public final static String CLEAR_OP = "CLEAR";

  SyncWrapper wrapper = SyncWrapper.getInstance();
  String intermediateTemp = "temp";
  String csvExt = ".csv";

  private CloudEndpointInfo cloudEndpointInfo = null;
  private String operation = null;
  private String tableId = null;
  private String dataPath = null;
  private String version = null;
  private boolean isGUI;

  public TableTask(CloudEndpointInfo cloudEndpointInfo, String tableId, String dataPath, String version,
                   String operation, boolean isGUI) {
    super();
    this.cloudEndpointInfo = cloudEndpointInfo;
    this.tableId = tableId;
    this.dataPath = dataPath;
    this.version = version;
    this.operation = operation;
    this.isGUI = isGUI;
  }

  @Override
  protected Void doInBackground() throws IOException, JSONException, InterruptedException {
    setString(IN_PROGRESS_STRING);

    SyncWrapper syncWrapper = SyncWrapper.getInstance();

    // We always want to update the table list as
    // things could have changed
    syncWrapper.updateTableList();

    String className = this.getClass().getSimpleName();
    if (cloudEndpointInfo == null) {
      System.out.println("cloudEndpointInfo must be specified " + className);
    }

    if (tableId == null || tableId.length() == 0) {
      System.out.println("tableId must be specified " + className);
    }
    
    if (operation == null || operation.length() == 0) {
      System.out.println("operation must be specified " + className);
    }

    String op = operation.toUpperCase();

    switch (op) {
    case CREATE_OP:
      if (dataPath == null || dataPath.length() == 0) {
        System.out.println("dataPath must be specified for " + CREATE_OP + "operation with "
            + className);
        return null;
      }

      wrapper.createTable(tableId, dataPath);
      break;

    case DELETE_OP:
      wrapper.deleteTableDefinition(tableId);
      break;

    case CLEAR_OP:
      JSONObject rows;
      String cursor = null;

      ArrayList<Row> rowList = new ArrayList<Row>();
      ObjectMapper mapper = new ObjectMapper();

      do {
        rows = wrapper.getRows(tableId, cursor);

        RowResourceList rowResListObj = mapper.readValue(rows.toString(), RowResourceList.class);
        ArrayList<RowResource> rowResArrayList = rowResListObj.getRows();
        
        for (int i = 0; i < rowResArrayList.size(); i++) {
          RowResource rowRes = rowResArrayList.get(i);

          Row row = Row.forUpdate(rowRes.getRowId(), rowRes.getRowETag(), rowRes.getFormId(), rowRes.getLocale(), 
              rowRes.getSavepointType(), rowRes.getSavepointTimestamp(), rowRes.getSavepointCreator(), rowRes.getRowFilterScope(), 
              rowRes.getValues());
          row.setDeleted(true);
          rowList.add(row);
        }
        cursor = rows.optString(SyncClient.WEB_SAFE_RESUME_CURSOR_JSON);
      } while (rows.getBoolean(SyncClient.HAS_MORE_RESULTS_JSON));
      
      if (rowList.size() > 0) {
        wrapper.deleteRowsUsingBulkUpload(tableId, rowList);
      }

      break;

    default:
      break;
    }

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
