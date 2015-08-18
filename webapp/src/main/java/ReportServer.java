import model.serialization.RowsData;
import model.serialization.TableInfo;
import net.RESTClient;
import org.json.JSONException;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.freemarker.FreeMarkerRoute;
import utils.SpreedSheetBuilder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;

public class ReportServer {
    private static final String LOCAL_URL = "http://localhost:4567";
    private static final String ERROR = "An error occurred when trying to open browser ERROR: ";
    private static final String DIR_TO_SAVE_TO = "me-report";
    static RESTClient sClient = new RESTClient();
    static SpreedSheetBuilder sBuilder;
    static TableInfo sInfo;
    static RowsData sRows;

    // Global UI Hooks
    static JTextArea sTextArea = new JTextArea();
    static JTextField sAggregateAddressText = new JTextField();
    static JTextField sAppIdText = new JTextField();
    static JTextField sTableIdText = new JTextField();

    // Server data
    private static String sAggregateAddress;
    private static String sAppId;
    private static String sTableId;

    public static void main(String[] args) {
        buildJFrame();

        sClient.setOutputText(sTextArea);

        get(new FreeMarkerRoute("/") {
            @Override
            public ModelAndView handle(Request request, Response response) {
                downloadData();
                Map<String, Object> viewObjects = new HashMap<String, Object>();
                viewObjects.put("templateName", "spreedsheet.ftl");
                viewObjects.put("spreedsheet", sBuilder.buildSpreedSheet());

                return modelAndView(viewObjects, "layout.ftl");
            }
        });
    }

    private static void downloadData() {
        try {
            sInfo = sClient.getTableResource();
            sRows = sClient.getAllDataRows(sInfo.getSchemaTag());
            sInfo.setRowList(sRows.getRows());
            sBuilder = new SpreedSheetBuilder(sInfo);
        } catch (IOException e) {
            sTextArea.append(ERROR + e.getMessage() + "\n");
        } catch (JSONException ex) {
            sTextArea.append(ERROR + ex.getMessage() + "\n");
        }
    }

