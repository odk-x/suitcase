package org.opendatakit.suitcase.ui;

import java.awt.*;

public class LayoutDefault {
  /**
   * Returns a predefined set of GridBagConstraints
   *
   * @return the default GridBagConstraints
   */
  public static GridBagConstraints getDefaultGbc() {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.ipadx = 0;
    gbc.ipady = 0;

    return gbc;
  }
}
