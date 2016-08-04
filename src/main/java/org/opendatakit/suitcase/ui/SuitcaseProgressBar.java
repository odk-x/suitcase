package org.opendatakit.suitcase.ui;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class SuitcaseProgressBar extends JProgressBar implements PropertyChangeListener {
  private static final String PROGRESS_PROPERTY = "progress";
  private static final String STRING_PROPERTY = "string";
  private static final String INDETERMINATE_PROPERTY = "indeterminate";

  public static final String PB_ERROR = "Error";
  public static final String PB_DONE = "Done!";
  public static final String PB_IDLE = "Idle";

  public SuitcaseProgressBar() {
    super();

    this.setValue(100);
    this.setString(PB_IDLE);
    this.setStringPainted(true);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (evt.getNewValue() != null) {
      switch (evt.getPropertyName()) {
      case PROGRESS_PROPERTY:
        setValue((int) evt.getNewValue());
        break;
      case STRING_PROPERTY:
        setString((String) evt.getNewValue());
        break;
      case INDETERMINATE_PROPERTY:
        setIndeterminate((boolean) evt.getNewValue());
        break;
      }
    }
  }
}
