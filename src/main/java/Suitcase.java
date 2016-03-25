import model.AggregateTableInfo;
import net.RESTClient;
import org.apache.commons.cli.*;
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
import java.util.Scanner;

/**
 * Handles UI of Suitcase
 */
public class Suitcase {
  private static final String APP_NAME = "ODK Suitcase";

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
  private JCheckBox sExtraMetadata;
  private JButton sDownloadButton;
  private boolean isGUI;

  // Server data
  private AggregateTableInfo table;
  private RESTClient restClient;

  private boolean force;
  private Scanner console;

  public static void main(String[] args) {
    Suitcase rs = new Suitcase();
    rs.start(args);
  }

  private Suitcase() {
    this.frame = new JFrame(APP_NAME);
  }

  private void start(String[] args) {
    this.sAggregateAddressText = new JTextField();
    this.sAppIdText = new JTextField();
    this.sTableIdText = new JTextField();
    this.sUserNameText = new JTextField();
    this.sPasswordText = new JPasswordField();
    this.sProgressBar = new JProgressBar();
    this.sDownloadAttachment = new JCheckBox();
    this.sApplyScanFmt = new JCheckBox();
    this.sExtraMetadata = new JCheckBox();
    this.sDownloadButton = new JButton();

    if (args.length == 0) {
      //start GUI when no argument is passed
      isGUI = true;
      buildJFrame();
    } else {
      isGUI = false;
      CommandLineParser parser = new DefaultParser();

      Options options = new Options();
      //aggregate related
      options.addOption("aggregate_url", true, "url to Aggregate server");
      options.addOption("app_id", true, "app id");
      options.addOption("table_id", true, "table id");
      options.addOption("username", true, "username");
      options.addOption("password", true, "password");

      //flags
      options.addOption("a", "attachment", false, "download attachments");
      options.addOption("s", "scan", false, "apply Scan formatting");
      options.addOption("f", "force", false, "do not prompt, overwrite existing files");
      options.addOption("e", "extra", false, "add extra metadata columns");

      //misc
      options.addOption("h", "help", false, "print this message");
      options.addOption("v", "version", false, "prints version information");

      try {
        CommandLine line = parser.parse(options, args);

        if (line.hasOption('h')) {
          HelpFormatter hf = new HelpFormatter();
          hf.printHelp("suitcase", options);
          return;
        }

        if (line.hasOption('v')) {
          System.out.println("ODK Suitcase 2.0");
          return;
        }

        //Aggregate related
        sAggregateAddressText.setText(line.getOptionValue("aggregate_url"));
        sAppIdText.setText(line.getOptionValue("app_id"));
        sTableIdText.setText(line.getOptionValue("table_id"));
        sUserNameText.setText(line.getOptionValue("username"));
        sPasswordText.setText(line.getOptionValue("password"));

        //CSV options
        sDownloadAttachment.setSelected(line.hasOption("a"));
        sApplyScanFmt.setSelected(line.hasOption("s"));
        sExtraMetadata.setSelected(line.hasOption("e"));

        this.force = line.hasOption("f");
      } catch (ParseException e) {
        e.printStackTrace();
      }

      console = new Scanner(System.in);
      if (checkState()) {
        download();
      }
    }
  }

  private void buildJFrame() {
    // UI Container Panel
    JPanel contentPanel = new JPanel(new GridLayout(5, 1));

    // Build the UI segments
    JPanel authPanel = new JPanel(new GridLayout(2, 1));
    buildAuthArea(authPanel);
    contentPanel.add(authPanel);

    JPanel inputPanel = new JPanel(new GridLayout(3, 1));
    buildInputArea(inputPanel);
    contentPanel.add(inputPanel);

    JPanel buttonPanel = new JPanel(new GridLayout(1, 1));
    buildButtonArea(buttonPanel);
    contentPanel.add(buttonPanel);

    JPanel checkboxesPanel = new JPanel(new GridLayout(1, 3));
    buildCheckboxArea(checkboxesPanel);
    contentPanel.add(checkboxesPanel);

    JPanel progressBarPanel = new JPanel(new GridLayout(1, 1));
    buildProgressBarArea(progressBarPanel);
    contentPanel.add(progressBarPanel);

    // Finish building the frame
    frame.add(contentPanel);
    frame.setSize(700, 450);
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
    sTableIdText.setText("scan_TB03_Register1");
    sTableIdText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    inputPanel.add(tableIdLabel);
    inputPanel.add(sTableIdText);
  }

  private void buildAuthArea(JPanel authPanel) {
    JLabel userNameLabel = new JLabel("Username");
    sUserNameText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    authPanel.add(userNameLabel);
    authPanel.add(sUserNameText);

    JLabel passwordLabel = new JLabel("Password");
    sPasswordText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    authPanel.add(passwordLabel);
    authPanel.add(sPasswordText);
  }


