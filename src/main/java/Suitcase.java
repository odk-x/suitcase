import model.AggregateTableInfo;
import net.RESTClient;
import org.apache.wink.client.ClientWebException;
import utils.FileUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;

/**
 * Handles UI of Suitcase
 */
public class Suitcase {
  // Global UI Hooks
  private final JFrame frame;
  private JTextField sAggregateAddressText;
  private JTextField sAppIdText;
  private JTextField sTableIdText;
  private JTextField sUserNameText;
  private JPasswordField sPasswordText;
  private JProgressBar sProgressBar;
  private JCheckBox sDownloadAttachment;
  private JCheckBox sApplyScanFmt;
  private JButton sDownloadButton;

  // Server data
  private AggregateTableInfo table;
  private RESTClient restClient;

  public static void main(String[] args) {
    Suitcase rs = new Suitcase();
    rs.start();
  }

  private Suitcase() {
    this.frame = new JFrame("ODK Suitcase");
  }

  private void start() {
    this.sAggregateAddressText = new JTextField();
    this.sAppIdText = new JTextField();
    this.sTableIdText = new JTextField();
    this.sUserNameText = new JTextField();
    this.sPasswordText = new JPasswordField();
    this.sProgressBar = new JProgressBar();
    this.sDownloadAttachment = new JCheckBox();
    this.sApplyScanFmt = new JCheckBox();
    this.sDownloadButton = new JButton();

    buildJFrame();
  }

