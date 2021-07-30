package org.opendatakit.suitcase.ui;

import org.opendatakit.suitcase.Suitcase;
import org.opendatakit.suitcase.model.CloudEndpointInfo;
import org.opendatakit.suitcase.net.SyncWrapper;
import org.opendatakit.suitcase.utils.ButtonState;
import org.opendatakit.suitcase.utils.SuitcaseConst;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.prefs.Preferences;

public class IOPanel extends JPanel {
  public static final String PULL_TAB_LABEL = "Download";
  public static final String PUSH_TAB_LABEL = "Upload";
  public static final String MODIFY_TAB_LABEL = "Modify";
  public static final String SUITCASE_DOCUMENTATION_URL = "https://docs.odk-x.org/suitcase-intro/";

  private MainPanel parent;
  private PullPanel pullPanel;
  private PushPanel pushPanel;
  private ModifyPanel modifyPanel;

  public IOPanel(MainPanel parent) {
    super(new BorderLayout());

    this.parent = parent;

    UIManager.put("TabbedPane.selected", LayoutConsts.BUTTON_BACKGROUND_COLOR);
    JTabbedPane tabs = new JTabbedPane(){
      public Color getForegroundAt(int index){
        if(getSelectedIndex() == index){
        return Color.WHITE;
        }
        else {
          return Color.BLACK;
        }
      }
    };
    pullPanel = new PullPanel(this);
    pushPanel = new PushPanel(this);
    modifyPanel = new ModifyPanel(this);
    tabs.addTab(PULL_TAB_LABEL, pullPanel);
    tabs.addTab(PUSH_TAB_LABEL, pushPanel);
    tabs.addTab(MODIFY_TAB_LABEL, modifyPanel);
    buildMenu();
    JSplitPane splitPane =
            new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, parent.getProgressBar());
    splitPane.setResizeWeight(0.8);
    splitPane.setDividerSize(0);
    splitPane.setEnabled(false);

    this.add(splitPane, BorderLayout.CENTER);
  }

  public void disableAllButtons(){
    pushPanel.setButtonsState(ButtonState.DISABLED, ButtonState.DISABLED);
    pullPanel.setButtonsState(ButtonState.DISABLED, ButtonState.DISABLED,ButtonState.DISABLED,ButtonState.DISABLED,ButtonState.DISABLED);
    modifyPanel.setButtonState(ButtonState.DISABLED, ButtonState.DISABLED);
  }

  public void enableAllButtons(){
    pushPanel.setButtonsState(ButtonState.ENABLED,ButtonState.ENABLED);
    pullPanel.setButtonsState(ButtonState.ENABLED,ButtonState.ENABLED,ButtonState.ENABLED,ButtonState.ENABLED,ButtonState.ENABLED);
    modifyPanel.setButtonState(ButtonState.ENABLED, ButtonState.ENABLED);
  }
  public CloudEndpointInfo getCloudEndpointInfo() {
    return parent.getCloudEndpointInfo();
  }

  public SuitcaseProgressBar getProgressBar() {
    return parent.getProgressBar();
  }

  public PullPanel getPullPanel() {
    return pullPanel;
  }

  public ModifyPanel getModifyPanel() {
    return modifyPanel;
  }

  public void logout(){
    Preferences userPreferences = Preferences.userNodeForPackage(Suitcase.class);
    userPreferences.remove(SuitcaseConst.PREFERENCES_PASSWORD_KEY);
    SyncWrapper.getInstance().reset();
    ((CardLayout) (parent.getLayout())).previous(parent);
  }

  private void buildMenu(){
    JMenuBar mb=new JMenuBar();
    JMenu menu = new JMenu("Menu");
    JMenuItem logoutButton = new JMenuItem("Logout");
    logoutButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        logout();
      }
    });
    JMenuItem documentationButton = new JMenuItem("Documentation");
    documentationButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          openWebpage(SUITCASE_DOCUMENTATION_URL);
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      }
    });
    menu.add(logoutButton);
    menu.add(documentationButton);
    mb.add(menu);
    this.add(mb,BorderLayout.NORTH);
  }

  private void openWebpage(String url) throws IOException {
    Runtime rt = Runtime.getRuntime();
    rt.exec("xdg-open "+url);
  }
}
