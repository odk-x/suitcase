package org.opendatakit.suitcase.ui;

import org.opendatakit.suitcase.net.DeleteTask;
import org.opendatakit.suitcase.net.ResetTask;
import org.opendatakit.suitcase.net.SuitcaseSwingWorker;
import org.opendatakit.suitcase.net.UpdateTask;
import org.opendatakit.suitcase.utils.ButtonState;
import org.opendatakit.suitcase.utils.FieldsValidatorUtils;
import org.opendatakit.suitcase.utils.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Set;

public class ClearPanel extends JPanel implements PropertyChangeListener {
    private static final String DELETE_LABEL = "Delete Table";
    private static final String RESET_LABEL = "Reset Server";
    private static final String DEFAULT_DATA_VERSION = "2";
    private static final String RESETTING_LABEL = "Resetting";

    private JButton sDeleteButton;
    private JComboBox<String> sTableIdDropdownForDelete;
    private JButton sResetButton;
    private IOPanel parent;
    private MutableComboBoxModel<String> deleteComboBoxModel;

    public ClearPanel(IOPanel parent) {
        super(new GridBagLayout());
        this.parent = parent;
        this.sDeleteButton = new JButton();
        this.sResetButton = new JButton();
        this.sTableIdDropdownForDelete = new JComboBox<>();

        GridBagConstraints gbc = LayoutDefault.getDefaultGbc();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;

        deleteComboBoxModel = new DefaultComboBoxModel<>();
        gbc.insets = new Insets(20, 0, 0, 0);
        buildDropDown(
                sTableIdDropdownForDelete,
                "Select Table ID to Delete",
                gbc
        );
        sTableIdDropdownForDelete.setModel(deleteComboBoxModel);

        JPanel tableDeleteButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));

        gbc.insets = new Insets(0,0,0,20);
        buildDeleteButtonArea(tableDeleteButtonPanel, gbc);
        JPanel tableResetButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        buildResetButtonArea(tableResetButtonPanel,gbc);
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
        dropDownPanel.setBorder(BorderFactory.createEmptyBorder(20,0,0,0));
        gbc.weighty=1;

        this.add(dropDownPanel,gbc);
    }

    public void setButtonState(ButtonState resetButtonState, ButtonState deleteButtonState) {
        sResetButton.setEnabled(resetButtonState.getButtonStateBooleanValue());
        sDeleteButton.setEnabled(deleteButtonState.getButtonStateBooleanValue());
    }

    private void buildDeleteButtonArea(JPanel tableOptionsPanel, GridBagConstraints gbc) {
        sDeleteButton.setText(DELETE_LABEL);

        this.sDeleteButton.setBackground(LayoutConsts.BUTTON_BACKGROUND_COLOR);
        this.sDeleteButton.setForeground(LayoutConsts.BUTTON_FOREGROUND_COLOR);
        sDeleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(DialogUtils.promptConfirm("Are you sure you want to delete the selected table?",true,false)) {
                    parent.disableAllButtons();
                    DeleteTask worker = new DeleteTask((String) deleteComboBoxModel.getSelectedItem(), DEFAULT_DATA_VERSION);
                    worker.addPropertyChangeListener(parent.getProgressBar());
                    worker.addPropertyChangeListener(parent.getUpdatePanel());
                    worker.addPropertyChangeListener(parent.getPullPanel());
                    worker.addPropertyChangeListener(ClearPanel.this);
                    worker.execute();
                }
            }
        });
        tableOptionsPanel.add(sDeleteButton);
        this.add(tableOptionsPanel,gbc);
    }

    private void buildResetButtonArea(JPanel tableOptionsPanel, GridBagConstraints gbc){

        sResetButton.setText(RESET_LABEL);
        sResetButton.setName("reset_button");
        sResetButton.setBackground(LayoutConsts.WARNING_BACKGROUND_COLOR);
        sResetButton.setForeground(LayoutConsts.BUTTON_FOREGROUND_COLOR);
        sResetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if(DialogUtils.promptConfirm("Are you sure you want to RESET? "
                            + "This will delete ALL your data on the server?", true, false)) {
                        sResetButton.setText(RESETTING_LABEL);
                        parent.disableAllButtons();

                        ResetTask worker = new ResetTask(DEFAULT_DATA_VERSION, true);
                        worker.addPropertyChangeListener(parent.getProgressBar());
                        worker.addPropertyChangeListener(parent.getPullPanel());
                        worker.addPropertyChangeListener(parent.getUpdatePanel());
                        worker.addPropertyChangeListener(ClearPanel.this);
                        worker.execute();
                    }
            }
        });
        gbc.insets = new Insets(10,20,0,20);
        JLabel resetLabel = new JLabel("Reset All Data in the Server");
        this.add(resetLabel,gbc);
        tableOptionsPanel.add(sResetButton);
        this.add(tableOptionsPanel,gbc);
        gbc.insets = new Insets(10,0,0,20);
        this.add(tableOptionsPanel,gbc);

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() != null && evt.getPropertyName().equals(SuitcaseSwingWorker.DONE_PROPERTY)) {
            // re-enable download button and restore its label
            parent.enableAllButtons();
            sResetButton.setText(RESET_LABEL);
            ((DefaultComboBoxModel) deleteComboBoxModel).removeAllElements();
            final Set<String> allTableIds = parent.getCloudEndpointInfo().getAllTableId();
            for (String s : allTableIds) {
                deleteComboBoxModel.addElement(s);
            }
        }
    }
}
