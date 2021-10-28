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
import java.net.URI;
import java.util.prefs.Preferences;

public class IOPanel extends JPanel {
  public static final String PULL_TAB_LABEL = "Download";
  public static final String PUSH_TAB_LABEL = "Upload";
  public static final String UPDATE_TAB_LABEL = "Update";
  public static final String CLEAR_TAB_LABEL = "Clear";
  public static final String MENU_LABEL = "Menu";
  public static final String LOGOUT_LABEL = "Logout";
  public static final String DOCUMENTATION_LABEL = "Documentation";
  public static final String OPEN_IN_BROWSER_LABEL = "Open in Browser";
  public static final String COPY_LINK_LABEL = "Copy link";
  public static final String SUITCASE_DOCUMENTATION_URL = "https://docs.odk-x.org/suitcase-intro/";

  private MainPanel parent;
  private PullPanel pullPanel;
  private PushPanel pushPanel;
  private UpdatePanel updatePanel;
  private ClearPanel clearPanel;

    public ClearPanel getClearPanel() {
        return clearPanel;
    }

    public IOPanel(MainPanel parent) {
    super(new BorderLayout());

    this.parent = parent;
    // Set Color For Selected Tab.
    // "TabbedPane.selected" is the default for the application.
    UIManager.put("TabbedPane.selected", LayoutConsts.SELECTED_TAB_COLOR);
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
    this.setName("io_panel");
    tabs.setName("tabs");
    

    pullPanel = new PullPanel(this);
    pushPanel = new PushPanel(this);
    updatePanel = new UpdatePanel(this);
    clearPanel = new ClearPanel(this);
    tabs.addTab(PULL_TAB_LABEL, pullPanel);
    tabs.addTab(PUSH_TAB_LABEL, pushPanel);
    tabs.addTab(UPDATE_TAB_LABEL, updatePanel);
    tabs.addTab(CLEAR_TAB_LABEL, clearPanel);
    buildMenu();
    JSplitPane splitPane =
            new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, parent.getProgressBar());
    splitPane.setResizeWeight(0.8);
    splitPane.setDividerSize(0);
    splitPane.setEnabled(false);

    this.add(splitPane, BorderLayout.CENTER);
  }

  public void disableAllButtons(){
    pushPanel.setButtonsState(ButtonState.DISABLED);
    pullPanel.setButtonsState(ButtonState.DISABLED, ButtonState.DISABLED,ButtonState.DISABLED,ButtonState.DISABLED,ButtonState.DISABLED);
    updatePanel.setButtonState(ButtonState.DISABLED);
    clearPanel.setButtonState(ButtonState.DISABLED,ButtonState.DISABLED);
  }

  public void enableAllButtons(){
    pushPanel.setButtonsState(ButtonState.ENABLED);
    pullPanel.setButtonsState(ButtonState.ENABLED,ButtonState.ENABLED,ButtonState.ENABLED,ButtonState.ENABLED,ButtonState.ENABLED);
    updatePanel.setButtonState(ButtonState.ENABLED);
    clearPanel.setButtonState(ButtonState.ENABLED,ButtonState.ENABLED);
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

  public UpdatePanel getUpdatePanel() {
    return updatePanel;
  }

  public void logout(){
    Preferences userPreferences = Preferences.userNodeForPackage(Suitcase.class);
    userPreferences.remove(SuitcaseConst.PREFERENCES_PASSWORD_KEY);
    SyncWrapper.getInstance().reset();
    ((CardLayout) (parent.getLayout())).first(parent);
  }

  private void buildMenu(){
    // Set color of selected Item in Menu
    UIManager.put("MenuItem.selectionBackground", LayoutConsts.SELECTED_MENU_ITEM_COLOR);
    UIManager.put("MenuItem.selectionForeground", LayoutConsts.MENU_ITEM_FOREGROUND_COLOR);

    // Two Items are there in  the menu. One for logout and another is a submenu for documentation
    // Sub menu has two options to either copy the link or to open the Documentation in a browser
    JMenuBar mb = new JMenuBar();
    JMenu menu = new JMenu(MENU_LABEL);
    JMenuItem logoutButton = new JMenuItem(LOGOUT_LABEL);
    logoutButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        logout();
      }
    });
    menu.add(logoutButton);
    menu.addSeparator();
    JMenu submenu = new JMenu(DOCUMENTATION_LABEL);
    JMenuItem copyLink = new JMenuItem(COPY_LINK_LABEL);
    JMenuItem documentationButton = new JMenuItem(OPEN_IN_BROWSER_LABEL);
    documentationButton.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
              openWebpage(SUITCASE_DOCUMENTATION_URL);
          }
    });
    copyLink.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
          String[] options = {"OK"};
          JPanel panel = new JPanel();
          JLabel lbl = new JLabel("Copy the following Link:");
          JTextField txt = new JTextField(SUITCASE_DOCUMENTATION_URL);
          panel.add(lbl);
          panel.add(txt);
          JOptionPane.showOptionDialog(null, panel, "Copy Link", JOptionPane.NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options , options[0]);
      }
    });
    submenu.add(copyLink);
    submenu.add(documentationButton);
    menu.add(submenu);
    mb.add(menu);
    this.add(mb,BorderLayout.NORTH);
  }

  private void openWebpage(String url){
    try{
      if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(new URI(url));
      }
    }
    catch (Exception e){
      e.printStackTrace();
    }
  }
}
