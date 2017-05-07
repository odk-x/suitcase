package org.opendatakit.suitcase.net;

import static org.opendatakit.suitcase.ui.MessageString.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.zip.DataFormatException;
import java.util.ArrayList;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

import org.opendatakit.suitcase.model.AggregateInfo;
import org.apache.http.client.ClientProtocolException;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.aggregate.odktables.rest.RFC4180CsvReader;
import org.opendatakit.aggregate.odktables.rest.entity.DataKeyValue;
import org.opendatakit.aggregate.odktables.rest.entity.Row;
import org.opendatakit.aggregate.odktables.rest.entity.RowFilterScope;
import org.opendatakit.aggregate.odktables.rest.entity.RowOutcome;
import org.opendatakit.aggregate.odktables.rest.entity.RowOutcome.OutcomeType;
import org.opendatakit.aggregate.odktables.rest.entity.RowOutcomeList;
import org.opendatakit.sync.client.SyncClient;
import org.opendatakit.suitcase.ui.DialogUtils;
import org.opendatakit.suitcase.ui.SuitcaseProgressBar;
import org.opendatakit.suitcase.utils.FileUtils;

public class UpdateTask extends SuitcaseSwingWorker<Void> {
  private static final String IN_PROGRESS_STRING = "Updating...";
  private static final int PUSH_FINISH_WAIT = 5000;
  public static final String FORCE_UPDATE_OP = "FORCE_UPDATE";
  public static final String UPDATE_OP = "UPDATE";
  public static final String NEW_OP = "NEW";
  public static final String DELETE_OP = "DELETE";
  public static final String OP_STR = "operation";
  public static final int MAX_BATCH_SIZE = 500;
  public static final String DEFAULT_OUTCOME_FILE_NAME = "outcomeFile.txt";

  private AggregateInfo aggInfo;
  private String dataPath;
  private String outcomePath;
  private String version;
  private String tableId;
  private boolean isGUI;

  public UpdateTask(AggregateInfo aggInfo, String dataPath, String version, String tableId,
      String outcomePath, boolean isGUI) {
    super();

    this.aggInfo = aggInfo;
    this.dataPath = dataPath;
    this.version = version;
    this.tableId = tableId;
    this.isGUI = isGUI;

    this.outcomePath = outcomePath;
    if (this.outcomePath == null || this.outcomePath.length() == 0) {
      this.outcomePath = FileUtils.getUpdateSavePath().toString() + File.separator
          + DEFAULT_OUTCOME_FILE_NAME;
    }
  }

