package net;

import model.AggregateTableInfo;
import model.ODKCsv;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.aggregate.odktables.rest.RFC4180CsvWriter;
import org.opendatakit.wink.client.WinkClient;

import javax.swing.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.opendatakit.wink.client.WinkClient.*;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/19/15
 * Time: 11:27 AM
 */
public class RESTClient {
    private JProgressBar pb;

    private WinkClient odkWinkClient;
    private String csvPath;

    private AggregateTableInfo tableInfo;
    private AttachmentManager attMngr;
    private ODKCsv csv;

    public static final String CSV_NAME = "data.csv";
    private static final int FETCH_LIMIT = 1000;

    //!!!ATTENTION!!! One per table
    public RESTClient(AggregateTableInfo tableInfo) {
        this.tableInfo = tableInfo;

        tableInfo.setSchemaETag(WinkClient.getSchemaETagForTable(
                this.tableInfo.getServerUrl(),
                this.tableInfo.getAppId(),
                this.tableInfo.getTableId()));

        this.odkWinkClient = new WinkClient();
        this.attMngr = new AttachmentManager(this.tableInfo, this.odkWinkClient);
        this.csv = new ODKCsv(this.attMngr, this.tableInfo);
    }

    public void writeCSVToFile(final boolean scanFormatting, final boolean localLink) throws IOException, JSONException, ExecutionException, InterruptedException {
        if (this.csv.getSize() == 0) {
            //Download json if not downloaded
            retrieveRows(FETCH_LIMIT);
        }

        this.pb.setIndeterminate(false);
        this.pb.setString("Processing and writing data");

        RFC4180CsvWriter csvWriter = new RFC4180CsvWriter(new FileWriter(
                utils.FileUtils.getCSVPath(this.tableInfo, scanFormatting, localLink).toAbsolutePath().toString()
        ));

        ODKCsv.ODKCSVIterator csvIt = this.csv.getODKCSVIterator();
        csvWriter.writeNext(this.csv.getHeader(scanFormatting));
        while (csvIt.hasNext()) {
            this.pb.setValue((int)((double) csvIt.getIndex() / this.csv.getSize() * this.pb.getMaximum()));
            String[] nextline = csvIt.next(scanFormatting, localLink);
//            System.out.println("RESTClient: Index is " + i++);
//            System.out.println("RESTClient: nextline size is " + nextline.length);
//            System.out.println("RESTClient: First item is " + nextline[0]);
            csvWriter.writeNext(nextline);
        }
        csvWriter.close();

/*        Map<Integer, String[]> csvRows = new ConcurrentSkipListMap<Integer, String[]>();
        ExecutorService pool = Executors.newFixedThreadPool(10);

        List<Future<String[]>> list = new ArrayList<Future<String[]>>();
        int[] arr = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (final int i : arr) {
            Callable<String[]> callable = new Callable<String[]>() {
                @Override
                public String[] call() throws Exception {
                    return csv.get(i, scanFormatting, localLink);
                }
            };
            list.add(pool.submit(callable));
        }
        pool.shutdown();

        for (Future<String[]> f : list) {
            csvWriter.writeNext(f.get());
        }
        csvWriter.close();*/
    }

    private void retrieveRows(int limit) throws JSONException {
        this.pb.setString("Retrieving rows");

        String cursor = null;
        JSONObject rows;

        do {
            rows = this.odkWinkClient.getRows(
                    this.tableInfo.getServerUrl(),
                    this.tableInfo.getAppId(),
                    this.tableInfo.getTableId(),
                    this.tableInfo.getSchemaETag(),
                    cursor, Integer.toString(limit));

            cursor = rows.optString("webSafeResumeCursor");
            this.csv.tryAdd(rows.getJSONArray(jsonRowsString));
        } while (rows.getBoolean("hasMoreResults"));
    }

    public void setProgressBar(JProgressBar pb) {
        this.pb = pb;
    }
}
