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
import java.io.File;
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
  private JTextField sSavePathText;
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
    frame.setSize(700, 500);
    frame.setLocationRelativeTo(null);
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        System.exit(0);
        super.windowClosing(e);
      }
    });
  }

  private void start(String[] args) {
    this.sAggregateAddressText = new JTextField();
    this.sAppIdText = new JTextField();
    this.sTableIdText = new JTextField();
    this.sSavePathText = new JTextField();
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

      //csv
      options.addOption("a", "attachment", false, "download attachments");
      options.addOption("s", "scan", false, "apply Scan formatting");
      options.addOption("e", "extra", false, "add extra metadata columns");
      options.addOption
          ("o", "output", true, "specify a custom output directory, default is ./Download/");

      //UI
      options.addOption("f", "force", false, "do not prompt, overwrite existing files");

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
        sSavePathText.setText(line.getOptionValue("o"));

        //CSV options
        sDownloadAttachment.setSelected(line.hasOption("a"));
        sApplyScanFmt.setSelected(line.hasOption("s"));
        sExtraMetadata.setSelected(line.hasOption("e"));

        //Misc
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
    JPanel contentPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = getDefaultGbc();
    gbc.gridx = 0;
    gbc.gridy = GridBagConstraints.RELATIVE;

    // Build the UI segments
    JPanel inputPanel = new JPanel(new GridBagLayout());
    buildInputArea(inputPanel);
    gbc.weighty = 40;
    gbc.insets = new Insets(0, 50, 0, 50);
    contentPanel.add(inputPanel, gbc);

    JPanel buttonPanel = new JPanel(new GridLayout(1, 1));
    buildButtonArea(buttonPanel);
    gbc.weighty = 15;
    gbc.insets = new Insets(20, frame.getWidth() / 4, 10, frame.getWidth() / 4);
    contentPanel.add(buttonPanel, gbc);

    JPanel savePathPanel = new JPanel(new GridBagLayout());
    buildSavePathArea(savePathPanel);
    gbc.weighty = 1;
    gbc.insets = new Insets(0, 20, 0, 20);
    contentPanel.add(savePathPanel, gbc);

    JPanel checkBoxPanel = new JPanel(new GridLayout(1, 3));
    buildCheckboxArea(checkBoxPanel);
    gbc.weighty = 10;
    gbc.insets = new Insets(0, 0, 0, 0);
    contentPanel.add(checkBoxPanel, gbc);

    JPanel progressBarPanel = new JPanel(new GridLayout(1, 1));
    buildProgressBarArea(progressBarPanel);
    gbc.weighty = 25;
    gbc.insets = new Insets(0, 0, 0, 0);
    contentPanel.add(progressBarPanel, gbc);

    // Finish building the frame
    frame.add(contentPanel);
    frame.setVisible(true);
  }

  private void buildInputArea(JPanel inputPanel) {
    GridBagConstraints gbc = getDefaultGbc();
    gbc.gridx = GridBagConstraints.RELATIVE;
    gbc.gridy = 0;

    //label area
    JPanel labelPanel = new JPanel(new GridLayout(5, 1));
    gbc.weightx = 0.3;
    inputPanel.add(labelPanel, gbc);

    //text area
    JPanel textPanel = new JPanel(new GridLayout(5, 1));
    gbc.weightx = 0.7;
    inputPanel.add(textPanel, gbc);

    buildServerArea(labelPanel, textPanel);
    buildAuthArea(labelPanel, textPanel);
  }

  private void buildServerArea(JPanel serverLabelPanel, JPanel serverTextPanel) {
    //labels
    serverLabelPanel.add(new JLabel("Aggregate Address"));
    serverLabelPanel.add(new JLabel("App ID"));
    serverLabelPanel.add(new JLabel("Table ID"));

    //text fields
    sAggregateAddressText.setText("http://52.32.12.103/");
    sAggregateAddressText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    serverTextPanel.add(sAggregateAddressText);

    sAppIdText.setText("tables");
    sAppIdText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    serverTextPanel.add(sAppIdText);

    sTableIdText.setText("scan_TB03_Register1");
    sTableIdText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    serverTextPanel.add(sTableIdText);
  }

  private void buildAuthArea(JPanel authLabelPanel, JPanel authTextPanel) {
    //labels
    authLabelPanel.add(new JLabel("Username"));
    authLabelPanel.add(new JLabel("Password"));

    //text fields
    sUserNameText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    authTextPanel.add(sUserNameText);

    sPasswordText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    authTextPanel.add(sPasswordText);
  }

  private void buildButtonArea(JPanel buttonsPanel) {
    // Define buttons
    sDownloadButton.setText("Download");
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

  private void buildSavePathArea(JPanel savePathPanel) {
    GridBagConstraints gbc = getDefaultGbc();
    gbc.gridx = GridBagConstraints.RELATIVE;
    gbc.gridy = 0;

    //label
    JLabel savePathLabel = new JLabel("Save to");
    savePathLabel.setHorizontalAlignment(JLabel.CENTER);
    gbc.weightx = 5;
    savePathPanel.add(savePathLabel, gbc);

    //text field
    sSavePathText.setText(FileUtils.getDefaultSavePath().toAbsolutePath().toString());
    sSavePathText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    gbc.weightx = 90;
    savePathPanel.add(sSavePathText, gbc);

    //button
    JButton dirButton = new JButton();
    dirButton.setText("...");
    dirButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(new File(sSavePathText.getText()));

        int result = chooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
          //TODO: check for path validity and respond accordingly
          sSavePathText.setText(chooser.getSelectedFile().toString());
        }
      }
    });
    gbc.weightx = 5;
    savePathPanel.add(dirButton, gbc);
  }

  private void buildCheckboxArea(JPanel checkboxPanel) {
    sDownloadAttachment.setText("Download attachments?");
    sDownloadAttachment.setHorizontalAlignment(JCheckBox.CENTER);
    sDownloadAttachment.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));
    checkboxPanel.add(sDownloadAttachment);

    sApplyScanFmt.setText("Apply Scan formatting?");
    sApplyScanFmt.setHorizontalAlignment(JCheckBox.CENTER);
    sApplyScanFmt.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));
    checkboxPanel.add(sApplyScanFmt);

    sExtraMetadata.setText("Extra metadata columns?");
    sExtraMetadata.setHorizontalAlignment(JCheckBox.CENTER);
    sExtraMetadata.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));
    checkboxPanel.add(sExtraMetadata);
  }

  private void buildProgressBarArea(JPanel pbPanel) {
    sProgressBar.setMinimum(0);
    sProgressBar.setMaximum(100);
    sProgressBar.setValue(100);
    sProgressBar.setString("Idle");
    sProgressBar.setStringPainted(true);

    pbPanel.add(sProgressBar);
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
        sAggregateAddressText.getText(),
        sAppIdText.getText(),
        sTableIdText.getText(),
        sUserNameText.getText(),
        String.valueOf(sPasswordText.getPassword())
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
      this.restClient.setSavePath(this.sSavePathText.getText());
    }

    if (firstRun && FileUtils.isDownloaded(table2, sSavePathText.getText())) {
      boolean delete = promptConfirm("Data from a previous session detected. "
          + "Delete existing data and download data from Aggregate server?");

      if (delete) {
        try {
          FileUtils.deleteTableData(table2, sSavePathText.getText());
        } catch (IOException e) {
          e.printStackTrace();
          showError("Unable to delete data.");
        }
      }
    }

    if (FileUtils.isDownloaded(table2, sApplyScanFmt.isSelected(), sDownloadAttachment.isSelected(),
        sExtraMetadata.isSelected(), sSavePathText.getText())) {
      boolean delete = promptConfirm("This CSV has been downloaded. "
          + "Delete existing CSV and download data from Aggregate server?");
      if (delete) {
        try {
          Files.delete(FileUtils.getCSVPath(table2, sApplyScanFmt.isSelected(),
              sDownloadAttachment.isSelected(), sExtraMetadata.isSelected(), sSavePathText.getText()));
        } catch (IOException e) {
          e.printStackTrace();
          showError("Unable to delete CSV");
        }
      }
    }
  }

  private void download() {
    try {
      sAggregateAddressText.setText(sAggregateAddressText.getText().trim());
      sAppIdText.setText(sAppIdText.getText().trim());
      sTableIdText.setText(sTableIdText.getText().trim());
      sUserNameText.setText(sUserNameText.getText().trim());
      //password could have spaces
      //trim only leading spaces for save path
      sSavePathText.setText(sSavePathText.getText().replaceAll("^\\s+", ""));

      updateAggregateTableInfo();

      if (table.getSchemaETag() == null || table.getSchemaETag().isEmpty()) {
        throw new IllegalArgumentException(
            "Unable to retrieve SchemaETag with given table info or credentials");
      }

      if (sDownloadAttachment.isSelected() || sApplyScanFmt.isSelected()) {
        FileUtils.createInstancesDirectory(table, sSavePathText.getText());
      } else {
        FileUtils.createBaseDirectory(table, sSavePathText.getText());
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

    if (sSavePathText.getText().isEmpty()) {
      System.out.println("sSavePathText is empty, using the default path");
      sSavePathText.setText(FileUtils.getDefaultSavePath().toAbsolutePath().toString());
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

  private GridBagConstraints getDefaultGbc() {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1;
    gbc.weighty = 1;

    return gbc;
  }
}