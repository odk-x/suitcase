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
    static JTextArea sTextArea = new JTextArea();


    public static void main(String[] args) {
        buildJFrame();

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
        JPanel contentPanel = new JPanel(new GridLayout(2, 1));
        JPanel buttonsPanel = new JPanel(new GridLayout(7, 2));
        JPanel textPanel = new JPanel(new GridLayout(1, 1));
        JLabel serverLabel = new JLabel("Stop server");
        JButton stopServerButton = new JButton();
        JLabel goToReportLabel = new JLabel("Show Report");
        JButton showReportButton = new JButton();
        JLabel resetLabel = new JLabel("Reset data");
        JButton resetButton = new JButton();
        JLabel downloadDefinitionsLabel = new JLabel("Download Definitions");
        JButton downloadDefinitionsButton = new JButton();
        JLabel downloadRawCSVLabel = new JLabel("Download Raw CSV");
        JButton downloadRawCSVButton = new JButton();
        JLabel downloadFormattedCSVLabel = new JLabel("Download Formatted CSV");
        JButton downloadFormattedCSVButton = new JButton();
        JLabel downloadAttachmentsLabel = new JLabel("Download Attachments");
        JButton downloadAttachmentsButton = new JButton();
        JScrollPane scroll = new JScrollPane(sTextArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        sTextArea.append("M&E report is available\n at " + LOCAL_URL);
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
        stopServerButton.setText("Stop");
        stopServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
                System.exit(0);
            }
        });
        resetButton.setText("Reset");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    sClient.resetData(DIR_TO_SAVE_TO);
                } catch (Exception exc) {
                    sTextArea.append("\nError when trying to download data: ERROR " + exc.getMessage() + "\n");
                }
            }
        });
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
        buttonsPanel.add(goToReportLabel);
        buttonsPanel.add(showReportButton);
        buttonsPanel.add(serverLabel);
        buttonsPanel.add(stopServerButton);
        buttonsPanel.add(downloadDefinitionsLabel);
        buttonsPanel.add(downloadDefinitionsButton);
        buttonsPanel.add(downloadRawCSVLabel);
        buttonsPanel.add(downloadRawCSVButton);
        buttonsPanel.add(downloadFormattedCSVLabel);
        buttonsPanel.add(downloadFormattedCSVButton);
        buttonsPanel.add(downloadAttachmentsLabel);
        buttonsPanel.add(downloadAttachmentsButton);
        buttonsPanel.add(resetLabel);
        buttonsPanel.add(resetButton);
        textPanel.add(scroll);
        contentPanel.add(buttonsPanel, 0);
        contentPanel.add(textPanel, 1);
        buttonsPanel.setSize(400, 150);
        sTextArea.setSize(400, 150);
        textPanel.setSize(400, 150);
        contentPanel.setSize(400, 300);
        frame.add(contentPanel);
        frame.setSize(400, 300);
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
