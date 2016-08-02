package net;

import static ui.MessageString.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.zip.DataFormatException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import model.AggregateInfo;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.aggregate.odktables.rest.RFC4180CsvReader;
import org.opendatakit.aggregate.odktables.rest.entity.DataKeyValue;
import org.opendatakit.aggregate.odktables.rest.entity.Row;
import org.opendatakit.aggregate.odktables.rest.entity.RowOutcome;
import org.opendatakit.aggregate.odktables.rest.entity.RowOutcome.OutcomeType;
import org.opendatakit.aggregate.odktables.rest.entity.RowOutcomeList;
import org.opendatakit.aggregate.odktables.rest.entity.Scope;
import org.opendatakit.wink.client.WinkClient;

import ui.DialogUtils;
import ui.SuitcaseProgressBar;

public class UpdateTask  extends SuitcaseSwingWorker<Void> {
	  private static final String IN_PROGRESS_STRING = "Updating...";
	  private static final int PUSH_FINISH_WAIT = 5000;
	  public static final String FORCE_UPDATE_OP = "FORCE_UPDATE";
	  public static final String UPDATE_OP = "UPDATE";
	  public static final String NEW_OP = "NEW";
	  public static final String DELETE_OP = "DELETE";
	  public static final String OP_STR = "operation";
	  public static final int MAX_BATCH_SIZE = 500;
	  
	  private AggregateInfo aggInfo;
	  private String dataPath;
	  private String version;
	  private String tableId;
	  private boolean isGUI;

	  public UpdateTask(AggregateInfo aggInfo, String dataPath, String version, String tableId, boolean isGUI) {
	  	super();

	    this.aggInfo = aggInfo;
	    this.dataPath = dataPath;
	    this.version = version;
	    this.tableId = tableId;
	    this.isGUI = isGUI;
	  }

