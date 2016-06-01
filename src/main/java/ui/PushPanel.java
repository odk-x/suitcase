package ui;

import net.ResetTask;
import net.SuitcaseSwingWorker;
import net.UploadTask;
import utils.FieldsValidatorUtils;
import utils.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Files;
import java.nio.file.Paths;

import static ui.MessageString.*;

public class PushPanel extends JPanel implements PropertyChangeListener {
  private static final String PUSH_LABEL = "Upload";
  private static final String PUSHING_LABEL = "Uploading";
  private static final String RESET_LABEL = "Reset";
  private static final String RESETTING_LABEL = "Resetting";
  private static final String DATA_PATH_LABEL = "Upload";

  private JTextField sVersionPushText;
  private JButton sPushButton;
  private JButton sResetButton;
  private PathChooserPanel dataPathChooser;

  private IOPanel parent;

  public PushPanel(IOPanel parent) {
    super(new GridBagLayout());

    this.parent = parent;

    this.sVersionPushText = new JTextField(1);
    this.sPushButton = new JButton();
    this.sResetButton = new JButton();
    this.dataPathChooser = new PathChooserPanel(
        DATA_PATH_LABEL, FileUtils.getDefaultUploadPath().toString()
    );

    GridBagConstraints gbc = LayoutDefault.getDefaultGbc();
    gbc.gridx = 0;
    gbc.gridy = GridBagConstraints.RELATIVE;

    JPanel pushInputPanel = new InputPanel(
        new String[] {"Version"},
        new JTextField[] {sVersionPushText},
        new String[] {"2"}
    );
    gbc.weighty = 2;
    this.add(pushInputPanel, gbc);

    // Will add upload options in the future, adding these now makes layout easier (a lot easier)
    JCheckBox placeholderCheckbox = new JCheckBox();
    placeholderCheckbox.setVisible(false);
    JCheckBox placeholderCheckbox2 = new JCheckBox();
    placeholderCheckbox2.setVisible(false);
    JPanel pushPrefPanel = new CheckboxPanel(
        new String[] {"Option Placeholder", "Option Placeholder 2"},
        new JCheckBox[] {placeholderCheckbox, placeholderCheckbox2},
        2, 1
    );
    gbc.weighty = 5;
    this.add(pushPrefPanel, gbc);

    gbc.weighty = 1;
    this.add(dataPathChooser, gbc);

    JPanel pushButtonPanel = new JPanel(new GridBagLayout());
    buildPushButtonArea(pushButtonPanel);
    gbc.insets = new Insets(10, -100, 0, 0);
    gbc.weighty = 2;
    this.add(pushButtonPanel, gbc);
  }

  private void buildPushButtonArea(JPanel pushButtonPanel) {
    GridBagConstraints gbc = LayoutDefault.getDefaultGbc();
    gbc.gridx = GridBagConstraints.RELATIVE;
    gbc.gridy = 0;

    sPushButton.setText(PUSH_LABEL);
    sPushButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        sVersionPushText.setText(sVersionPushText.getText().trim());

        String error = FieldsValidatorUtils.checkUploadFields(sVersionPushText.getText(),
            dataPathChooser.getPath());

        if (error != null) {
          DialogUtils.showError(error, true);
        } else {
          sPushButton.setText(PUSHING_LABEL);
          setButtonState(false);

          UploadTask worker = new UploadTask(parent.getAggInfo(), dataPathChooser.getPath(),
              sVersionPushText.getText(), true);
          worker.addPropertyChangeListener(parent.getProgressBar());
          worker.addPropertyChangeListener(PushPanel.this);
          worker.execute();
        }
      }
    });

    sResetButton.setText(RESET_LABEL);
    sResetButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        sVersionPushText.setText(sVersionPushText.getText().trim());

        String error = FieldsValidatorUtils.checkResetFields(sVersionPushText.getText());

        if (error != null) {
          DialogUtils.showError(error, true);
        } else {
          sResetButton.setText(RESETTING_LABEL);
          setButtonState(false);

          ResetTask worker = new ResetTask(sVersionPushText.getText(), true);
          worker.addPropertyChangeListener(parent.getProgressBar());
          worker.addPropertyChangeListener(PushPanel.this);
          worker.execute();
        }
      }
    });

    pushButtonPanel.add(sResetButton, gbc);
    pushButtonPanel.add(sPushButton, gbc);
  }

  private void setButtonState(boolean state) {
    sPushButton.setEnabled(state);
    sResetButton.setEnabled(state);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (evt.getNewValue() != null && evt.getPropertyName().equals(SuitcaseSwingWorker.DONE_PROPERTY)) {
      setButtonState(true);
      sPushButton.setText(PUSH_LABEL);
      sResetButton.setText(RESET_LABEL);
    }
  }
}
