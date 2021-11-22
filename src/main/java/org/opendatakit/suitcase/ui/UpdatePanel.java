package org.opendatakit.suitcase.ui;

import org.opendatakit.suitcase.net.SuitcaseSwingWorker;
import org.opendatakit.suitcase.net.UpdateTask;
import org.opendatakit.suitcase.utils.ButtonState;
import org.opendatakit.suitcase.utils.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Set;

public class UpdatePanel extends JPanel implements PropertyChangeListener {
    private static final String UPDATE_LABEL = "Update Table";
    private static final String CSV_PATH_LABEL = "CSV Path";
    private static final String FILE_CHOOSER_LABEL = "Select";
    private static final String UPDATE_LOG_PATH_OPT = "/updateLogPath";
    private static final String DEFAULT_DATA_VERSION = "2";

    private JButton sUpdateButton;
    private PathChooserPanel pathChooser;
    private JComboBox<String> sTableIdDropdownForUpdate;
    // other instance vars
    private IOPanel parent;
    private MutableComboBoxModel<String> updateComboBoxModel;

    public UpdatePanel(IOPanel parent) {
        super(new GridBagLayout());

        this.parent = parent;
        this.sUpdateButton = new JButton();
        this.sTableIdDropdownForUpdate = new JComboBox<>();
        this.pathChooser = new PathChooserPanel(
                CSV_PATH_LABEL, FILE_CHOOSER_LABEL, FileUtils.getDefaultSavePath().toString() , JFileChooser.FILES_ONLY
        );
        this.sUpdateButton.setBackground(LayoutConsts.BUTTON_BACKGROUND_COLOR);
        this.sUpdateButton.setForeground(LayoutConsts.BUTTON_FOREGROUND_COLOR);
        GridBagConstraints gbc = LayoutDefault.getDefaultGbc();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;

        updateComboBoxModel = new DefaultComboBoxModel<>();
        sTableIdDropdownForUpdate.setModel(updateComboBoxModel);
        buildDropDown(sTableIdDropdownForUpdate, "Select Table ID to Update",gbc);


        gbc.insets = new Insets(0,0,40,20);
        gbc.weighty = 0.5;
        this.add(this.pathChooser, gbc);

        JPanel tableUpdateButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));

        gbc.insets = new Insets(0,0,80,20);
        buildUpdateButtonArea(tableUpdateButtonPanel, gbc);
    }

    private void buildDropDown(JComboBox<String> dropDown, String label,GridBagConstraints gbc) {
        JPanel dropDownPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,15,0));
        dropDown.setMaximumSize(new Dimension(200,25));
        dropDown.setMinimumSize(new Dimension(150,25));
        dropDown.setPreferredSize(new Dimension(200,25));
        dropDown.setUI(new DropdownPanel.ComboBoxUI());
        dropDown.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        dropDownPanel.add(new JLabel(label));
        dropDownPanel.add(dropDown);
        dropDownPanel.setBorder(BorderFactory.createEmptyBorder(100,0,0,0));
        gbc.weighty=1;

        this.add(dropDownPanel,gbc);
    }

    public void setButtonState(ButtonState updateButtonState) {
        sUpdateButton.setEnabled(updateButtonState.getButtonStateBooleanValue());
    }

    private void buildUpdateButtonArea(JPanel tableOptionsPanel, GridBagConstraints gbc) {
        sUpdateButton.setText(UPDATE_LABEL);
        sUpdateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parent.disableAllButtons();
                String outcomePath = new File("").getAbsolutePath()+UPDATE_LOG_PATH_OPT;
                UpdateTask worker = new UpdateTask(parent.getCloudEndpointInfo(), pathChooser.getPath(),DEFAULT_DATA_VERSION, (String) updateComboBoxModel.getSelectedItem(),outcomePath,true);
                worker.addPropertyChangeListener(parent.getProgressBar());
                worker.addPropertyChangeListener(UpdatePanel.this);
                worker.execute();
            }
        });
        tableOptionsPanel.add(sUpdateButton);
        this.add(tableOptionsPanel,gbc);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() != null && evt.getPropertyName().equals(SuitcaseSwingWorker.DONE_PROPERTY)) {
            // re-enable download button and restore its label
            parent.enableAllButtons();
            ((DefaultComboBoxModel) updateComboBoxModel).removeAllElements();
            final Set<String> allTableIds = parent.getCloudEndpointInfo().getAllTableId();
            for (String s : allTableIds) {
                updateComboBoxModel.addElement(s);
            }
        }
    }
}
