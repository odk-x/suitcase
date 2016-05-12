import model.AggregateInfo;
import net.RESTClient;
import org.apache.commons.cli.*;
import org.apache.wink.json4j.JSONException;
import utils.FileUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import static ui.MessageString.*;

/**
 * Handles UI of Suitcase
 */
public class Suitcase {
  private static final String APP_NAME = "ODK Suitcase";

  private static final int WIDTH = 1000;
  private static final int HEIGHT = 400;
  private static final int PUSH_PULL_H_MARGIN = 10;

  // Global UI Hooks
  private JFrame frame;
  private JTextField sAggregateAddressText;
  private JTextField sAppIdText;
  private JTextField sTableIdText;
  private JTextField sVersionPushText;
  private JTextField sSavePathText;
  private JTextField sDataPathText;
  private JTextField sUserNameText;
  private JPasswordField sPasswordText;
  private JProgressBar sProgressBar;
  private JCheckBox sDownloadAttachment;
  private JCheckBox sApplyScanFmt;
  private JCheckBox sExtraMetadata;
  private JButton sLoginButton;
  private JButton sAnonLoginButton;
  private JButton sPullButton;
  private JButton sPushButton;
  private JButton sResetButton;
  private boolean isGUI;
  private CardLayout contentCardLayout;
  private JPanel contentPanel;

  // Server data
  private AggregateInfo table;
  private RESTClient restClient;

  private boolean force;
  private Scanner console;

  private enum OPERATION {
    DOWNLOAD, UPLOAD, RESET, INFO
  }

  public static void main(String[] args) {
    Suitcase rs = new Suitcase();
    rs.start(args);
  }

  private Suitcase() {
    this.sAggregateAddressText = new JTextField(1);
    this.sAppIdText = new JTextField(1);
    this.sTableIdText = new JTextField(1);
    this.sVersionPushText = new JTextField(1);
    this.sSavePathText = new JTextField(1);
    this.sDataPathText = new JTextField(1);
    this.sUserNameText = new JTextField();
    this.sPasswordText = new JPasswordField();
    this.sProgressBar = new JProgressBar();
    this.sDownloadAttachment = new JCheckBox();
    this.sApplyScanFmt = new JCheckBox();
    this.sExtraMetadata = new JCheckBox();
    this.sLoginButton = new JButton();
    this.sAnonLoginButton = new JButton();
    this.sPullButton = new JButton();
    this.sPushButton = new JButton();
    this.sResetButton = new JButton();
    this.contentCardLayout = new CardLayout();
    this.contentPanel = new JPanel(contentCardLayout);

    this.isGUI = true;
    this.console = new Scanner(System.in);
  }

  private void start(String[] args) {
    if (args.length == 0) {
      //start GUI when no argument is passed
      startGUI();
    } else {
      isGUI = false;
      startCLI(args);
    }
  }

  private void startCLI(String[] args) {
    switch (parseArgs(args, buildOptions())) {
    case DOWNLOAD:
      if (checkDownloadFields())
        download();
      break;
    case UPLOAD:
      if (checkUploadFields()) {
        upload();
      }
      break;
    case RESET:
      if (checkResetFields()) {
        reset();
      }
      break;
    default:
      //nothing
    }
  }

  private Options buildOptions() {
    Options opt = new Options();
    //operations
    opt.addOption("download", false, "Download csv");
    opt.addOption("upload", false, "Upload csv");
    opt.addOption("reset", false, "Reset server");

    //aggregate related
    opt.addOption("aggregate_url", true, "url to Aggregate server");
    opt.addOption("app_id", true, "app id");
    opt.addOption("table_id", true, "table id");
    opt.addOption("username", true, "username");
    opt.addOption("password", true, "password");
    opt.addOption("dataVersion", true, "version of data, usually 1 or 2");

    //csv
    opt.addOption("a", "attachment", false, "download attachments");
    opt.addOption("s", "scan", false, "apply Scan formatting");
    opt.addOption("e", "extra", false, "add extra metadata columns");
    opt.addOption
        ("o", "output", true, "specify a custom output directory, default is ./Download/");

    //UI
    opt.addOption("f", "force", false, "do not prompt, overwrite existing files");

    //misc
    opt.addOption("h", "help", false, "print this message");
    opt.addOption("v", "version", false, "prints version information");

    return opt;
  }

