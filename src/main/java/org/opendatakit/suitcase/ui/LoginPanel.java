package org.opendatakit.suitcase.ui;

import org.opendatakit.suitcase.model.CloudEndpointInfo;
import org.opendatakit.suitcase.net.LoginTask;
import org.opendatakit.suitcase.net.SuitcaseSwingWorker;
import org.opendatakit.suitcase.net.SyncWrapper;
import org.opendatakit.suitcase.utils.ButtonState;
import org.opendatakit.suitcase.utils.FieldsValidatorUtils;
import org.opendatakit.suitcase.utils.SuitcaseConst;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.MalformedURLException;
import java.util.Properties;

public class LoginPanel extends JPanel implements PropertyChangeListener {
  private class LoginActionListener implements ActionListener {
    private boolean isAnon;
    private JButton loginButton;
    private JButton anonLoginButton;

    public LoginActionListener(boolean isAnon, JButton loginButton, JButton anonLoginButton) {
      this.isAnon = isAnon;
      this.loginButton = loginButton;
      this.anonLoginButton = anonLoginButton;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      sanitizeFields(isAnon);
      String error = FieldsValidatorUtils.checkLoginFields(
          sCloudEndpointAddressText.getText(), sAppIdText.getText(), sUserNameText.getText(),
          String.valueOf(sPasswordText.getPassword()), isAnon
      );

      if (error != null) {
        DialogUtils.showError(error, true);
      } else {
        loginButton.setEnabled(false);
        anonLoginButton.setEnabled(false);

        // change label of the login button clicked
        (isAnon ? anonLoginButton : loginButton).setText(LOGIN_LOADING_LABEL);

        try {
          buildCloudEndpointInfo();

          LoginTask worker = new LoginTask(cloudEndpointInfo, true);
          worker.addPropertyChangeListener(parent.getProgressBar());
          worker.addPropertyChangeListener(LoginPanel.this);
          worker.execute();
        } catch (MalformedURLException e1) {
          DialogUtils.showError(MessageString.BAD_URL, true);
          e1.printStackTrace();
        }
      }
    }
  }

    private static final String LOGIN_LABEL = "Login";
    private static final String LOGIN_ANON_LABEL = "Anonymous Login";
    private static final String LOGIN_LOADING_LABEL = "Loading";
    private static final String LOGO_DESCRIPTION = "ODK-X Logo";
    private static final String DEFAULT_APP_ID = "default";

    private CloudEndpointInfo cloudEndpointInfo;
    private JTextField sCloudEndpointAddressText;
    private JTextField sAppIdText;
    private JTextField sUserNameText;
    private JPasswordField sPasswordText;
    private JButton sLoginButton;
    private JButton sAnonLoginButton;
    private JCheckBox sUseDefaultAppIdCheckbox;
    private JCheckBox sSavePasswordCheckbox;

    private MainPanel parent;

    public LoginPanel(MainPanel parent) {
        super(new GridBagLayout());

        this.parent = parent;

        this.sCloudEndpointAddressText = new JTextField(1);
        this.sAppIdText = new JTextField(1);
        this.sUserNameText = new JTextField(1);
        this.sPasswordText = new JPasswordField(1);
        this.sLoginButton = new JButton();
        this.sAnonLoginButton = new JButton();
        this.sUseDefaultAppIdCheckbox = new JCheckBox();
        this.sSavePasswordCheckbox = new JCheckBox();

        GridBagConstraints gbc = LayoutDefault.getDefaultGbc();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;

        JPanel logoAndInputPanel = new JPanel(new GridBagLayout());
        buildInputPanel(logoAndInputPanel);
        gbc.weighty = 75;
        gbc.insets = new Insets(60, 40, 0, 40);
        this.add(logoAndInputPanel, gbc);

        gbc.insets = new Insets(10, LayoutConsts.WINDOW_WIDTH / 2, 0, 40);
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        buildLoginButtonArea(buttonPanel);

        buildCheckBoxArea(gbc);
        gbc.weighty = 15;
        gbc.insets = new Insets(10, LayoutConsts.WINDOW_WIDTH / 2, 40, 40);
        this.add(buttonPanel, gbc);
    }

    public CloudEndpointInfo getCloudEndpointInfo() {
        return this.cloudEndpointInfo;
    }

