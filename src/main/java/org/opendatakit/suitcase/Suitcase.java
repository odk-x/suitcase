package org.opendatakit.suitcase;

import org.opendatakit.suitcase.net.SuitcaseSwingWorker;
import org.opendatakit.suitcase.ui.LayoutConsts;
import org.opendatakit.suitcase.ui.MainPanel;
import org.opendatakit.suitcase.ui.SuitcaseCLI;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

import static org.opendatakit.suitcase.ui.LayoutConsts.WINDOW_HEIGHT;
import static org.opendatakit.suitcase.ui.LayoutConsts.WINDOW_WIDTH;

public class Suitcase {
  private static final String SUITCASE_TITLE = "Suitcase";
  public static void main(String[] args) {
    int retCode = SuitcaseSwingWorker.okCode;
    if (args.length > 0) {
      retCode = new SuitcaseCLI(args).startCLI();
      System.exit(retCode);
    } else {
      EventQueue.invokeLater(new Runnable() {
        @Override
        public void run() {
          JFrame frame = new JFrame(SUITCASE_TITLE);
          frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
          frame.setLocationRelativeTo(null);
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

          frame.add(new MainPanel());
          Font defaultLabelFont;
          try(InputStream resourceStream = this.getClass().getResourceAsStream(LayoutConsts.DEFAULT_LABEL_FONT_FILE)) {
            defaultLabelFont = Font.createFont(Font.TRUETYPE_FONT, resourceStream);
            UIManager.put("Label.font", defaultLabelFont.deriveFont(Font.PLAIN,14));
          } catch (FontFormatException e) {
            e.printStackTrace();
          } catch (IOException e) {
            e.printStackTrace();
          }
          frame.setVisible(true);
        }
      });
    }
  }
}