  private void buildJFrame() {
    // UI Container Panel
    JPanel contentPanel = new JPanel(new GridLayout(4, 1));

    // Build the UI segments
    JPanel inputPanel = new JPanel(new GridLayout(3, 1));
    buildInputArea(inputPanel);
    contentPanel.add(inputPanel);

    JPanel buttonPanel = new JPanel(new GridLayout(1, 1));
    buildButtonArea(buttonPanel);
    contentPanel.add(buttonPanel);

    JPanel checkboxesPanel = new JPanel(new GridLayout(1, 2));
    buildCheckboxArea(checkboxesPanel);
    contentPanel.add(checkboxesPanel);

    JPanel progressBarPanel = new JPanel(new GridLayout(1, 1));
    buildProgressBarArea(progressBarPanel);
    contentPanel.add(progressBarPanel);

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

  private void buildProgressBarArea(JPanel pbPanel) {
    sProgressBar.setMinimum(0);
    sProgressBar.setMaximum(100);
    sProgressBar.setValue(100);
    sProgressBar.setString("");
    sProgressBar.setStringPainted(true);

    pbPanel.add(sProgressBar);
  }

  private void buildInputArea(JPanel inputPanel) {
    JLabel aggregateAddressLabel = new JLabel("Aggregate Address");
    sAggregateAddressText.setText("http://52.32.12.103/");
    sAggregateAddressText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    inputPanel.add(aggregateAddressLabel);
    inputPanel.add(sAggregateAddressText);

    JLabel appIdLabel = new JLabel("App ID");
    sAppIdText.setText("tables");
    sAppIdText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    inputPanel.add(appIdLabel);
    inputPanel.add(sAppIdText);

    JLabel tableIdLabel = new JLabel("Table ID");
    sTableIdText.setText("scan_TB03_Register");
    sTableIdText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    inputPanel.add(tableIdLabel);
    inputPanel.add(sTableIdText);
    
    // Username and password input
    JLabel userNameLabel = new JLabel("User name:");
    inputPanel.add(userNameLabel);
    sUserNameText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    inputPanel.add(sUserNameText);
    
    JLabel passwordLabel = new JLabel("Password:");
    sPasswordText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    inputPanel.add(passwordLabel);
    inputPanel.add(sPasswordText);
    
  }

  private void buildCheckboxArea(JPanel checkboxPanel) {
    checkboxPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

    sDownloadAttachment.setText("Download attachments?");
    sDownloadAttachment.setHorizontalAlignment(JCheckBox.CENTER);
    checkboxPanel.add(sDownloadAttachment);

    sApplyScanFmt.setText("Apply Scan formatting?");
    sApplyScanFmt.setHorizontalAlignment(JCheckBox.CENTER);
    checkboxPanel.add(sApplyScanFmt);
  }

  private void buildButtonArea(JPanel buttonsPanel) {
    buttonsPanel.setBorder(new EmptyBorder(40, 150, 0, 150));

    // Define buttons
    sDownloadButton.setText("Download");
    sDownloadButton.setHorizontalAlignment(JButton.CENTER);

    sDownloadButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!checkState()) {
          return;
        }

        sDownloadButton.setEnabled(false);
        sDownloadButton.setText("Downloading...");
        sProgressBar.setIndeterminate(true);

        new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              updateAggregateTableInfo();
              if (table.getSchemaETag() == null || table.getSchemaETag().isEmpty()) {
                throw new IllegalArgumentException(
                    "Unable to retrieve SchemaETag with given table info");
              }

              if (sDownloadAttachment.isSelected()) {
                FileUtils.createInstancesDirectory(table);
              } else {
                FileUtils.createBaseDirectory(table);
              }

              restClient
                  .writeCSVToFile(sApplyScanFmt.isSelected(), sDownloadAttachment.isSelected());
              sProgressBar.setString("Done");
            } catch (MalformedURLException e) {
              e.printStackTrace();
              showErrPopup("Aggregate address is invalid.");
            } catch (IOException e) {
              e.printStackTrace();
              showErrPopup("Unable to write file.");
            } catch (ClientWebException e) {
              e.printStackTrace();
              showErrPopup("Error occurred.");
            } catch (IllegalArgumentException e) {
              // Note: authentication error is also caught here
              e.printStackTrace();
              showErrPopup("Aggregate address, App ID, Table ID, user name, or password is invalid. Please check your credentials.");
            } // Add authentication error
            catch (Exception e) {
              e.printStackTrace();
              showErrPopup("Error occurred.");
            }
            finally {
              sProgressBar.setValue(sProgressBar.getMaximum());
              sProgressBar.setIndeterminate(false);
              sDownloadButton.setEnabled(true);
              sDownloadButton.setText("Download");
            }
          }
        }).start();
      }
    });

    buttonsPanel.add(sDownloadButton);
  }

  /**
   * Checks whether table info (address, app id, table id) has changed, and update internal state
   * if needed.
   * Initializes internal state on the first run.
   *
   * @throws MalformedURLException
   */
  private void updateAggregateTableInfo() throws MalformedURLException {
    AggregateTableInfo table2 = new AggregateTableInfo(
        sAggregateAddressText.getText().trim(),
        sAppIdText.getText().trim(),
        sTableIdText.getText().trim(),
        sUserNameText.getText().trim(),
        String.valueOf(sPasswordText.getPassword()).trim()
    );

    boolean firstRun = (this.table == null);

    if (firstRun || !table.equals(table2)) {
      if (firstRun) {
        sProgressBar.setString("Initializing");
      } else {
        sProgressBar.setString("Aggregate table info changed, initializing");
      }

      this.table = table2;
      this.restClient = new RESTClient(table);
      this.restClient.setProgressBar(this.sProgressBar);
    }

    if (firstRun && FileUtils.isDownloaded(table2)) {
      int delete = JOptionPane
          .showConfirmDialog(frame,
              "Data from a previous session detected. "
              + "Delete existing data and download data from Aggregate server?", "ODK Suitcase",
              JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

      if (delete == JOptionPane.YES_OPTION) {
        try {
          FileUtils.deleteTableData(table2);
        } catch (IOException e) {
          e.printStackTrace();
          showErrPopup("Unable to delete data.");
        }
      }
    }

    if (FileUtils.isDownloaded(table2,
        sApplyScanFmt.isSelected(), sDownloadAttachment.isSelected())) {
      int delete = JOptionPane.showConfirmDialog(frame,
          "This CSV has been downloaded. "
          + "Delete existing CSV and download data from Aggregate server?", "ODK Suitcase",
          JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

      if (delete == JOptionPane.YES_OPTION) {
        try {
          Files.delete(FileUtils.getCSVPath(table2, sApplyScanFmt.isSelected(),
              sDownloadAttachment.isSelected()));
        } catch (IOException e) {
          e.printStackTrace();
          showErrPopup("Unable to delete CSV");
        }
      }
    }
  }

  /**
   * Checks if aggregate address, App ID, and Table ID supplied are empty.
   * And displays a pop up showing the fields that are empty, if applicable.
   *
   * @return true when information presented has no problem
   */
  private boolean checkState() {
    boolean state = true;
    StringBuilder errorMsgBuilder = new StringBuilder();

    if (sAggregateAddressText.getText().isEmpty()) {
      errorMsgBuilder.append("Aggregate address cannot be empty.\n");
      state = false;
    }

    if (sAppIdText.getText().isEmpty()) {
      errorMsgBuilder.append("App ID cannot be empty.\n");
      state = false;
    }

    if (sTableIdText.getText().isEmpty()) {
      errorMsgBuilder.append("Table ID cannot be empty.\n");
      state = false;
    }

    if (!state) {
      showErrPopup(errorMsgBuilder.toString().trim());
    }

    return state;
  }

  /**
   * Displays a pop up with an error message.
   * Progress bar is set to non-indeterminate and string set to "error."
   *
   * @param errMsg Error message to display
   */
  private void showErrPopup(String errMsg) {
    sProgressBar.setIndeterminate(false);
    sProgressBar.setString("error");
    JOptionPane.showConfirmDialog(frame, errMsg, "Error", JOptionPane.DEFAULT_OPTION,
        JOptionPane.ERROR_MESSAGE);
  }
}
