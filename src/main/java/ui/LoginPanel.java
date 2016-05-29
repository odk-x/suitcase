package ui;

import model.AggregateInfo;
import net.LoginTask;
import net.SuitcaseSwingWorker;
import net.WinkSingleton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;

import static ui.MessageString.*;
import static ui.LayoutConsts.*;

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
      if (checkFields(isAnon)) {
        loginButton.setEnabled(false);
        anonLoginButton.setEnabled(false);

        (isAnon ? anonLoginButton : loginButton).setText(LOGIN_LOADING_LABEL);

        try {
          buildAggregateInfo();

          LoginTask worker = new LoginTask(aggInfo, true);
          worker.addPropertyChangeListener(parent.getProgressBar());
          worker.addPropertyChangeListener(LoginPanel.this);
          worker.execute();
        } catch (MalformedURLException e1) {
          DialogUtils.showError(BAD_URL, true);
          e1.printStackTrace();
        }
      }
    }
  }

  private static final String LOGIN_LABEL = "Login";
  private static final String LOGIN_ANON_LABEL = "Anonymous Login";
  private static final String LOGIN_LOADING_LABEL = "Loading";

  private AggregateInfo aggInfo;
  private JTextField sAggregateAddressText;
  private JTextField sAppIdText;
  private JTextField sUserNameText;
  private JPasswordField sPasswordText;
  private JButton sLoginButton;
  private JButton sAnonLoginButton;

  private MainPanel parent;

  public LoginPanel(MainPanel parent) {
    super(new GridBagLayout());

    this.parent = parent;

    this.sAggregateAddressText = new JTextField(1);
    this.sAppIdText = new JTextField(1);
    this.sUserNameText = new JTextField(1);
    this.sPasswordText = new JPasswordField(1);
    this.sLoginButton = new JButton();
    this.sAnonLoginButton = new JButton();

    GridBagConstraints gbc = LayoutDefault.getDefaultGbc();
    gbc.gridx = 0;
    gbc.gridy = GridBagConstraints.RELATIVE;

    JPanel inputPanel = new InputPanel(
        new String[] {"Aggregate Address", "App ID", "Username", "Password"},
        new JTextField[] {sAggregateAddressText, sAppIdText, sUserNameText, sPasswordText},
        new String[] {"https://aggregate-server-url.appspot.com", "default", "", ""}
    );
    gbc.weighty = 85;
    gbc.insets = new Insets(80, 50, 0, 50);
    this.add(inputPanel, gbc);

    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 20, 0));
    buildLoginButtonArea(buttonPanel);
    gbc.weighty = 15;
    gbc.insets = new Insets(20, WINDOW_WIDTH / 4, 80, WINDOW_WIDTH / 4);
    this.add(buttonPanel, gbc);
  }

  public AggregateInfo getAggregateInfo() {
    return this.aggInfo;
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

  private void sanitizeFields(boolean anonymous) {
    sAggregateAddressText.setText(sAggregateAddressText.getText().trim());
    sAppIdText.setText(sAppIdText.getText().trim());
    sUserNameText.setText(sUserNameText.getText().trim());

    if (anonymous) {
      sUserNameText.setText("");
      sPasswordText.setText("");
    }
  }

  private boolean checkFields(boolean anonymous) {
    sanitizeFields(anonymous);

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

    // these are not required for anonymous authentication
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
      DialogUtils.showError(errorMsgBuilder.toString().trim(), true);
    }

    return state;
  }

  private void buildAggregateInfo() throws MalformedURLException {
    this.aggInfo = new AggregateInfo(
        sAggregateAddressText.getText(), sAppIdText.getText(), sUserNameText.getText(),
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
      if (WinkSingleton.getInstance().isInitialized()) {
        ((CardLayout) getParent().getLayout()).next(getParent());
      }
    }
  }
}
