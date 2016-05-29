package ui;

import model.AggregateInfo;

import javax.swing.*;
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

    JSplitPane splitPane =
        new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, parent.getProgressBar());
    splitPane.setResizeWeight(0.8);
    splitPane.setDividerSize(0);
    splitPane.setEnabled(false);

    this.add(splitPane, LayoutDefault.getDefaultGbc());
  }

  public AggregateInfo getAggInfo() {
    return parent.getAggInfo();
  }

  public SuitcaseProgressBar getProgressBar() {
    return parent.getProgressBar();
  }
}