	  @Override
	  protected Void doInBackground() throws Exception {
	    setString(IN_PROGRESS_STRING);

	    WinkWrapper wink = WinkWrapper.getInstance();
	    
	    // We always want to update the table list as 
	    // things could have changed during the update
	    wink.updateTableList();
	    
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
	    String [] firstLine;
	    String operation = null;
	    int opIdx = 0, rowIdIdx = 1, rowFormIdIdx = 2, rowLocaleIdx = 3, rowSavepointTypeIdx = 4, rowSavepointTimestampIdx = 5,
	        rowSavepointCreatorIdx = 6;
	    String rowId = null;
	    String rowFormId = null;
	    String rowLocale = null;
	    String rowSavepointType = null;
	    String rowSavepointTimestamp = null;
	    String rowSavepointCreator = null;
	    String rowETag = null;
	    String rowFilterType = null;
	    String rowFilterValue = null;
	    
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
      int rowETagIdx = lastCol - 2, rowFilterTypeIdx = lastCol - 1, rowFilterValueIdx = lastCol;
	    rowETag = firstLine[rowETagIdx];
	    rowFilterType = firstLine[rowFilterTypeIdx];
	    rowFilterValue = firstLine[rowFilterValueIdx];
	    
	    if ((!operation.equals(OP_STR)) || 
	        (!rowId.equals(WinkClient.rowDefId)) ||
	        (!rowFormId.equals(WinkClient.rowDefFormId)) || 
	        (!rowLocale.equals(WinkClient.rowDefLocale)) || 
					(!rowSavepointType.equals(WinkClient.rowDefSavepointType)) ||
          (!rowSavepointTimestamp.equals(WinkClient.rowDefSavepointTimestamp)) ||
          (!rowSavepointCreator.equals(WinkClient.rowDefSavepointCreator))) {
	      
	      throw new IllegalArgumentException("The number of columns in CSV does not contain the first set of metadata columns");
	    }
	    
	    if ((!rowETag.equals(WinkClient.rowDefRowETag))||
	        (!rowFilterType.equals(WinkClient.rowDefFilterType)) ||
	        (!rowFilterValue.equals(WinkClient.rowDefFilterValue))) {
	      throw new IllegalArgumentException("The number of columns in CSV does not contain the last set of metadata columns");
	    }
	   
	    String [] lineIn;
	    ArrayList<Row> newRowArrayList = new ArrayList<Row>();
	    ArrayList<Row> deletedRowArrayList = new ArrayList<Row>();
	    ArrayList<Row> updatedRowArrayList = new ArrayList<Row>();
	    ArrayList<Row> forceUpdatedRowArrayList = new ArrayList<Row>();
	    while((lineIn = csvReader.readNext()) != null) {

	      operation = lineIn[opIdx];
	      rowId = lineIn[rowIdIdx];
	      rowFormId = lineIn[rowFormIdIdx];
	      rowLocale = lineIn[rowLocaleIdx];
	      rowSavepointType = lineIn[rowSavepointTypeIdx];
	      rowSavepointTimestamp = lineIn[rowSavepointTimestampIdx];
	      rowSavepointCreator = lineIn[rowSavepointCreatorIdx];
	      rowETag = lineIn[rowETagIdx];
	      rowFilterType = lineIn[rowFilterTypeIdx];
	      rowFilterValue = lineIn[rowFilterValueIdx];
	      
	      ArrayList<DataKeyValue> dkvl = new ArrayList<DataKeyValue>();
	      for (int i = 7; i < lineIn.length - 3; i++) {
	        DataKeyValue dkv = new DataKeyValue(firstLine[i], lineIn[i]);
	        dkvl.add(dkv);
	      }
	      
	      String opToCompare = operation.toUpperCase();
	      
	      // Get the current rows in the table
	      // in order to pull the rowETags
	      SortedMap<String, String> rowIdToRowETag = new TreeMap<>();
	      JSONObject rows;
	      String cursor = null;
	      JSONArray rowResArray = new JSONArray();
	      do {
	        rows = wink.getRows(tableId, cursor);
	        cursor = rows.optString(WinkClient.jsonWebSafeResumeCursor);
	        JSONArray rowsArray = rows.getJSONArray(WinkClient.jsonRowsString);
	        rowResArray.addAll(rowsArray);
	      } while (rows.getBoolean(WinkClient.jsonHasMoreResults));
	      
	      setupRowIdToRowETagMap(rowIdToRowETag, rowResArray);
				String existingRowETag = rowIdToRowETag.get(rowId);
	      
	      switch(opToCompare)
	      {
	        // Figure out what rows need to be force updated
	        case FORCE_UPDATE_OP:
	          Row forceUpdatedRow = Row.forUpdate(rowId, rowETag, rowFormId, rowLocale, rowSavepointType, rowSavepointTimestamp, rowSavepointCreator, Scope.asScope(rowFilterType, rowFilterValue), dkvl);
	          if (existingRowETag != null) {
	            forceUpdatedRow.setRowETag(existingRowETag);
	          }
	          forceUpdatedRowArrayList.add(forceUpdatedRow);
	          break;
	          
	        // Figure out what rows need to be updated
	        case UPDATE_OP: 
	          Row updatedRow = Row.forUpdate(rowId, rowETag, rowFormId, rowLocale, rowSavepointType, rowSavepointTimestamp, rowSavepointCreator, Scope.asScope(rowFilterType, rowFilterValue), dkvl);
	          if (existingRowETag != null) {
	            updatedRow.setRowETag(existingRowETag);
	          }
	          updatedRowArrayList.add(updatedRow);
	          break;
	          
	        // Figure out what rows need to be added
	        case NEW_OP:
	          Row insertedRow = Row.forInsert(rowId, rowFormId, rowLocale, rowSavepointType, rowSavepointTimestamp, rowSavepointCreator, Scope.asScope(rowFilterType, rowFilterValue), dkvl);
	          if (existingRowETag != null) {
	            insertedRow.setRowETag(existingRowETag);
	          }
	          newRowArrayList.add(insertedRow);
	          break;
	        
	        // Figure out what rows need to be deleted
	        case DELETE_OP:
	          Row deletedRow = Row.forUpdate(rowId, rowETag, rowFormId, rowLocale, rowSavepointType, rowSavepointTimestamp, rowSavepointCreator, Scope.asScope(rowFilterType, rowFilterValue), dkvl);
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
	    } else {
	      schemaETag = wink.getSchemaETagForTable(tableId);
	    }
	    
	     // Handle the new rows
	    String dataETag = null;
	    if (newRowArrayList.size() > 0) {
	      dataETag = wink.getDataETag(tableId, schemaETag);
	      handleRowBatches(wink, dataPath, tableId, schemaETag, dataETag, newRowArrayList);
	    }
    
	    // Handle the deleted rows
	    if (deletedRowArrayList.size() > 0) {
	     dataETag = wink.getDataETag(tableId, schemaETag);
	     handleRowBatches(wink, dataPath, tableId, schemaETag, dataETag, deletedRowArrayList);
	    }
	    
	    // Handle the updated rows
	    if (updatedRowArrayList.size() > 0) {
	      dataETag = wink.getDataETag(tableId, schemaETag);
	      handleRowBatches(wink, dataPath, tableId, schemaETag, dataETag, updatedRowArrayList);
	    }
	    
	    // Handle the force-updated rows
	    if (forceUpdatedRowArrayList.size() > 0) {
	      dataETag = wink.getDataETag(tableId, schemaETag);
	      handleRowBatches(wink, dataPath, tableId, schemaETag, dataETag, forceUpdatedRowArrayList);
	    }

	    Thread.sleep(PUSH_FINISH_WAIT);
	    wink.updateTableList();

	    return null;
	  }
	  
	  protected void handleRowBatches(WinkWrapper wink, String dataPath, String tableId, String schemaETag, String dataETagVal, ArrayList<Row> rows)  
	    throws Exception {
	   
	    ArrayList<Row> batchedRows = new ArrayList<Row>();
	    RowOutcomeList rowOutcomeList = null;
	    int i = 0;
	    while (i < rows.size()) {
	      batchedRows.add(rows.get(i));
	      
	      if (batchedRows.size() > MAX_BATCH_SIZE) {
	        rowOutcomeList = wink.alterRowsUsingSingleBatch(tableId, schemaETag, dataETagVal, batchedRows);
	        handleRowOutcomeList(rowOutcomeList);
	        batchedRows = new ArrayList<Row>();
	      }
	      i++;
	    }
	    
	    if (batchedRows.size() > 0) {
	      rowOutcomeList = wink.alterRowsUsingSingleBatch(tableId, schemaETag, dataETagVal, batchedRows);
         handleRowOutcomeList(rowOutcomeList);
	    }
	  }
	  
	  public void handleRowOutcomeList(RowOutcomeList rowOutcomeList) {
	    ArrayList<RowOutcome> outcomes = rowOutcomeList.getRows();
	    
	    for (int i = 0; i < outcomes.size(); i++) {
	      RowOutcome outcome = outcomes.get(i);
	      
	      // Use RowOutcomeList to show the status of each row 
	      // to the console - this should be changed to a log file
	      // Eventually a database should be used to store the output
	      if (outcome.getOutcome() != OutcomeType.SUCCESS) {
	        System.out.println("row Id: " + outcome.getRowId() + " had outcome " + outcome.getOutcome());
	      }
	    }
	  }
     
     public void setupRowIdToRowETagMap(SortedMap<String, String> idToETagMap, JSONArray rowsArray) {
       try {
         for (int i = 0; i < rowsArray.size(); i++) {
           JSONObject rowObj = rowsArray.getJSONObject(i);
           String rowId = rowObj.has(WinkClient.jsonId) && !rowObj.isNull(WinkClient.jsonId) ? rowObj.getString(WinkClient.jsonId) : null;
           String rowETag = rowObj.has(WinkClient.jsonRowETag) && !rowObj.isNull(WinkClient.jsonRowETag) ? rowObj.getString(WinkClient.jsonRowETag) : null;
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
