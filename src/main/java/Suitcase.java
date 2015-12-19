import model.AggregateTableInfo;
import net.RESTClient;
import org.apache.wink.json4j.JSONException;
import utils.FileUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Scanner;

public class Suitcase {
    public static void main(String[] args) throws IOException, JSONException {
        Scanner console = new Scanner(System.in);
        String serverAddr = "";
        String appId = "";
        String tableId = "";

        System.out.print("Aggregate Address: ");
//        serverAddr = console.nextLine();
        serverAddr = "https://odk-test-area.appspot.com/";
        System.out.print("App ID: ");
//        appId = console.nextLine();
        appId = "tables";
        System.out.print("Table ID: ");
//        tableId = console.nextLine();
        tableId = "scan_MNH_Register1";

        AggregateTableInfo table = new AggregateTableInfo(serverAddr, appId, tableId);
//        FileUtils.createDiretoryStructure(table);
        FileUtils.createInstancesDirectory(table);
        long start = System.currentTimeMillis();
        RESTClient client = new RESTClient(table);

        boolean end = false;
        do {
            System.out.print("Link or Data? ");
//            String linkOrData = console.nextLine();
            String linkOrData = "link";
            System.out.print("Apply scan formatting? ");
//            String scanFormatting = console.nextLine();
            String scanFormatting = "yes";
            client.writeCSVToFile(scanFormatting.equalsIgnoreCase("yes"), linkOrData.equalsIgnoreCase("data"));
            long endTime = System.currentTimeMillis();

            System.out.println(endTime - start);

            System.out.print("Download another CSV? ");
//            end = console.nextLine().equalsIgnoreCase("yes");
        } while (end);
    }
}
