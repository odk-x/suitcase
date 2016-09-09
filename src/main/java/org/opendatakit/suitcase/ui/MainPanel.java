package org.opendatakit.suitcase.ui;

import org.opendatakit.suitcase.model.AggregateInfo;

import javax.swing.*;
import java.awt.*;

public class MainPanel extends JPanel {
  private SuitcaseProgressBar progressBar;
  private LoginPanel loginPanel;

  public MainPanel() {
    super(new CardLayout());

    this.progressBar = new SuitcaseProgressBar();
    this.loginPanel = new LoginPanel(this);

    this.add(loginPanel);
    this.add(new IOPanel(this));
  }

  public AggregateInfo getAggInfo() {
    return loginPanel.getAggregateInfo();
  }

  public SuitcaseProgressBar getProgressBar() {
    return progressBar;
  }
}
