package org.opendatakit.suitcase.net;

import static org.opendatakit.suitcase.ui.MessageString.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.zip.DataFormatException;

import org.opendatakit.suitcase.model.CloudEndpointInfo;
import org.apache.http.client.ClientProtocolException;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.aggregate.odktables.rest.RFC4180CsvReader;
import org.opendatakit.aggregate.odktables.rest.SavepointTypeManipulator;
import org.opendatakit.aggregate.odktables.rest.TableConstants;
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
import org.opendatakit.suitcase.utils.SuitcaseConst;

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

  private CloudEndpointInfo cloudEndpointInfo;
  private String dataPath;
  private String outcomePath;
  private String version;
  private String tableId;
  private boolean isGUI;
  
  private Map<String, Integer> colToIdx = new HashMap<String, Integer>();
  private static ArrayList<String> metadataColumns = new ArrayList<String>();
  
  static {
    metadataColumns.add(OP_STR);
    metadataColumns.add(SyncClient.ID_ROW_DEF);
    metadataColumns.add(SyncClient.FORM_ID_ROW_DEF);
    metadataColumns.add(SyncClient.LOCALE_ROW_DEF);
    metadataColumns.add(SyncClient.SAVEPOINT_TYPE_ROW_DEF); 
    metadataColumns.add(SyncClient.SAVEPOINT_TIMESTAMP_ROW_DEF);
    metadataColumns.add(SyncClient.SAVEPOINT_CREATOR_ROW_DEF);
    metadataColumns.add(SyncClient.ROW_ETAG_ROW_DEF);
    metadataColumns.add(SyncClient.DEFAULT_ACCESS_ROW_DEF);
    metadataColumns.add(SyncClient.ROW_OWNER_ROW_DEF); 
    metadataColumns.add(SyncClient.GROUP_READ_ONLY_ROW_DEF);
    metadataColumns.add(SyncClient.GROUP_MODIFY_ROW_DEF);
    metadataColumns.add(SyncClient.GROUP_PRIVILEGED_ROW_DEF);
    metadataColumns.add(SyncClient.CREATE_USER_ROW_DEF);
    metadataColumns.add(SyncClient.LAST_UPDATE_USER_ROW_DEF);
    metadataColumns.add(SyncClient.DELETED_ROW_DEF);
    metadataColumns.add(SyncClient.DATA_ETAG_AT_MODIFICATION_ROW_DEF);
  }

  public UpdateTask(CloudEndpointInfo cloudEndpointInfo, String dataPath, String version, String tableId,
                    String outcomePath, boolean isGUI) {
    super();

    this.cloudEndpointInfo = cloudEndpointInfo;
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
  
  protected void verifyColumns(String[] headerCols) {
    for (int i = 0; i < headerCols.length; i++) {
      String col = headerCols[i];
          colToIdx.put(col, i);
    }
    
    if (!colToIdx.containsKey(OP_STR)) {
      throw new IllegalArgumentException(
          "CSV does not contain metadata column:" + OP_STR);
    }
    
    if (!colToIdx.containsKey(SyncClient.ID_ROW_DEF)) {
      throw new IllegalArgumentException(
          "CSV does not contain metadata column:" + SyncClient.ID_ROW_DEF);
    }
    
    if (!colToIdx.containsKey(SyncClient.FORM_ID_ROW_DEF)) {
      throw new IllegalArgumentException(
          "CSV does not contain metadata column:" + SyncClient.FORM_ID_ROW_DEF);
    }
    
    if (!colToIdx.containsKey(SyncClient.LOCALE_ROW_DEF)) {
      throw new IllegalArgumentException(
          "CSV does not contain metadata column:" + SyncClient.LOCALE_ROW_DEF);   
    }
    
    if (!colToIdx.containsKey(SyncClient.SAVEPOINT_TYPE_ROW_DEF)) {
      throw new IllegalArgumentException(
          "CSV does not contain metadata column:" + SyncClient.SAVEPOINT_TYPE_ROW_DEF);   
    }
    
    if (!colToIdx.containsKey(SyncClient.SAVEPOINT_TIMESTAMP_ROW_DEF)) {
      throw new IllegalArgumentException(
          "CSV does not contain metadata column:" + SyncClient.SAVEPOINT_TIMESTAMP_ROW_DEF); 
    }
    
    if (!colToIdx.containsKey(SyncClient.SAVEPOINT_CREATOR_ROW_DEF)) {
      throw new IllegalArgumentException(
          "CSV does not contain metadata column:" + SyncClient.SAVEPOINT_CREATOR_ROW_DEF); 
    }
    
    if (!colToIdx.containsKey(SyncClient.ROW_ETAG_ROW_DEF)) {
      throw new IllegalArgumentException(
          "CSV does not contain metadata column:" + SyncClient.ROW_ETAG_ROW_DEF); 
    }
    
    if (!colToIdx.containsKey(SyncClient.DEFAULT_ACCESS_ROW_DEF)) {
      throw new IllegalArgumentException(
          "CSV does not contain metadata column:" + SyncClient.DEFAULT_ACCESS_ROW_DEF); 
    }
    
    if (!colToIdx.containsKey(SyncClient.ROW_OWNER_ROW_DEF)) {
      throw new IllegalArgumentException(
          "CSV does not contain metadata column:" + SyncClient.ROW_OWNER_ROW_DEF); 
    }
    
    if (!colToIdx.containsKey(SyncClient.GROUP_READ_ONLY_ROW_DEF)) {
      throw new IllegalArgumentException(
          "CSV does not contain metadata column:" + SyncClient.GROUP_READ_ONLY_ROW_DEF); 
    }
    
    if (!colToIdx.containsKey(SyncClient.GROUP_MODIFY_ROW_DEF)) {
      throw new IllegalArgumentException(
          "CSV does not contain metadata column:" + SyncClient.GROUP_MODIFY_ROW_DEF); 
    }
    
    if (!colToIdx.containsKey(SyncClient.GROUP_PRIVILEGED_ROW_DEF)) {
      throw new IllegalArgumentException(
          "CSV does not contain metadata column:" + SyncClient.GROUP_PRIVILEGED_ROW_DEF); 
    }
  }

  protected boolean isDataColumn(String colName) {
    if (colName == null || colName.length() == 0) {
      return false;
    }
    
    if (!metadataColumns.contains(colName)) {
      return true;
    } else {
      return false;
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
      throw new IllegalArgumentException("This CSV is empty.");
    }
    
    verifyColumns(firstLine);
    
    int opIdx = colToIdx.get(OP_STR);
    int rowIdIdx = colToIdx.get(SyncClient.ID_ROW_DEF);
    int rowFormIdIdx = colToIdx.get(SyncClient.FORM_ID_ROW_DEF);
    int rowLocaleIdx = colToIdx.get(SyncClient.LOCALE_ROW_DEF);
    int rowSavepointTypeIdx = colToIdx.get(SyncClient.SAVEPOINT_TYPE_ROW_DEF);
    int rowSavepointTimestampIdx = colToIdx.get(SyncClient.SAVEPOINT_TIMESTAMP_ROW_DEF);
    int rowSavepointCreatorIdx = colToIdx.get(SyncClient.SAVEPOINT_CREATOR_ROW_DEF);

    operation = firstLine[opIdx];
    rowId = firstLine[rowIdIdx];
    rowFormId = firstLine[rowFormIdIdx];
    rowLocale = firstLine[rowLocaleIdx];
    rowSavepointType = firstLine[rowSavepointTypeIdx];
    rowSavepointTimestamp = firstLine[rowSavepointTimestampIdx];
    rowSavepointCreator = firstLine[rowSavepointCreatorIdx];

    int rowDefaultAccessIdx = colToIdx.get(SyncClient.DEFAULT_ACCESS_ROW_DEF);
    int rowGroupModifyIdx = colToIdx.get(SyncClient.GROUP_MODIFY_ROW_DEF);
    int rowGroupPrivilegedIdx = colToIdx.get(SyncClient.GROUP_PRIVILEGED_ROW_DEF);
    int rowGroupReadOnlyIdx = colToIdx.get(SyncClient.GROUP_READ_ONLY_ROW_DEF);
    int rowETagIdx = colToIdx.get(SyncClient.ROW_ETAG_ROW_DEF);
    int rowOwnerIdx = colToIdx.get(SyncClient.ROW_OWNER_ROW_DEF);
    
    rowETag = firstLine[rowETagIdx];
    rowDefaultAccess = firstLine[rowDefaultAccessIdx];
    rowOwner = firstLine[rowOwnerIdx];
    rowGroupReadOnly = firstLine[rowGroupReadOnlyIdx];
    rowGroupModify = firstLine[rowGroupModifyIdx];
    rowGroupPrivileged = firstLine[rowGroupPrivilegedIdx];

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
      for (int i = 0; i < lineIn.length; i++) {
        if (isDataColumn(firstLine[i])) {
          DataKeyValue dkv = new DataKeyValue(firstLine[i], lineIn[i]);
          dkvl.add(dkv);
        } 
      }

      String opToCompare = operation != null ? operation.toUpperCase() : "";

      String existingRowETag = rowId != null ? rowIdToRowETag.get(rowId) : null;
      
      // If the operation is an add, update, or force_update,
      // we need to populate savepoint_creator,
      // savepoint_timestamp, savepoint_type, and locale if
      // the value is not supplied
      if (opToCompare.equals(FORCE_UPDATE_OP) || opToCompare.equals(UPDATE_OP) ||
          opToCompare.equals(NEW_OP)) {
        
        if (rowSavepointCreator == null || rowSavepointCreator.length() == 0) {
          rowSavepointCreator = SuitcaseConst.ANONYMOUS_USER;
          String suppliedUserName = cloudEndpointInfo.getUserName();
          if (suppliedUserName != null && suppliedUserName.length() > 0) {
            String privilegedUserName = cloudEndpointInfo.getPrivilegedUserName();
            if (privilegedUserName != null && privilegedUserName.length() > 0) {
              rowSavepointCreator = cloudEndpointInfo.getPrivilegedUserName();
            }
          }
        } 
        
        if (rowSavepointTimestamp == null || rowSavepointTimestamp.length() == 0) {
          rowSavepointTimestamp =
              TableConstants.nanoSecondsFromMillis(System.currentTimeMillis(), Locale.ROOT);
        }
        
        if (rowSavepointType == null || rowSavepointType.length() == 0) {
          rowSavepointType = SavepointTypeManipulator.complete();
        }
        
        if (rowLocale == null || rowLocale.length() == 0) {
          rowLocale = SuitcaseConst.DEFAULT_LOCALE;
        }
      }

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
    if (cloudEndpointInfo.tableIdExists(tableId)) {
      schemaETag = cloudEndpointInfo.getSchemaETag(tableId);
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
          String v_savepoint_timestamp =
              TableConstants.nanoSecondsFromMillis(System.currentTimeMillis(), Locale.ROOT);
          Row updatedRow = Row.forUpdate(outcome.getRowId(), outcome.getRowETag(), outcome.getFormId(), 
              outcome.getLocale(), outcome.getSavepointType(), v_savepoint_timestamp, 
              outcome.getSavepointCreator(), outcome.getRowFilterScope(), outcome.getValues());
          
          if (outcome.getOutcome() != OutcomeType.SUCCESS) {
            forceUpdatedRowArrayList2.add(updatedRow);
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
