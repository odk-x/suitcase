package org.opendatakit.suitcase.ui;

import org.opendatakit.suitcase.model.CloudEndpointInfo;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.*;

public class IOPanel extends JPanel {
  public static final String PULL_TAB_LABEL = "Download";
  public static final String PUSH_TAB_LABEL = "Upload";

  private MainPanel parent;

  public IOPanel(MainPanel parent) {
    super(new GridBagLayout());

    this.parent = parent;

    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab(PULL_TAB_LABEL, new PullPanel(this));
    tabs.addTab(PUSH_TAB_LABEL, new PushPanel(this));
    
    ChangeListener changeListener = new ChangeListener() {
        public void stateChanged(ChangeEvent changeEvent) {
          JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent.getSource();
          int index = sourceTabbedPane.getSelectedIndex();
          if(sourceTabbedPane.getTitleAt(index) == PUSH_TAB_LABEL) {
        	  LayoutConsts.EMPTY_DIR = true ;
          }
        }
      };
    tabs.addChangeListener(changeListener);
    
    JSplitPane splitPane =
        new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, parent.getProgressBar());
    splitPane.setResizeWeight(0.8);
    splitPane.setDividerSize(0);
    splitPane.setEnabled(false);

    this.add(splitPane, LayoutDefault.getDefaultGbc());
  }

  public CloudEndpointInfo getCloudEndpointInfo() {
    return parent.getCloudEndpointInfo();
  }

  public SuitcaseProgressBar getProgressBar() {
    return parent.getProgressBar();
  }
}
