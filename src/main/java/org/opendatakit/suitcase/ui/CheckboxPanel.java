package org.opendatakit.suitcase.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CheckboxPanel extends JPanel{
  public CheckboxPanel(String[] labels, JCheckBox[] checkboxes, int rows, int cols) {
    super(new GridLayout(rows, cols));

    if (labels.length != checkboxes.length) {
      throw new IllegalArgumentException("input arrays have unequal length");
    }

    for (int i = 0; i < labels.length; i++) {
      JCheckBox cb = checkboxes[i];

      cb.setText(labels[i]);
      cb.setHorizontalAlignment(JCheckBox.CENTER);
      cb.setBorder(new EmptyBorder(0, -100, 0, -100));

      this.add(cb);
    }
  }
}
