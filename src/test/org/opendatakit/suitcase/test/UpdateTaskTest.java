package org.opendatakit.suitcase.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.aggregate.odktables.rest.RFC4180CsvReader;
import org.opendatakit.wink.client.WinkClient;

import org.opendatakit.suitcase.net.UpdateTask;
import org.opendatakit.suitcase.model.AggregateInfo;
import junit.framework.TestCase;

public class UpdateTaskTest extends TestCase{
  
  String serverUrl;
  String appId;
  String absolutePathOfTestFiles;
  String host;
  String username;
  String password;
  int batchSize;
  String version;
  
  /*
   * Perform setup for test if necessary
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    //agg_url = System.getProperty("test.aggUrl");
    //appId = System.getProperty("test.appId");
    //absolutePathOfTestFiles = System.getProperty("test.absolutePathOfTestFiles");
    //batchSize = Integer.valueOf(System.getProperty("test.batchSize"));
    
    serverUrl = "https://test.appspot.com";
    appId = "odktables/default";
    absolutePathOfTestFiles = "testfiles/";
    batchSize = 1000;
    username = "";
    password = "";
    URL url = new URL(serverUrl);
    host = url.getHost();
    version = "2";
  }
  
  public void testUpdateTaskAdd() {

    String suitcaseAppId = "default"; 
    AggregateInfo aggInfo = null;
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String dataPath = absolutePathOfTestFiles + "plot/plot-add.csv";
    String testTableId = "test1";
    String tableSchemaETag = null;
    WinkClient wc = null;

    try {
    aggInfo = new AggregateInfo(serverUrl, suitcaseAppId, username, password);
    wc = new WinkClient();
    wc.init(serverUrl, username, password);

    JSONObject result = wc.createTableWithCSV(serverUrl, appId, testTableId, null, csvFile);
    System.out.println("testUpdateTaskAdd: result is " + result);

    if (result.containsKey("tableId")) {
      String tableId = result.getString("tableId");
      assertEquals(tableId, testTableId);
      tableSchemaETag = result.getString("schemaETag");
    }

    // Get the table definition
    JSONObject tableDef = wc.getTableDefinition(serverUrl, appId, testTableId, tableSchemaETag);

    // Make sure it is the same as the csv definition
    assertTrue(checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

    } catch (Exception e) {
      System.out.println("testUpdateTaskAdd failed during table creation: " + e);
      fail();
      e.printStackTrace();
    }
    
    final CountDownLatch signal = new CountDownLatch(1);
    
    UpdateTask task = null;
    try {
      task = new UpdateTask(aggInfo, dataPath, version, testTableId, false) {
        @Override
        protected void finished() {
          signal.countDown();// notify the count downlatch
        }
      };
      task.execute();
    } catch (Exception e) {
      System.out.println("testUpdateTaskAdd failed with exception: " + e);
      fail();
      e.printStackTrace();
    }
    
    try {
      signal.await();// wait for callback
      task.get();
      
      JSONObject res = wc.getRowsSince(serverUrl, appId, testTableId, tableSchemaETag, null, null,
          null);
      
      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);
      
      JSONObject jsonRow = rows.getJSONObject(0);
      
      // Now check that the row was created with the right rowId
      assertTrue(this.checkThatRowHasId("12", jsonRow));
      
      // Now delete the table
      wc.deleteTableDefinition(serverUrl, appId, testTableId, tableSchemaETag);
      
    } catch (Exception e1) {
      e1.printStackTrace();
      System.out.println("testUpdateTaskAdd failed with exception: " + e1);
      fail();
    }
  }
  
  public void testUpdateTaskDelete() {

    String suitcaseAppId = "default"; 
    AggregateInfo aggInfo = null;
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String dataPathAdd = absolutePathOfTestFiles + "plot/plot-add.csv";
    String dataPathDelete = absolutePathOfTestFiles + "plot/plot-delete.csv";
    String testTableId = "test2";
    String tableSchemaETag = null;
    WinkClient wc = null;

    try {
    aggInfo = new AggregateInfo(serverUrl, suitcaseAppId, username, password);
    wc = new WinkClient();
    wc.init(serverUrl, username, password);

    JSONObject result = wc.createTableWithCSV(serverUrl, appId, testTableId, null, csvFile);
    System.out.println("testUpdateTaskDelete: result is " + result);

    if (result.containsKey("tableId")) {
      String tableId = result.getString("tableId");
      assertEquals(tableId, testTableId);
      tableSchemaETag = result.getString("schemaETag");
    }

    // Get the table definition
    JSONObject tableDef = wc.getTableDefinition(serverUrl, appId, testTableId, tableSchemaETag);

    // Make sure it is the same as the csv definition
    assertTrue(checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

    } catch (Exception e) {
      System.out.println("testUpdateTaskDelete failed during table creation: " + e);
      fail();
      e.printStackTrace();
    }
    
    final CountDownLatch signal = new CountDownLatch(1);
    
    UpdateTask task = null;
    try {
      task = new UpdateTask(aggInfo, dataPathAdd, version, testTableId, false) {
        @Override
        protected void finished() {
          signal.countDown();// notify the count downlatch
        }
      };
      task.execute();
    } catch (Exception e) {
      System.out.println("testUpdateTaskDelete failed with exception: " + e);
      fail();
      e.printStackTrace();
    }
    
    try {
      signal.await();// wait for callback
      task.get();
      
      JSONObject res = wc.getRowsSince(serverUrl, appId, testTableId, tableSchemaETag, null, null,
          null);
      
      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);
      
      JSONObject jsonRow = rows.getJSONObject(0);
      
      // Now check that the row was created with the right rowId
      assertTrue(this.checkThatRowHasId("12", jsonRow));
      
    } catch (Exception e1) {
      e1.printStackTrace();
      System.out.println("testUpdateTaskDelete failed with exception: " + e1);
      fail();
    }
    
    final CountDownLatch deleteSignal = new CountDownLatch(1);
    
    UpdateTask taskDelete = null;
    try {
      taskDelete = new UpdateTask(aggInfo, dataPathDelete, version, testTableId, false) {
        @Override
        protected void finished() {
          deleteSignal.countDown();// notify the count downlatch
        }
      };
      taskDelete.execute();
    } catch (Exception e) {
      System.out.println("testUpdateTaskDelete failed with exception: " + e);
      fail();
      e.printStackTrace();
    }
    
    try {
      deleteSignal.await();// wait for callback
      taskDelete.get();
      
      JSONObject res = wc.getRowsSince(serverUrl, appId, testTableId, tableSchemaETag, null, null,
          null);
      
      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 0);
      
      // Now delete the table
      wc.deleteTableDefinition(serverUrl, appId, testTableId, tableSchemaETag);
      
    } catch (Exception e1) {
      e1.printStackTrace();
      System.out.println("testUpdateTaskDelete failed with exception: " + e1);
      fail();
    }
  }
  
  public void testUpdateTaskUpdate() {
    String suitcaseAppId = "default"; 
    AggregateInfo aggInfo = null;
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String dataPathAdd = absolutePathOfTestFiles + "plot/plot-add.csv";
    String dataPathUpdate = absolutePathOfTestFiles + "plot/plot-update.csv";
    String testTableId = "test3";
    String tableSchemaETag = null;
    WinkClient wc = null;

    try {
    aggInfo = new AggregateInfo(serverUrl, suitcaseAppId, username, password);
    wc = new WinkClient();
    wc.init(serverUrl, username, password);

    JSONObject result = wc.createTableWithCSV(serverUrl, appId, testTableId, null, csvFile);
    System.out.println("testUpdateTaskUpdate: result is " + result);

    if (result.containsKey("tableId")) {
      String tableId = result.getString("tableId");
      assertEquals(tableId, testTableId);
      tableSchemaETag = result.getString("schemaETag");
    }

    // Get the table definition
    JSONObject tableDef = wc.getTableDefinition(serverUrl, appId, testTableId, tableSchemaETag);

    // Make sure it is the same as the csv definition
    assertTrue(checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

    } catch (Exception e) {
      System.out.println("testUpdateTaskUpdate failed during table creation: " + e);
      fail();
      e.printStackTrace();
    }
    
    final CountDownLatch signal = new CountDownLatch(1);
    
    UpdateTask task = null;
    try {
      task = new UpdateTask(aggInfo, dataPathAdd, version, testTableId, false) {
        @Override
        protected void finished() {
          signal.countDown();// notify the count downlatch
        }
      };
      task.execute();
    } catch (Exception e) {
      System.out.println("testUpdateTaskUpdate failed with exception: " + e);
      fail();
      e.printStackTrace();
    }
    
    try {
      signal.await();// wait for callback
      task.get();
      
      JSONObject res = wc.getRowsSince(serverUrl, appId, testTableId, tableSchemaETag, null, null,
          null);
      
      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);
      
      JSONObject jsonRow = rows.getJSONObject(0);
      
      // Check that the row was created with the right rowId
      assertTrue(this.checkThatRowHasId("12", jsonRow));
      
    } catch (Exception e1) {
      e1.printStackTrace();
      System.out.println("testUpdateTaskUpdate failed with exception: " + e1);
      fail();
    }
    
    final CountDownLatch updateSignal = new CountDownLatch(1);
    
    UpdateTask taskUpdate = null;
    try {
      taskUpdate = new UpdateTask(aggInfo, dataPathUpdate, version, testTableId, false) {
        @Override
        protected void finished() {
          updateSignal.countDown();// notify the count downlatch
        }
      };
      taskUpdate.execute();
    } catch (Exception e) {
      System.out.println("testUpdateTaskUpdate failed with exception: " + e);
      fail();
      e.printStackTrace();
    }
    
    try {
      updateSignal.await();// wait for callback
      taskUpdate.get();
      
      JSONObject res = wc.getRowsSince(serverUrl, appId, testTableId, tableSchemaETag, null, null,
          null);
      
      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);
      
      JSONObject jsonRow = rows.getJSONObject(0);
      
      // Check that the row was created with the right rowId
      assertTrue(checkThatRowHasId("12", jsonRow));
     
      assertTrue(checkThatRowHasColumnValue("plot_name", "Clarice", jsonRow));
      
      // Now delete the table
      wc.deleteTableDefinition(serverUrl, appId, testTableId, tableSchemaETag);
      
    } catch (Exception e1) {
      e1.printStackTrace();
      System.out.println("testUpdateTaskUpdate failed with exception: " + e1);
      fail();
    }
  }
  
  public void testUpdateTaskForceUpdate() {
    String suitcaseAppId = "default"; 
    AggregateInfo aggInfo = null;
    String csvFile = absolutePathOfTestFiles + "plot/definition.csv";
    String dataPathAdd = absolutePathOfTestFiles + "plot/plot-add.csv";
    String dataPathUpdate = absolutePathOfTestFiles + "plot/plot-forceUpdate.csv";
    String testTableId = "test4";
    String tableSchemaETag = null;
    WinkClient wc = null;

    try {
    aggInfo = new AggregateInfo(serverUrl, suitcaseAppId, username, password);
    wc = new WinkClient();
    wc.init(serverUrl, username, password);

    JSONObject result = wc.createTableWithCSV(serverUrl, appId, testTableId, null, csvFile);
    System.out.println("testUpdateTaskForceUpdate: result is " + result);

    if (result.containsKey("tableId")) {
      String tableId = result.getString("tableId");
      assertEquals(tableId, testTableId);
      tableSchemaETag = result.getString("schemaETag");
    }

    // Get the table definition
    JSONObject tableDef = wc.getTableDefinition(serverUrl, appId, testTableId, tableSchemaETag);

    // Make sure it is the same as the csv definition
    assertTrue(checkThatTableDefAndCSVDefAreEqual(csvFile, tableDef));

    } catch (Exception e) {
      System.out.println("testUpdateTaskForceUpdate failed during table creation: " + e);
      fail();
      e.printStackTrace();
    }
    
    final CountDownLatch signal = new CountDownLatch(1);
    
    UpdateTask task = null;
    try {
      task = new UpdateTask(aggInfo, dataPathAdd, version, testTableId, false) {
        @Override
        protected void finished() {
          signal.countDown();// notify the count downlatch
        }
      };
      task.execute();
    } catch (Exception e) {
      System.out.println("testUpdateTaskForceUpdate failed with exception: " + e);
      fail();
      e.printStackTrace();
    }
    
    try {
      signal.await();// wait for callback
      task.get();
      
      JSONObject res = wc.getRowsSince(serverUrl, appId, testTableId, tableSchemaETag, null, null,
          null);
      
      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);
      
      JSONObject jsonRow = rows.getJSONObject(0);
      
      // Check that the row was created with the right rowId
      assertTrue(this.checkThatRowHasId("12", jsonRow));
      
    } catch (Exception e1) {
      e1.printStackTrace();
      System.out.println("testUpdateTaskForceUpdate failed with exception: " + e1);
      fail();
    }
    
    final CountDownLatch updateSignal = new CountDownLatch(1);
    
    UpdateTask taskUpdate = null;
    try {
      taskUpdate = new UpdateTask(aggInfo, dataPathUpdate, version, testTableId, false) {
        @Override
        protected void finished() {
          updateSignal.countDown();// notify the count downlatch
        }
      };
      taskUpdate.execute();
    } catch (Exception e) {
      System.out.println("testUpdateTaskForceUpdate failed with exception: " + e);
      fail();
      e.printStackTrace();
    }
    
    try {
      updateSignal.await();// wait for callback
      taskUpdate.get();
      
      JSONObject res = wc.getRowsSince(serverUrl, appId, testTableId, tableSchemaETag, null, null,
          null);
      
      JSONArray rows = res.getJSONArray("rows");

      assertEquals(rows.size(), 1);
      
      JSONObject jsonRow = rows.getJSONObject(0);
      
      // Check that the row was created with the right rowId
      assertTrue(checkThatRowHasId("12", jsonRow));
     
      assertTrue(checkThatRowHasColumnValue("plot_name", "Clarice", jsonRow));
      
      // Now delete the table
      wc.deleteTableDefinition(serverUrl, appId, testTableId, tableSchemaETag);
      
    } catch (Exception e1) {
      e1.printStackTrace();
      System.out.println("testUpdateTaskForceUpdate failed with exception: " + e1);
      fail();
    }
  }
  
  public boolean checkThatTableDefAndCSVDefAreEqual(String csvFile, JSONObject tableDef) {
    boolean same = false;
    InputStream in;

    try {
      in = new FileInputStream(new File(csvFile));

      InputStreamReader inputStream = new InputStreamReader(in);
      RFC4180CsvReader reader = new RFC4180CsvReader(inputStream);

      // Skip the first line - it's just headers
      reader.readNext();

      if (tableDef.containsKey("orderedColumns")) {
        JSONArray cols = tableDef.getJSONArray("orderedColumns");
        String[] csvDef;
        while ((csvDef = reader.readNext()) != null) {
          same = false;
          for (int i = 0; i < cols.size(); i++) {
            JSONObject col = cols.getJSONObject(i);
            String testElemKey = col.getString("elementKey");
            if (csvDef[0].equals(testElemKey)) {
              same = true;
              // Remove the index so we don't keep
              // comparing old defs
              cols.remove(i);
              break;
            }
          }

          if (!same) {
            return same;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return same;
  }
  
  public boolean checkThatRowHasId(String RowId, JSONObject rowRes) {
    boolean same = false;

    try {
      if (RowId.equals(rowRes.getString("id"))) {
        same = true;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return same;
  }
  
  public boolean checkThatRowHasColumnValue(String column, String columnValue, JSONObject rowRes) {
    String COLUMN_STRING = "column";
    String VALUE_STRING = "value";
    
    if (column == null || columnValue == null) {
      return false;
    }

    try {
      if (rowRes.has(WinkClient.orderedColumnsDef)) {
        JSONArray ordCols = rowRes.getJSONArray(WinkClient.orderedColumnsDef);
        for (int i = 0; i < ordCols.length(); i++) {
          JSONObject col = ordCols.getJSONObject(i);
          String colStr = col.has(COLUMN_STRING) && !col.isNull(COLUMN_STRING) ? col.getString(COLUMN_STRING) : null;
          if (colStr == null) {
            return false;
          }
          if (colStr.equals(column)) {
            String recVal = col.has(VALUE_STRING) && !col.isNull(VALUE_STRING) ? col.getString(VALUE_STRING) : null;
            if (recVal == null) {
              return false;
            }
            if (recVal.equals(columnValue)) {
              return true;
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
}
