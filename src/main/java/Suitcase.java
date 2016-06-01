import ui.MainPanel;
import ui.SuitcaseCLI;

import javax.swing.*;
import java.awt.*;

import static ui.LayoutConsts.WINDOW_HEIGHT;
import static ui.LayoutConsts.WINDOW_WIDTH;

public class Suitcase {
  public static void main(String[] args) {
    if (args.length > 0) {
      new SuitcaseCLI(args).startCLI();
    } else {
      EventQueue.invokeLater(new Runnable() {
        @Override
        public void run() {
          JFrame frame = new JFrame("Suitcase");
          frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
          frame.setLocationRelativeTo(null);
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

          frame.add(new MainPanel());
          frame.setVisible(true);
        }
      });
    }
  }
}