  @Override
  protected Void doInBackground() throws IOException, JSONException, InterruptedException {
    setString(IN_PROGRESS_STRING);

    SyncWrapper syncWrapper = SyncWrapper.getInstance();

    // We always want to update the table list as
    // things could have changed during the update
    syncWrapper.updateTableList();

    // If tableId is not passed in then do nothing
    if (tableId == null) {
      return null;
    }

    if (dataPath == null) {
      return null;
    }

    String csvFilePath = dataPath;
    File f = new File(csvFilePath);
    FileInputStream in = null;
    InputStreamReader inReader = null;
    RFC4180CsvReader csvReader = null;

    // Check to make sure that the file exists
    if (f.exists()) {
      in = new FileInputStream(f);
      inReader = new InputStreamReader(in);
      csvReader = new RFC4180CsvReader(inReader);
    } else {
      throw new IllegalArgumentException("Update Task requires a valid file");
    }

    // First check that the right number of columns are present
    // The first seven columns are our metadata columns and
    // the last three columns are also our metadata columns
    // NOTE: This could change in the future
    String[] firstLine;
    String operation = null;
    int opIdx = 0, rowIdIdx = 1, rowFormIdIdx = 2, rowLocaleIdx = 3, rowSavepointTypeIdx = 4, rowSavepointTimestampIdx = 5, rowSavepointCreatorIdx = 6;
    String rowId = null;
    String rowFormId = null;
    String rowLocale = null;
    String rowSavepointType = null;
    String rowSavepointTimestamp = null;
    String rowSavepointCreator = null;
    String rowETag = null;
    String rowDefaultAccess = null;
    String rowOwner = null;
    String rowGroupReadOnly = null;
    String rowGroupModify = null;
    String rowGroupPrivileged = null;

    firstLine = csvReader.readNext();
    if (firstLine == null) {
      throw new IllegalArgumentException("The number of columns present in the CSV is not correct");
    }

    operation = firstLine[opIdx];
    rowId = firstLine[rowIdIdx];
    rowFormId = firstLine[rowFormIdIdx];
    rowLocale = firstLine[rowLocaleIdx];
    rowSavepointType = firstLine[rowSavepointTypeIdx];
    rowSavepointTimestamp = firstLine[rowSavepointTimestampIdx];
    rowSavepointCreator = firstLine[rowSavepointCreatorIdx];

    int lastCol = firstLine.length - 1;
    int rowETagIdx = lastCol - 5;
    int rowDefaultAccessIdx = lastCol - 4;
    int rowOwnerIdx = lastCol - 3;
    int rowGroupReadOnlyIdx = lastCol - 2;
    int rowGroupModifyIdx = lastCol - 1;
    int rowGroupPrivilegedIdx = lastCol;
    
    rowETag = firstLine[rowETagIdx];
    rowDefaultAccess = firstLine[rowDefaultAccessIdx];
    rowOwner = firstLine[rowOwnerIdx];
    rowGroupReadOnly = firstLine[rowGroupReadOnlyIdx];
    rowGroupModify = firstLine[rowGroupModifyIdx];
    rowGroupPrivileged = firstLine[rowGroupPrivilegedIdx];

    if ((!operation.equals(OP_STR)) || (!rowId.equals(SyncClient.ID_ROW_DEF))
        || (!rowFormId.equals(SyncClient.FORM_ID_ROW_DEF))
        || (!rowLocale.equals(SyncClient.LOCALE_ROW_DEF))
        || (!rowSavepointType.equals(SyncClient.SAVEPOINT_TYPE_ROW_DEF))
        || (!rowSavepointTimestamp.equals(SyncClient.SAVEPOINT_TIMESTAMP_ROW_DEF))
        || (!rowSavepointCreator.equals(SyncClient.SAVEPOINT_CREATOR_ROW_DEF))) {

      throw new IllegalArgumentException(
          "The number of columns in CSV does not contain the first set of metadata columns");
    }

    if ((!rowETag.equals(SyncClient.ROW_ETAG_ROW_DEF))
        || (!rowDefaultAccess.equals(SyncClient.DEFAULT_ACCESS_ROW_DEF))
        || (!rowOwner.equals(SyncClient.OWNER_ROW_DEF))
        || (!rowGroupReadOnly.equals(SyncClient.GROUP_READ_ONLY_ROW_DEF))
        || (!rowGroupModify.equals(SyncClient.GROUP_MODIFY_ROW_DEF))
        || (!rowGroupPrivileged.equals(SyncClient.GROUP_PRIVILEGED_ROW_DEF))){
      throw new IllegalArgumentException(
          "The number of columns in CSV does not contain the last set of metadata columns");
    }
    
    // Get the current rows in the table
    // in order to pull the rowETags
    SortedMap<String, String> rowIdToRowETag = new TreeMap<>();
    JSONObject rows;
    String cursor = null;
    JSONArray rowResArray = new JSONArray();
    do {
      rows = syncWrapper.getRows(tableId, cursor);
      cursor = rows.optString(SyncClient.WEB_SAFE_RESUME_CURSOR_JSON);
      JSONArray rowsArray = rows.getJSONArray(SyncClient.ROWS_STR_JSON);
      rowResArray.addAll(rowsArray);
    } while (rows.getBoolean(SyncClient.HAS_MORE_RESULTS_JSON));

    setupRowIdToRowETagMap(rowIdToRowETag, rowResArray);

    String[] lineIn;
    ArrayList<Row> newRowArrayList = new ArrayList<Row>();
    ArrayList<Row> deletedRowArrayList = new ArrayList<Row>();
    ArrayList<Row> updatedRowArrayList = new ArrayList<Row>();
    ArrayList<Row> forceUpdatedRowArrayList = new ArrayList<Row>();
    while ((lineIn = csvReader.readNext()) != null) {

      operation = lineIn[opIdx];
      rowId = lineIn[rowIdIdx];
      rowFormId = lineIn[rowFormIdIdx];
      rowLocale = lineIn[rowLocaleIdx];
      rowSavepointType = lineIn[rowSavepointTypeIdx];
      rowSavepointTimestamp = lineIn[rowSavepointTimestampIdx];
      rowSavepointCreator = lineIn[rowSavepointCreatorIdx];
      rowETag = lineIn[rowETagIdx];
      rowDefaultAccess = lineIn[rowDefaultAccessIdx];
      rowOwner = lineIn[rowOwnerIdx];
      rowGroupReadOnly = lineIn[rowGroupReadOnlyIdx];
      rowGroupModify = lineIn[rowGroupModifyIdx];
      rowGroupPrivileged = lineIn[rowGroupPrivilegedIdx];

      ArrayList<DataKeyValue> dkvl = new ArrayList<DataKeyValue>();
      for (int i = 7; i < lineIn.length - 6; i++) {
        DataKeyValue dkv = new DataKeyValue(firstLine[i], lineIn[i]);
        dkvl.add(dkv);
      }

      String opToCompare = operation.toUpperCase();

      String existingRowETag = rowIdToRowETag.get(rowId);

      switch (opToCompare) {
      // Figure out what rows need to be force updated
      case FORCE_UPDATE_OP:
        Row forceUpdatedRow = Row.forUpdate(rowId, rowETag, rowFormId, rowLocale, rowSavepointType,
            rowSavepointTimestamp, rowSavepointCreator,
            RowFilterScope.asRowFilter(rowDefaultAccess, rowOwner, rowGroupReadOnly, rowGroupModify, rowGroupPrivileged), dkvl);
        if (existingRowETag != null) {
          forceUpdatedRow.setRowETag(existingRowETag);
        }
        forceUpdatedRowArrayList.add(forceUpdatedRow);
        break;

      // Figure out what rows need to be updated
      case UPDATE_OP:
        Row updatedRow = Row.forUpdate(rowId, rowETag, rowFormId, rowLocale, rowSavepointType,
            rowSavepointTimestamp, rowSavepointCreator,
            RowFilterScope.asRowFilter(rowDefaultAccess, rowOwner, rowGroupReadOnly, rowGroupModify, rowGroupPrivileged), dkvl);
        if (existingRowETag != null) {
          updatedRow.setRowETag(existingRowETag);
        }
        updatedRowArrayList.add(updatedRow);
        break;

      // Figure out what rows need to be added
      case NEW_OP:
        Row insertedRow = Row.forInsert(rowId, rowFormId, rowLocale, rowSavepointType,
            rowSavepointTimestamp, rowSavepointCreator,
            RowFilterScope.asRowFilter(rowDefaultAccess, rowOwner, rowGroupReadOnly, rowGroupModify, rowGroupPrivileged), dkvl);
        if (existingRowETag != null) {
          insertedRow.setRowETag(existingRowETag);
        }
        newRowArrayList.add(insertedRow);
        break;

      // Figure out what rows need to be deleted
      case DELETE_OP:
        Row deletedRow = Row.forUpdate(rowId, rowETag, rowFormId, rowLocale, rowSavepointType,
            rowSavepointTimestamp, rowSavepointCreator,
            RowFilterScope.asRowFilter(rowDefaultAccess, rowOwner, rowGroupReadOnly, rowGroupModify, rowGroupPrivileged), dkvl);
        if (existingRowETag != null) {
          deletedRow.setRowETag(existingRowETag);
        }
        deletedRow.setDeleted(true);
        deletedRowArrayList.add(deletedRow);
        break;

      default:
        throw new IllegalArgumentException("Operation " + operation + " is not supported");
      }
    }

    // Get the schemaETag
    // Get the dataETag
    // Finally bulk upload the rows with the different collections
    String schemaETag = null;
    if (aggInfo.tableIdExists(tableId)) {
      schemaETag = aggInfo.getSchemaETag(tableId);
    }

    // Used to handle row outcomes
    ArrayList<RowOutcome> outcomeList = null;
    
    // Handle the new rows
    String dataETag = null;
    if (newRowArrayList.size() > 0) {
      dataETag = syncWrapper.getDataETag(tableId, schemaETag);
      outcomeList = handleRowBatches(syncWrapper, dataPath, tableId, newRowArrayList);
      if (outcomeList != null && outcomeList.size() > 0) {
        handleRowOutcomeList(outcomeList);
      }
    }

    // Handle the deleted rows
    if (deletedRowArrayList.size() > 0) {
      dataETag = syncWrapper.getDataETag(tableId, schemaETag);
      outcomeList = handleRowBatches(syncWrapper, dataPath, tableId, deletedRowArrayList);
      if (outcomeList != null && outcomeList.size() > 0) {
        handleRowOutcomeList(outcomeList);
      }
    }

    // Handle the updated rows
    if (updatedRowArrayList.size() > 0) {
      dataETag = syncWrapper.getDataETag(tableId, schemaETag);
      outcomeList = handleRowBatches(syncWrapper, dataPath, tableId, updatedRowArrayList);
      if (outcomeList != null && outcomeList.size() > 0) {
        handleRowOutcomeList(outcomeList);
      }
    }

    // Handle the force-updated rows
    if (forceUpdatedRowArrayList.size() > 0) {
      dataETag = syncWrapper.getDataETag(tableId, schemaETag);
      outcomeList = handleRowBatches(syncWrapper, dataPath, tableId, forceUpdatedRowArrayList);
      if (outcomeList != null && outcomeList.size() > 0) {
        // Re-run processing on any row that is not successful
        ArrayList<Row> forceUpdatedRowArrayList2 = new ArrayList<Row>();
        for (int i = 0; i < outcomeList.size(); i++) {
          RowOutcome outcome = outcomeList.get(i);
          if (outcome.getOutcome() != OutcomeType.SUCCESS) {
            forceUpdatedRowArrayList2.add(outcome);
          }
        }
        
        if (forceUpdatedRowArrayList2.size() > 0) {
          dataETag = syncWrapper.getDataETag(tableId, schemaETag);
          outcomeList = handleRowBatches(syncWrapper, dataPath, tableId, forceUpdatedRowArrayList2);
        }

        // Then handleRowOutcomeList
        handleRowOutcomeList(outcomeList);
      }
    }

    Thread.sleep(PUSH_FINISH_WAIT);
    syncWrapper.updateTableList();

    return null;
  }