    private static void buildJFrame() {
        final JFrame frame = new JFrame("M&E Report");

        // UI Containter Panel
        JPanel contentPanel = new JPanel(new GridLayout(3, 1));
        contentPanel.setSize(600, 450);

        // Build the UI segments
        JPanel inputPanel = new JPanel(new GridLayout(3,1));
        buildInputArea(inputPanel);
        inputPanel.setSize(600, 100);
        contentPanel.add(inputPanel, 0);

        JPanel buttonsPanel = new JPanel(new GridLayout(7, 2));
        buildButtonArea(frame, buttonsPanel);
        buttonsPanel.setSize(600, 150);
        contentPanel.add(buttonsPanel, 1);

        JPanel textPanel = new JPanel(new GridLayout(1, 1));
        buildTextArea(textPanel);
        textPanel.setSize(600, 200);
        contentPanel.add(textPanel, 2);

        // Finish building the frame
        frame.add(contentPanel);
        frame.setSize(600, 450);
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
                super.windowClosing(e);
            }
        });
        frame.setVisible(true);
    }

    private static void buildTextArea(JPanel textPanel) {
        JScrollPane scroll = new JScrollPane(sTextArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        // TODO:
        //sTextArea.append("M&E report is available\n at " + LOCAL_URL);
        textPanel.add(scroll);

        sTextArea.setSize(400, 200);
    }

    private static void buildInputArea(JPanel inputPanel) {
        JLabel aggregateAddressLabel = new JLabel("Aggregate Address");
        sAggregateAddressText.setText("https://vraggregate2.appspot.com/");
        sAggregateAddressText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        inputPanel.add(aggregateAddressLabel);
        inputPanel.add(sAggregateAddressText);

        JLabel appIdLabel = new JLabel("App ID");
        sAppIdText.setText("tables");
        sAppIdText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        inputPanel.add(appIdLabel);
        inputPanel.add(sAppIdText);

        JLabel tableIdLabel = new JLabel("Table ID");
        sTableIdText.setText("scan_MNH_Register1");
        sTableIdText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        inputPanel.add(tableIdLabel);
        inputPanel.add(sTableIdText);
    }

    private static void buildButtonArea(final JFrame frame, JPanel buttonsPanel) {
        // Define buttons
        final JLabel resetLabel = new JLabel("Select Server");
        final JButton resetButton = new JButton();
        final JLabel serverLabel = new JLabel("Stop Server/Quit");
        final JButton stopServerButton = new JButton();
        final JLabel goToReportLabel = new JLabel("Show Report");
        final JButton showReportButton = new JButton();
        final JLabel downloadDefinitionsLabel = new JLabel("Download Definitions");
        final JButton downloadDefinitionsButton = new JButton();
        final JLabel downloadRawCSVLabel = new JLabel("Download Raw CSV");
        final JButton downloadRawCSVButton = new JButton();
        final JLabel downloadFormattedCSVLabel = new JLabel("Download Formatted CSV");
        final JButton downloadFormattedCSVButton = new JButton();
        final JLabel downloadAttachmentsLabel = new JLabel("Download Attachments");
        final JButton downloadAttachmentsButton = new JButton();

        // Grab the server address and make a new connection. Clear out old data.
        resetButton.setText("Select");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    sAggregateAddress = sAggregateAddressText.getText().trim();
                    sAppId = sAppIdText.getText().trim();
                    sTableId = sTableIdText.getText().trim();

                    sClient.resetData(sAggregateAddress, sAppId, sTableId, DIR_TO_SAVE_TO);

                    sTextArea.append("\nAggregate URL set to: " + sAggregateAddress);
                    sTextArea.append("\nApp ID set to: " + sAppId);
                    sTextArea.append("\nTable ID set to: " + sTableId + "\n");

                    stopServerButton.setEnabled(true);
                    showReportButton.setEnabled(true);
                    downloadDefinitionsButton.setEnabled(true);
                    downloadRawCSVButton.setEnabled(true);
                    downloadFormattedCSVButton.setEnabled(true);
                    downloadAttachmentsButton.setEnabled(true);

                    resetLabel.setText("Switch/Refresh Server & Delete old data");
                    resetButton.setText("Refresh");

                } catch (Exception exc) {
                    sTextArea.append("\nError when trying to download data: ERROR " + exc.getMessage() + "\n");
                }
            }
        });
        buttonsPanel.add(resetLabel);
        buttonsPanel.add(resetButton);

        // Close visual report and shut down
        stopServerButton.setText("Stop");
        stopServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
                System.exit(0);
            }
        });
        buttonsPanel.add(serverLabel);
        buttonsPanel.add(stopServerButton);
        stopServerButton.setEnabled(false);

        // Launch visual report
        showReportButton.setText("Show");
        showReportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    openBrowser();
                } catch (IOException ioe) {
                    sTextArea.append(ERROR + ioe.getMessage() + "\n");
                } catch (URISyntaxException use) {
                    sTextArea.append(ERROR + use.getMessage() + "\n");
                }
            }
        });
        buttonsPanel.add(goToReportLabel);
        buttonsPanel.add(showReportButton);
        showReportButton.setEnabled(false);

        // Download the table definitions files
        downloadDefinitionsButton.setText("Download");
        downloadDefinitionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    sClient.downloadDefinitions(DIR_TO_SAVE_TO);
                } catch (Exception exc) {
                    sTextArea.append("\nError when trying to download data: ERROR " + exc.getMessage() + "\n");
                }
            }
        });
        buttonsPanel.add(downloadDefinitionsLabel);
        buttonsPanel.add(downloadDefinitionsButton);
        downloadDefinitionsButton.setEnabled(false);

        // Download the unaltered CSV file of the table data
        downloadRawCSVButton.setText("Download");
        downloadRawCSVButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    sClient.downloadRawCSV(DIR_TO_SAVE_TO);
                } catch (Exception exc) {
                    sTextArea.append("\nError when trying to download data: ERROR " + exc.getMessage() + "\n");
                }
            }
        });
        buttonsPanel.add(downloadRawCSVLabel);
        buttonsPanel.add(downloadRawCSVButton);
        downloadRawCSVButton.setEnabled(false);

        // Download the CSV and then format it for Excel for easier consumption
        downloadFormattedCSVButton.setText("Download");
        downloadFormattedCSVButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    sClient.downloadFormattedCSV(DIR_TO_SAVE_TO);
                } catch (Exception exc) {
                    sTextArea.append("\nError when trying to download data: ERROR " + exc.getMessage() + "\n");
                }
            }
        });
        buttonsPanel.add(downloadFormattedCSVLabel);
        buttonsPanel.add(downloadFormattedCSVButton);
        downloadFormattedCSVButton.setEnabled(false);

        // Download instance files
        downloadAttachmentsButton.setText("Download");
        downloadAttachmentsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    sClient.downloadAttachments(DIR_TO_SAVE_TO);
                } catch (Exception exc) {
                    sTextArea.append("\nError when trying to download data: ERROR " + exc.getMessage() + "\n");
                }
            }
        });
        buttonsPanel.add(downloadAttachmentsLabel);
        buttonsPanel.add(downloadAttachmentsButton);
        downloadAttachmentsButton.setEnabled(false);
    }

    private static void openBrowser() throws URISyntaxException, IOException {
        switch (determineSystem()) {
            case WIN:
                Desktop.getDesktop().browse(new URI(LOCAL_URL));
                break;
            case MAC:
                Runtime rt = Runtime.getRuntime();
                rt.exec("open " + LOCAL_URL);
                break;
            case LINUX:
                Runtime runtime = Runtime.getRuntime();
                runtime.exec("/usr/bin/firefox -new-window " + LOCAL_URL);
                break;
            default:
                break;
        }
    }

    private static OS determineSystem() {
        OS runningOn = null;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("win") >= 0) {
            runningOn = OS.WIN;
        } else if (os.indexOf("mac") >= 0) {
            runningOn = OS.MAC;
        } else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
            runningOn = OS.LINUX;
        }
        return runningOn;
    }

    enum OS {
        WIN, MAC, LINUX
    }
}
