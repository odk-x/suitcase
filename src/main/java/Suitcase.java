import model.AggregateTableInfo;
import net.RESTClient;
import utils.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;

public class Suitcase {
    // Global UI Hooks
    private JTextArea sTextArea = new JTextArea();
    private JTextField sAggregateAddressText = new JTextField();
    private JTextField sAppIdText = new JTextField();
    private JTextField sTableIdText = new JTextField();

    // Server data
    private AggregateTableInfo table;
    private RESTClient restClient;

    public static void main(String[] args) {
        Suitcase rs = new Suitcase();
        rs.start();
    }

    private void start() {
        buildJFrame();
    }

    private void buildJFrame() {
        final JFrame frame = new JFrame("ODK Suitcase");

        // UI Container Panel
        JPanel contentPanel = new JPanel(new GridLayout(3, 1));
        contentPanel.setSize(600, 450);

        // Build the UI segments
        JPanel inputPanel = new JPanel(new GridLayout(3,1));
        buildInputArea(inputPanel);
        inputPanel.setSize(600, 100);
        contentPanel.add(inputPanel, 0);

        JPanel buttonsPanel = new JPanel(new GridLayout(3, 2));
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

    private void buildTextArea(JPanel textPanel) {
        JScrollPane scroll = new JScrollPane(sTextArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        textPanel.add(scroll);

        sTextArea.setSize(400, 200);
    }

    private void buildInputArea(JPanel inputPanel) {
        JLabel aggregateAddressLabel = new JLabel("Aggregate Address");
        sAggregateAddressText.setText("https://odk-test-area.appspot.com/");
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

    private void buildButtonArea(final JFrame frame, JPanel buttonsPanel) {
        //TODO: Check if server info changed

        // Define buttons
        final JLabel downloadDataCSVLabel = new JLabel("Download Data CSV");
        final JButton downloadDataCSVButton = new JButton();
        final JLabel downloadLinkCSVLabel = new JLabel("Download Link CSV");
        final JButton downloadLinkCSVButton = new JButton();
        final JLabel scanFormattingLabel = new JLabel("Apply Scan Formatting?");
        final JCheckBox scanFormattingCheckbox = new JCheckBox();

        downloadDataCSVButton.setText("Download");
        downloadDataCSVButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    updateAggregateTableInfo();
                    FileUtils.createInstancesDirectory(table);
                    restClient.writeCSVToFile(scanFormattingCheckbox.isSelected(), true);
                } catch (Exception exc) {
                    sTextArea.append("\nError when trying to download data: ERROR " + exc.getMessage() + "\n");
                }
            }
        });
        buttonsPanel.add(downloadDataCSVLabel);
        buttonsPanel.add(downloadDataCSVButton);

        downloadLinkCSVButton.setText("Download");
        downloadLinkCSVButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    updateAggregateTableInfo();
                    FileUtils.createDiretoryStructure(table);
                    restClient.writeCSVToFile(scanFormattingCheckbox.isSelected(), false);
                } catch (Exception exc) {
                    sTextArea.append("\nError when trying to download data: ERROR " + exc.getMessage() + "\n");
                }
            }
        });
        buttonsPanel.add(downloadLinkCSVLabel);
        buttonsPanel.add(downloadLinkCSVButton);

        buttonsPanel.add(scanFormattingLabel);
        buttonsPanel.add(scanFormattingCheckbox);
    }

    private void updateAggregateTableInfo() throws MalformedURLException {
        AggregateTableInfo table2 = new AggregateTableInfo(
                sAggregateAddressText.getText().trim(),
                sAppIdText.getText().trim(),
                sTableIdText.getText().trim());

        if (this.table == null || !table.equals(table2)) {
            if (this.table == null) {
                sTextArea.append("Initializing...");
            } else {
                sTextArea.append("Aggregate table info changed, initializing...");
            }

            this.table = table2;
            this.restClient = new RESTClient(table);
            this.restClient.setOutputText(sTextArea);

            sTextArea.append("done\n");
        }
    }
}