  protected ArrayList<RowOutcome> handleRowBatches(SyncWrapper syncWrapper, String dataPath, String tableId,
      ArrayList<Row> rows) throws ClientProtocolException, IOException, JSONException {

    ArrayList<Row> batchedRows = new ArrayList<Row>();
    RowOutcomeList rowOutcomeList = null;
    ArrayList<RowOutcome> outcomes = new ArrayList<RowOutcome>();
    int i = 0;
    while (i < rows.size()) {
      batchedRows.add(rows.get(i));

      if (batchedRows.size() > MAX_BATCH_SIZE) {
        rowOutcomeList = syncWrapper.alterRowsUsingSingleBatch(tableId, batchedRows);
        if (rowOutcomeList != null) {
          outcomes.addAll(rowOutcomeList.getRows());
        }

        batchedRows = new ArrayList<Row>();
      }
      i++;
    }

    if (batchedRows.size() > 0) {
      rowOutcomeList = syncWrapper.alterRowsUsingSingleBatch(tableId, batchedRows);
      if (rowOutcomeList != null) {
        outcomes.addAll(rowOutcomeList.getRows());
      }

    }
    return outcomes;
  }

  public void handleRowOutcomeList(ArrayList<RowOutcome> outcomes) throws IOException {
    FileWriter fw = null;

    try {
      File outcomeFile = new File(outcomePath);
      if (!outcomeFile.exists()) {
        outcomeFile.getParentFile().mkdirs();
        outcomeFile.createNewFile();
      }

      fw = new FileWriter(outcomeFile, true);
      for (int i = 0; i < outcomes.size(); i++) {
        RowOutcome outcome = outcomes.get(i);

        Date currDate = new Date();
        // Use RowOutcomeList to show the status of each row
        // Eventually a database should be used to store the output
        fw.write(currDate + " row Id: " + outcome.getRowId() + " had outcome "
              + outcome.getOutcome());
        fw.write(System.lineSeparator());
        if (outcome.getOutcome() != OutcomeType.SUCCESS) {
          System.out.println("row Id: " + outcome.getRowId() + " had outcome "
              + outcome.getOutcome());
        }
      }
    } finally {
      if (fw != null) {
        fw.close();
      }
    }
  }

  public void setupRowIdToRowETagMap(SortedMap<String, String> idToETagMap, JSONArray rowsArray) {
    try {
      for (int i = 0; i < rowsArray.size(); i++) {
        JSONObject rowObj = rowsArray.getJSONObject(i);
        String rowId = rowObj.has(SyncClient.ID_JSON) && !rowObj.isNull(SyncClient.ID_JSON) ? rowObj
            .getString(SyncClient.ID_JSON) : null;
        String rowETag = rowObj.has(SyncClient.ROW_ETAG_JSON)
            && !rowObj.isNull(SyncClient.ROW_ETAG_JSON) ? rowObj
            .getString(SyncClient.ROW_ETAG_JSON) : null;
        if (rowId != null && rowETag != null) {
          idToETagMap.put(rowId, rowETag);
        }
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
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