  private void buildCheckboxArea(JPanel checkboxPanel) {
    checkboxPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

    sDownloadAttachment.setText("Download attachments?");
    sDownloadAttachment.setHorizontalAlignment(JCheckBox.CENTER);
    checkboxPanel.add(sDownloadAttachment);

    sApplyScanFmt.setText("Apply Scan formatting?");
    sApplyScanFmt.setHorizontalAlignment(JCheckBox.CENTER);
    checkboxPanel.add(sApplyScanFmt);

    sExtraMetadata.setText("Extra metadata columns?");
    sExtraMetadata.setHorizontalAlignment(JCheckBox.CENTER);
    checkboxPanel.add(sExtraMetadata);
  }

  private void buildButtonArea(JPanel buttonsPanel) {
    buttonsPanel.setBorder(new EmptyBorder(40, 150, 0, 150));

    // Define buttons
    sDownloadButton.setText("Download");
    sDownloadButton.setHorizontalAlignment(JButton.CENTER);

    sDownloadButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (checkState()) {
          sDownloadButton.setEnabled(false);
          sDownloadButton.setText("Downloading...");
          sProgressBar.setIndeterminate(true);

          new Thread(new Runnable() {
            @Override
            public void run() {
              download();
            }
          }).start();
        }
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
      boolean delete = promptConfirm("Data from a previous session detected. "
          + "Delete existing data and download data from Aggregate server?");

      if (delete) {
        try {
          FileUtils.deleteTableData(table2);
        } catch (IOException e) {
          e.printStackTrace();
          showError("Unable to delete data.");
        }
      }
    }

    if (FileUtils.isDownloaded(table2, sApplyScanFmt.isSelected(), sDownloadAttachment.isSelected(),
        sExtraMetadata.isSelected())) {
      boolean delete = promptConfirm("This CSV has been downloaded. "
          + "Delete existing CSV and download data from Aggregate server?");
      if (delete) {
        try {
          Files.delete(FileUtils.getCSVPath(table2, sApplyScanFmt.isSelected(),
              sDownloadAttachment.isSelected(), sExtraMetadata.isSelected()));
        } catch (IOException e) {
          e.printStackTrace();
          showError("Unable to delete CSV");
        }
      }
    }
  }

  private void download() {
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
          .writeCSVToFile(sApplyScanFmt.isSelected(), sDownloadAttachment.isSelected(),
              sExtraMetadata.isSelected());

      if (isGUI)
        sProgressBar.setString("Done!");
      else
        System.out.println("Done!");
    } catch (MalformedURLException e) {
      e.printStackTrace();
      showError("Aggregate address is invalid.");
    } catch (IOException e) {
      e.printStackTrace();
      showError("Unable to write file.");
    } catch (ClientWebException e) {
      e.printStackTrace();
      showError("Error occurred.");
    } catch (IllegalArgumentException e) {
      // Note: authentication error is also caught here
      e.printStackTrace();
      showError("Aggregate address, App ID, Table ID, user name, or password is invalid. "
          + "Please check your credentials.");
    } // Add authentication error
    catch (Exception e) {
      e.printStackTrace();
      showError("Error occurred.");
    }
    finally {
      if (isGUI) {
        sProgressBar.setValue(sProgressBar.getMaximum());
        sProgressBar.setIndeterminate(false);
        sDownloadButton.setEnabled(true);
        sDownloadButton.setText("Download");
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
      showError(errorMsgBuilder.toString().trim());
    }

    return state;
  }

  /**
   * Displays error message.
   *
   * When ran as GUI,
   * displays a pop up with an error message.
   * Progress bar is set to non-indeterminate and string set to "error."
   *
   * @param errMsg Error message to display
   */
  private void showError(String errMsg) {
    if (isGUI)
      showErrorPopup(errMsg);
    else
      System.out.println("Error: " + errMsg);
  }

  private void showErrorPopup(String errMsg) {
    sProgressBar.setIndeterminate(false);
    sProgressBar.setString("error");
    JOptionPane.showConfirmDialog(frame, errMsg, "Error", JOptionPane.DEFAULT_OPTION,
        JOptionPane.ERROR_MESSAGE);
  }

  private boolean promptConfirm(String msg) {
    if (force)
      return true;

    if (isGUI) {
      return JOptionPane.YES_OPTION ==
          JOptionPane.showConfirmDialog(frame, msg, APP_NAME, JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE);
    } else {
      System.out.print(msg + " yes / no ");
      return console.nextLine().toLowerCase().startsWith("y");
    }
  }
}
