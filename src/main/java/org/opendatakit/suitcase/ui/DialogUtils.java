package org.opendatakit.suitcase.ui;

import javax.swing.*;
import org.opendatakit.suitcase.utils.SuitcaseConst;
import java.util.Scanner;

public class DialogUtils {
  private static final String ERR_POPUP_TITLE = "Error";
  private static final String CONFIRM_POPUP_TITLE = SuitcaseConst.APP_NAME;

  /**
   * Displays error message.
   *
   * When ran as GUI,
   * displays a pop up with an error message.
   * Progress bar is set to non-indeterminate and string set to "error."
   *
   * @param errMsg Error message to display
   * @param isGUI is GUI?
   */
  public static void showError(String errMsg, boolean isGUI) {
    if (isGUI) {
      JOptionPane.showConfirmDialog(null, errMsg, ERR_POPUP_TITLE, JOptionPane.DEFAULT_OPTION,
          JOptionPane.ERROR_MESSAGE);
    }
    else {
      System.out.println("Error: " + errMsg);
    }
  }

  public static boolean promptConfirm(String msg, boolean isGUI, boolean skip) {
    if (skip) {
      return true;
    }

    if (isGUI) {
      return JOptionPane.YES_OPTION ==
          JOptionPane.showConfirmDialog(null, msg, CONFIRM_POPUP_TITLE, JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE);
    } else {
      System.out.print(msg + " yes / no ");
      boolean confirmed = false;
      
      Scanner scanner = null;
      try {
        scanner = new Scanner(System.in);
        confirmed = scanner.nextLine().toLowerCase().startsWith("y");
      } finally {
        if (scanner != null) {
          scanner.close();
        }
        
      }
      return confirmed;
    }
  }
}