  /**
   * Parses user arguments from pre-specified Options.
   *
   * @param args Arguments passed to by user
   * @param options Options to parse from
   * @return false when either "-h" or "-v" is passed, otherwise true
   */
  private OPERATION parseArgs(String[] args, Options options) {
    try {
      CommandLineParser parser = new DefaultParser();
      CommandLine line = parser.parse(options, args);

      //handle -h and --help
      if (line.hasOption('h')) {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp("suitcase", options);
        return OPERATION.INFO;
      }

      //handle -v
      if (line.hasOption('v')) {
        System.out.println("ODK Suitcase 2.0");
        return OPERATION.INFO;
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

      //Misc
      sSavePathText.setText(line.getOptionValue("o"));
    } catch (ParseException e) {
      e.printStackTrace();
    }

    return OPERATION.DOWNLOAD;
  }

  private void startGUI() {
    this.frame = new JFrame(APP_NAME);
    frame.setSize(WIDTH, HEIGHT);
    frame.setLocationRelativeTo(null);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    buildJFrame();
  }

  private void buildJFrame() {
    // UI Container Panels
    JPanel loginCard = new JPanel(new GridBagLayout());
    buildLoginCard(loginCard);

    JPanel pushPullCard = new JPanel(new GridBagLayout());
    buildPushPullCard(pushPullCard);

//    contentPanel.add(loginCard);
    contentPanel.add(pushPullCard);

    // Finish building the frame
    frame.add(contentPanel);
    frame.setVisible(true);
  }

  private void buildLoginCard(JPanel loginCard) {
    GridBagConstraints gbc = getDefaultGbc();
    gbc.gridx = 0;
    gbc.gridy = GridBagConstraints.RELATIVE;

    JPanel inputPanel = new JPanel(new GridBagLayout());
    buildInputArea(
        inputPanel,
        new String[] {"Aggregate Address", "App ID", "Username", "Password"},
        new JTextField[] {sAggregateAddressText, sAppIdText, sUserNameText, sPasswordText},
        new String[] {"https://aggregate-server-url.appspot.com", "default", "", ""}
    );
    gbc.weighty = 85;
    gbc.insets = new Insets(80, 200, 0, 200);
    loginCard.add(inputPanel, gbc);

    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 20, 0));
    buildLoginButtonArea(buttonPanel);
    gbc.weighty = 15;
    gbc.insets = new Insets(20, WIDTH / 4, 80, WIDTH / 4);
    loginCard.add(buttonPanel, gbc);
  }

  private void buildPushPullCard(JPanel pushPullCard) {
    GridBagConstraints gbc = getDefaultGbc();

    JPanel pullPanel = new JPanel(new GridBagLayout());
    pullPanel.setBorder(new EmptyBorder(10, PUSH_PULL_H_MARGIN, 0, PUSH_PULL_H_MARGIN));
    buildPullArea(pullPanel);

    JPanel pushPanel = new JPanel(new GridBagLayout());
    pushPanel.setBorder(new EmptyBorder(10, PUSH_PULL_H_MARGIN, 0, PUSH_PULL_H_MARGIN));
    buildPushArea(pushPanel);

    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pullPanel, pushPanel);
    splitPane.setResizeWeight(0.5);
    splitPane.setDividerSize(0);
    splitPane.setEnabled(false);
    pushPullCard.add(splitPane, gbc);

    JPanel progressBarPanel = new JPanel(new GridLayout(1, 1));
    buildProgressBarArea(progressBarPanel);

    JSplitPane splitPaneVertical =
        new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPane, progressBarPanel);
    splitPaneVertical.setResizeWeight(0.8);
    splitPaneVertical.setDividerSize(0);
    splitPaneVertical.setEnabled(false);
    pushPullCard.add(splitPaneVertical, gbc);
  }

  private void buildPullArea(JPanel pullPanel) {
    GridBagConstraints gbc = getDefaultGbc();
    gbc.gridx = 0;
    gbc.gridy = GridBagConstraints.RELATIVE;

    JPanel pullInputPanel = new JPanel(new GridBagLayout());
    buildInputArea(
        pullInputPanel,
        new String[] {"Table ID"}, new JTextField[] {sTableIdText}, new String[] {"table_id"}
    );
    gbc.weighty = 2;
    pullPanel.add(pullInputPanel, gbc);

    JPanel pullPrefPanel = new JPanel(new GridLayout(2, 1));
    buildCheckboxArea(
        pullPrefPanel,
        new String[] {"Download attachments?", "Apply Scan formatting?", "Extra metadata columns?"},
        new JCheckBox[] {sDownloadAttachment, sApplyScanFmt, sExtraMetadata}
    );
    gbc.weighty = 5;
    pullPanel.add(pullPrefPanel, gbc);

    JPanel savePathPanel = new JPanel(new GridBagLayout());
    buildDirChooserArea(
        savePathPanel, sSavePathText, "Save to",
        FileUtils.getDefaultSavePath().toAbsolutePath().toString()
    );
    gbc.weighty = 1;
    pullPanel.add(savePathPanel, gbc);

    JPanel pullButtonPanel = new JPanel(new GridLayout(1, 1));
    buildPullButtonArea(pullButtonPanel);
    gbc.weighty = 2;
    gbc.insets = new Insets(10, 0, 0, 0);
    pullPanel.add(pullButtonPanel, gbc);
  }

  private void buildPushArea(JPanel pushPanel) {
    GridBagConstraints gbc = getDefaultGbc();
    gbc.gridx = 0;
    gbc.gridy = GridBagConstraints.RELATIVE;

    JPanel pushInputPanel = new JPanel(new GridBagLayout());
    buildInputArea(
        pushInputPanel,
        new String[] {"Version"}, new JTextField[] {sVersionPushText}, new String[] {"2"}
    );
    gbc.weighty = 2;
    pushPanel.add(pushInputPanel, gbc);

    // Will add upload options in the future, adding these now makes layout easier (a lot easier)
    JPanel pushPrefPanel = new JPanel(new GridLayout(2, 1));
    buildCheckboxArea(
        pushPrefPanel,
        new String[] {"Option Placeholder", "Option Placeholder 2"},
        new JCheckBox[] {new JCheckBox(), new JCheckBox()}
    );
    gbc.weighty = 5;
    pushPanel.add(pushPrefPanel, gbc);

    JPanel dataPathPanel = new JPanel(new GridBagLayout());
    buildDirChooserArea(
        dataPathPanel, sDataPathText, "Upload",
        FileUtils.getDefaultSavePath().toAbsolutePath().toString()
    );
    gbc.weighty = 1;
    pushPanel.add(dataPathPanel, gbc);

    JPanel pushButtonPanel = new JPanel(new GridBagLayout());
    buildPushButtonArea(pushButtonPanel);
    gbc.insets = new Insets(10, -100, 0, 0);
    gbc.weighty = 2;
    pushPanel.add(pushButtonPanel, gbc);
  }

  private void buildInputArea(JPanel parentPanel, String[] labelArr, JTextField[] fields,
      String[] defaultText) {
    if ((labelArr.length != fields.length) || (fields.length != defaultText.length)) {
      throw new IllegalArgumentException("Arrays have unequal length!");
    }

    GridBagConstraints gbc = getDefaultGbc();
    gbc.gridx = GridBagConstraints.RELATIVE;
    gbc.gridy = 0;

    //labels
    JPanel labelPanel = new JPanel(new GridLayout(labelArr.length, 1));
    gbc.weightx = 1;
    parentPanel.add(labelPanel, gbc);
    for (String label : labelArr) {
      labelPanel.add(new JLabel(label));
    }

    //text fields
    JPanel inputPanel = new JPanel(new GridLayout(labelArr.length, 1));
    gbc.weightx = 9;
    parentPanel.add(inputPanel, gbc);
    for (int i = 0; i < fields.length; i++) {
      if (!defaultText[i].isEmpty()) {
        fields[i].setText(defaultText[i]);
      }
      fields[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
      inputPanel.add(fields[i]);
    }
  }

  private void buildLoginButtonArea(JPanel buttonsPanel) {
    // Define buttons
    sLoginButton.setText("Login");
    sLoginButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (checkLoginFields(false)) {
          sLoginButton.setEnabled(false);
          sLoginButton.setText("Loading...");

          sAnonLoginButton.setEnabled(false);

          new Thread(new Runnable() {
            @Override
            public void run() {
              if (login()) {
                contentCardLayout.next(contentPanel);
              }
            }
          }).start();
        }
      }
    });

    sAnonLoginButton.setText("Anonymous Login");
    sAnonLoginButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (checkLoginFields(true)) {
          sAnonLoginButton.setEnabled(false);
          sAnonLoginButton.setText("Loading...");

          sLoginButton.setEnabled(false);

          sUserNameText.setText("");
          sPasswordText.setText("");

          new Thread(new Runnable() {
            @Override
            public void run() {
              if (login()) {
                contentCardLayout.next(contentPanel);
              }
            }
          }).start();
        }
      }
    });

    buttonsPanel.add(sLoginButton);
    buttonsPanel.add(sAnonLoginButton);
  }

  private void buildPullButtonArea(JPanel pullButtonPanel) {
    sPullButton.setText("Download");
    sPullButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (checkDownloadFields()) {
          setPushPullBtnState(false);
          sPullButton.setText("Downloading...");
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

    pullButtonPanel.add(sPullButton);
  }

  private void buildPushButtonArea(JPanel pushButtonPanel) {
    GridBagConstraints gbc = getDefaultGbc();
    gbc.gridx = GridBagConstraints.RELATIVE;
    gbc.gridy = 0;

    sPushButton.setText("Upload");
    sPushButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (checkUploadFields()) {
          setPushPullBtnState(false);
          sPushButton.setText("Uploading...");
          sProgressBar.setIndeterminate(true);

          new Thread(new Runnable() {
            @Override
            public void run() {
              upload();
            }
          }).start();
        }
      }
    });

    sResetButton.setText("Reset");
    sResetButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (checkResetFields()) {
          setPushPullBtnState(false);
          sResetButton.setText("Resetting...");
          sProgressBar.setIndeterminate(true);

          new Thread(new Runnable() {
            @Override
            public void run() {
              reset();
            }
          }).start();
        }
      }
    });

    pushButtonPanel.add(sResetButton, gbc);
    pushButtonPanel.add(sPushButton, gbc);
  }

  private void buildDirChooserArea(JPanel parentPanel, final JTextField pathText, String label,
      final String defaultPath) {
    GridBagConstraints gbc = getDefaultGbc();
    gbc.gridx = GridBagConstraints.RELATIVE;
    gbc.gridy = 0;

    //label
    JLabel savePathLabel = new JLabel(label);
    savePathLabel.setHorizontalAlignment(JLabel.CENTER);
    gbc.weightx = 5;
    parentPanel.add(savePathLabel, gbc);

    //text field
    pathText.setText(defaultPath);
    pathText.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    gbc.weightx = 90;
    parentPanel.add(pathText, gbc);

    //button
    JButton dirButton = new JButton();
    dirButton.setText("...");
    dirButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(new File(defaultPath));

        int result = chooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
          pathText.setText(chooser.getSelectedFile().toString());
        }
      }
    });
    gbc.weightx = 5;

    parentPanel.add(dirButton, gbc);
  }

  private void buildCheckboxArea(JPanel parentPanel, String[] labels, JCheckBox[] checkBoxes) {
    if (labels.length != checkBoxes.length) {
      throw new IllegalArgumentException("Array length not equal");
    }

    for (int i = 0; i < labels.length; i++) {
      JCheckBox cb = checkBoxes[i];

      cb.setText(labels[i]);
      cb.setHorizontalAlignment(JCheckBox.CENTER);
      cb.setBorder(new EmptyBorder(0, -100, 0, -100));

      parentPanel.add(cb);
    }
  }

  private void buildProgressBarArea(JPanel pbPanel) {
    sProgressBar.setMinimum(0);
    sProgressBar.setMaximum(100);
    sProgressBar.setValue(100);
    sProgressBar.setString("Idle");
    sProgressBar.setStringPainted(true);

    pbPanel.add(sProgressBar);
  }

  private boolean login() {
    try {
      this.table = new AggregateInfo(
          sAggregateAddressText.getText(), sAppIdText.getText(),
          sUserNameText.getText(), String.valueOf(sPasswordText.getPassword())
      );
      this.restClient = new RESTClient(table);
      if (isGUI) {
        this.restClient.setProgressBar(this.sProgressBar);
      }

      return true;
    } catch (MalformedURLException e) {
      e.printStackTrace();
      showError(BAD_URL);
    } catch (JSONException e) {
      e.printStackTrace();
      showError(BAD_CRED);
    } catch (Exception e) {
      e.printStackTrace();
      showError(GENERIC_ERR);
    } finally {
      if (isGUI) {
        sLoginButton.setEnabled(true);
        sAnonLoginButton.setEnabled(true);

        sLoginButton.setText("Login");
        sAnonLoginButton.setText("Anonymous Login");

        sProgressBar.setString("Idle");
      }
    }

    return false;
  }

  private void download() {
    boolean firstRun = table.getCurrentTableId() == null;

    table.setCurrentTableId(sTableIdText.getText());
    restClient.setFilePath(sSavePathText.getText());

    if (firstRun && FileUtils.isDownloaded(table, sSavePathText.getText())) {
      boolean delete = promptConfirm(OVERWRITE_DATA);

      if (delete) {
        try {
          FileUtils.deleteTableData(table, sSavePathText.getText());
        } catch (IOException e) {
          e.printStackTrace();
          showError(IO_DELETE_ERR);
        }
      }
    }

    if (FileUtils.isDownloaded(table, sApplyScanFmt.isSelected(), sDownloadAttachment.isSelected(),
        sExtraMetadata.isSelected(), sSavePathText.getText())) {
      boolean delete = promptConfirm(OVERWRITE_CSV);

      if (delete) {
        try {
          Files.delete(FileUtils.getCSVPath(table, sApplyScanFmt.isSelected(),
              sDownloadAttachment.isSelected(), sExtraMetadata.isSelected(), sSavePathText.getText()));
        } catch (IOException e) {
          e.printStackTrace();
          showError(IO_DELETE_ERR);
        }
      }
    }

    try {
      if (sDownloadAttachment.isSelected() || sApplyScanFmt.isSelected()) {
        FileUtils.createInstancesDirectory(table, sSavePathText.getText());
      } else {
        FileUtils.createBaseDirectory(table, sSavePathText.getText());
      }

      restClient.writeCSVToFile(
          sApplyScanFmt.isSelected(), sDownloadAttachment.isSelected(), sExtraMetadata.isSelected()
      );

      if (isGUI)
        sProgressBar.setString("Done!");
      else
        System.out.println("Done!");
    } catch (IOException e) {
      e.printStackTrace();
      showError(IO_WRITE_ERR);
    } catch (JSONException e) {
      e.printStackTrace();
      showError(GENERIC_ERR);
    } finally {
      if (isGUI) {
        sProgressBar.setValue(sProgressBar.getMaximum());
        sProgressBar.setIndeterminate(false);
        sPullButton.setText("Download");
        setPushPullBtnState(true);
      }
    }
  }

  private void upload() {
    restClient.setFilePath(sDataPathText.getText());
    restClient.setVersion(sVersionPushText.getText());

    try {
      restClient.pushAllData();

      if (isGUI)
        sProgressBar.setString("Done!");
      else
        System.out.println("Done!");
    } catch (Exception e) {
      e.printStackTrace();
      showError(GENERIC_ERR);
    } finally {
      try {
        restClient.updateTableList();
      } catch (Exception e) {
        // ignore
      }

      if (isGUI) {
        sProgressBar.setValue(sProgressBar.getMaximum());
        sProgressBar.setIndeterminate(false);
        sPushButton.setText("Upload");
        setPushPullBtnState(true);
      }
    }
  }

  private void reset() {
    restClient.setVersion(sVersionPushText.getText());

    try {
      restClient.deleteAllRemote();

      if (isGUI)
        sProgressBar.setString("Done!");
      else
        System.out.println("Done!");
    } catch (Exception e) {
      e.printStackTrace();
      showError(GENERIC_ERR);
    } finally {
      try {
        restClient.updateTableList();
      } catch (Exception e) {
        // ignore
      }

      if (isGUI) {
        sProgressBar.setValue(sProgressBar.getMaximum());
        sProgressBar.setIndeterminate(false);
        sResetButton.setText("Reset");
        setPushPullBtnState(true);
      }
    }
  }

  /**
   * Checks if aggregate address, App ID, and Table ID supplied are empty.
   * And displays a pop up showing the fields that are empty, if applicable.
   *
   * @return true when information presented has no problem
   */
  private boolean checkLoginFields(boolean anonymous) {
    sAggregateAddressText.setText(sAggregateAddressText.getText().trim());
    sAppIdText.setText(sAppIdText.getText().trim());
    sUserNameText.setText(sUserNameText.getText().trim());

    boolean state = true;
    StringBuilder errorMsgBuilder = new StringBuilder();

    if (sAggregateAddressText.getText().isEmpty()) {
      errorMsgBuilder.append(AGG_EMPTY).append(NEW_LINE);
      state = false;
    }

    if (sAppIdText.getText().isEmpty()) {
      errorMsgBuilder.append(APP_ID_EMPTY).append(NEW_LINE);
      state = false;
    }

    if (!anonymous) {
      if (sUserNameText.getText().isEmpty()) {
        errorMsgBuilder.append(USERNAME_EMPTY).append(NEW_LINE);
        state = false;
      }

      if (String.valueOf(sPasswordText.getPassword()).isEmpty()) {
        errorMsgBuilder.append(PASSWORD_EMPTY).append(NEW_LINE);
        state = false;
      }
    }

    if (!state) {
      showError(errorMsgBuilder.toString().trim());
    }

    return state;
  }

  private boolean checkDownloadFields() {
    sTableIdText.setText(sTableIdText.getText().trim());
    //trim only leading spaces for save path
    sSavePathText.setText(sSavePathText.getText().replaceAll("^\\s+", ""));

    boolean state = true;
    StringBuilder errorMsgBuilder = new StringBuilder();

    if (sTableIdText.getText().isEmpty()) {
      errorMsgBuilder.append(TABLE_ID_EMPTY).append(NEW_LINE);
      state = false;
    }

    if (!sTableIdText.getText().isEmpty() && !table.tableIdExists(sTableIdText.getText())) {
      errorMsgBuilder.append(BAD_TABLE_ID).append(NEW_LINE);
      state = false;
    }

    if (sSavePathText.getText().isEmpty()) {
      System.out.println("sSavePathText is empty, using the default path.\n");
      sSavePathText.setText(FileUtils.getDefaultSavePath().toAbsolutePath().toString());
      // does not flip state to false
    }

    if (!state) {
      showError(errorMsgBuilder.toString().trim());
    }

    return state;
  }

  private boolean checkUploadFields() {
    sVersionPushText.setText(sVersionPushText.getText().trim());
    //trim only leading spaces for data path
    sDataPathText.setText(sDataPathText.getText().replaceAll("^\\s+", ""));

    boolean state = true;
    StringBuilder errorMsgBuilder = new StringBuilder();

    if (sVersionPushText.getText().isEmpty()) {
      errorMsgBuilder.append(VERSION_EMPTY).append(NEW_LINE);
      state = false;
    }

    if (sDataPathText.getText().isEmpty()) {
      errorMsgBuilder.append(DATA_PATH_EMPTY).append(NEW_LINE);
      state = false;
    } else {
      if (Files.notExists(Paths.get(sDataPathText.getText()))) {
        errorMsgBuilder.append(DATA_DIR_NOT_EXIST).append(NEW_LINE);
        state = false;
      } else {
        if (!FileUtils.checkUploadDir(sDataPathText.getText())) {
          errorMsgBuilder.append(DATA_DIR_INVALID).append(NEW_LINE);
          state = false;
        }
      }
    }

    if (!state) {
      showError(errorMsgBuilder.toString().trim());
    }

    return state;
  }

  private boolean checkResetFields() {
    sVersionPushText.setText(sVersionPushText.getText().trim());

    boolean state = true;
    StringBuilder errorMsgBuilder = new StringBuilder();

    if (sVersionPushText.getText().isEmpty()) {
      errorMsgBuilder.append(VERSION_EMPTY).append(NEW_LINE);
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
    if (isGUI) {
      sProgressBar.setIndeterminate(false);
      sProgressBar.setString("error");
      JOptionPane.showConfirmDialog(frame, errMsg, "Error", JOptionPane.DEFAULT_OPTION,
          JOptionPane.ERROR_MESSAGE);
    }
    else
      System.out.println("Error: " + errMsg);
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
    gbc.ipadx = 0;
    gbc.ipady = 0;

    return gbc;
  }

  private void setPushPullBtnState(boolean state) {
    sPullButton.setEnabled(state);
    sPushButton.setEnabled(state);
    sResetButton.setEnabled(state);
  }
}