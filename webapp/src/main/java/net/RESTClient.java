package net;

import org.apache.commons.io.FileUtils;
import java.io.File;

import org.opendatakit.wink.client.WinkClient;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/19/15
 * Time: 11:27 AM
 */
public class RESTClient {
    public static final String AGGREGATE_URL = "https://vraggregate2.appspot.com/odktables";
    public static final String APP_ID = "tables";
    public static final String TABLE_ID = "scan_MNH_Register1";

    public static String separator = "/";

    private WinkClient mWinkClient;
    private String schemaETag;

    public RESTClient() {
        mWinkClient = new WinkClient();
        schemaETag = WinkClient.getSchemaETagForTable(AGGREGATE_URL, APP_ID, TABLE_ID);
    }

    public void resetData(String dirToSaveDataTo) throws Exception {
        FileUtils.deleteDirectory(new File(dirToSaveDataTo));

        schemaETag = WinkClient.getSchemaETagForTable(AGGREGATE_URL, APP_ID, TABLE_ID);
    }

    public void downloadDefinitions(String dirToSaveDataTo) throws Exception {
        // Get all Table Level Files
        mWinkClient.getAllTableLevelFilesFromUri(AGGREGATE_URL, APP_ID, TABLE_ID, dirToSaveDataTo);

        // Write out Table Definition CSV's
        String tableDefinitionCSVPath = dirToSaveDataTo + separator + "tables" + separator + TABLE_ID
                + separator + "definition.csv";
        mWinkClient.writeTableDefinitionToCSV(AGGREGATE_URL, APP_ID, TABLE_ID, schemaETag, tableDefinitionCSVPath);
    }

    public void downloadData(String dirToSaveDataTo) throws Exception {
        // Write out the Table Data CSV's
        String dataCSVPath = dirToSaveDataTo + separator + "assets" + separator + "csv" + separator
                + TABLE_ID + ".csv";
        mWinkClient.writeRowDataToCSV(AGGREGATE_URL, APP_ID, TABLE_ID, schemaETag, dataCSVPath);
    }

    public void downloadAttachments(String dirToSaveDataTo) throws Exception {
        // Get all Instance Files
        // get all rows - check for attachment
        mWinkClient.getAllTableInstanceFilesFromUri(AGGREGATE_URL, APP_ID, TABLE_ID, schemaETag, dirToSaveDataTo);
    }
}