    public void loginFromProperties(){
        // Auto login if credentials are present
        File propFile = new File(SuitcaseConst.PROPERTIES_FILE);
        if(propFile.exists()){
            Properties appProperties = new Properties();
            try (FileInputStream fileInputStream = new FileInputStream(propFile)){
                appProperties.load(fileInputStream);
                if(appProperties.containsKey("username")&&appProperties.containsKey("app_id")&&appProperties.containsKey("server_url")) {
                    String username = appProperties.getProperty("username");
                    String serverUrl = appProperties.getProperty("server_url");
                    String appId = appProperties.getProperty("app_id");
                    String password = appProperties.getProperty("password");
                    if (!appProperties.containsKey("password")) {
                        sUserNameText.setText(username);
                        sAppIdText.setText(appId);
                        sCloudEndpointAddressText.setText(serverUrl);
                    } else {
                        this.cloudEndpointInfo = new CloudEndpointInfo(serverUrl, appId, username, password);
                        ((CardLayout) getParent().getLayout()).next(getParent());
                        LoginTask worker = new LoginTask(cloudEndpointInfo, true);
                        worker.addPropertyChangeListener(parent.getProgressBar());
                        worker.addPropertyChangeListener(parent.getIoPanel().getPullPanel());
                        parent.getIoPanel().setButtonsState(ButtonState.DISABLED, ButtonState.DISABLED, ButtonState.DISABLED, ButtonState.DISABLED);
                        worker.execute();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void buildLoginButtonArea(JPanel buttonsPanel) {
        // Define buttons
        sLoginButton.setText(LOGIN_LABEL);
        sLoginButton.addActionListener(new LoginActionListener(false, sLoginButton, sAnonLoginButton));

        sAnonLoginButton.setText(LOGIN_ANON_LABEL);
        sAnonLoginButton.addActionListener(new LoginActionListener(true, sLoginButton, sAnonLoginButton));

        buttonsPanel.add(sLoginButton);
        buttonsPanel.add(sAnonLoginButton);
    }

    private void buildCheckBoxArea(GridBagConstraints gbc){
        sUseDefaultAppIdCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if(sUseDefaultAppIdCheckbox.isSelected()){
                    sAppIdText.setText(DEFAULT_APP_ID);
                    sAppIdText.setEditable(false);
                }
                else {
                    sAppIdText.setEditable(true);
                }
            }
        });
        sUseDefaultAppIdCheckbox.setSelected(true);
        sAppIdText.setEditable(false);
        JPanel checkBoxPanel = new CheckboxPanel(new String[]{"Use default app id", "Save password"},
                new JCheckBox[]{sUseDefaultAppIdCheckbox, sSavePasswordCheckbox}, 1, 2);
        gbc.weighty = 10;
        this.add(checkBoxPanel,gbc);
    }

    private void buildInputPanel(JPanel logoAndInputPanel) {

        GridBagConstraints gbc = LayoutDefault.getDefaultGbc();
        gbc.gridy = 0;
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.weightx = 20;
        gbc.insets = new Insets(0, 0, 0, 0);

        try (InputStream resourceAsStream = this.getClass().getResourceAsStream(LayoutConsts.ODKX_LOGO_FILE_NAME)) {
            Image image = ImageIO.read(resourceAsStream);
            ImageIcon imageIcon = new ImageIcon(image, LOGO_DESCRIPTION);
            JLabel iconLabel = new JLabel(imageIcon);
            iconLabel.setHorizontalAlignment(SwingConstants.LEFT);
            logoAndInputPanel.add(iconLabel, gbc);
        } catch (IOException e) {
            e.printStackTrace();
        }
        gbc.weightx = 80;
        JPanel inputPanel = new InputPanel(
                new String[]{"Cloud Endpoint Address", "Username", "Password", "App ID"},
                new JTextField[]{sCloudEndpointAddressText, sUserNameText, sPasswordText, sAppIdText},
                new String[]{"https://cloud-endpoint-server-url.appspot.com", "", "", DEFAULT_APP_ID}
        );
        logoAndInputPanel.add(inputPanel, gbc);
    }


    private void sanitizeFields(boolean anonymous) {
        sCloudEndpointAddressText.setText(sCloudEndpointAddressText.getText().trim());
        sAppIdText.setText(sAppIdText.getText().trim());
        sUserNameText.setText(sUserNameText.getText().trim());

        if (anonymous) {
            sUserNameText.setText("");
            sPasswordText.setText("");
        }
    }

    private void buildCloudEndpointInfo() throws MalformedURLException {
        this.cloudEndpointInfo = new CloudEndpointInfo(
                sCloudEndpointAddressText.getText(), sAppIdText.getText(), sUserNameText.getText(),
                String.valueOf(sPasswordText.getPassword())
        );
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() != null && evt.getPropertyName().equals(SuitcaseSwingWorker.DONE_PROPERTY)) {
            // restore buttons
            sLoginButton.setText(LOGIN_LABEL);
            sLoginButton.setEnabled(true);
            sAnonLoginButton.setText(LOGIN_ANON_LABEL);
            sAnonLoginButton.setEnabled(true);

            // if login is successful, let parent switch to the next card
            if (SyncWrapper.getInstance().isInitialized()) {
              if(!sUserNameText.getText().equals("")){
                File propFile = new File(SuitcaseConst.PROPERTIES_FILE);
                if(!propFile.exists()){
                  try {
                    propFile.createNewFile();
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                }
                Properties appProperties = new Properties();
                appProperties.put("username",sUserNameText.getText());
                appProperties.put("app_id",sAppIdText.getText());
                appProperties.put("server_url",sCloudEndpointAddressText.getText());
                if(sSavePasswordCheckbox.isSelected())
                {
                    appProperties.put("password", String.valueOf(sPasswordText.getPassword()));
                }

                try(FileOutputStream fileOutputStream = new FileOutputStream(SuitcaseConst.PROPERTIES_FILE)) {
                    appProperties.store(fileOutputStream,"Save login credentials");
                  } catch (IOException e) {
                      e.printStackTrace();
                  }
              }
                ((CardLayout) getParent().getLayout()).next(getParent());
            }
        }
    }
}
