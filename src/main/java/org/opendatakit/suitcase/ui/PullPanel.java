package org.opendatakit.suitcase.ui;

import org.opendatakit.suitcase.model.CloudEndpointInfo;
import org.opendatakit.suitcase.model.CsvConfig;
import org.opendatakit.suitcase.model.ODKCsv;
import org.opendatakit.suitcase.net.*;
import org.apache.wink.json4j.JSONException;
import org.opendatakit.suitcase.utils.ButtonAction;
import org.opendatakit.suitcase.utils.ButtonState;
import org.opendatakit.suitcase.utils.FieldsValidatorUtils;
import org.opendatakit.suitcase.utils.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PullPanel extends JPanel implements PropertyChangeListener {
    private static final String DOWNLOAD_LABEL = "Download";
    private static final String REFRESH_LABEL = "Refresh Tables List Metadata";
    private static final String DOWNLOADING_LABEL = "Downloading";
    private static final String SAVE_PATH_LABEL = "Save to";
    private static final String FILE_CHOOSER_LABEL = "Save";

    // ui components
    private JCheckBox sDownloadAttachment;
    private JCheckBox sApplyScanFmt;
    private JCheckBox sExtraMetadata;
    private JButton sPullButton;
    private JButton sRefreshButton;
    private PathChooserPanel savePathChooser;
    private JComboBox<String> sTableIdDropdown;
    private SelectedTablesListPanel selectedTablesListPanel;
    private DropdownPanel pullDropdown;

    // other instance vars
    private IOPanel parent;
    private MutableComboBoxModel<String> comboBoxModel;
    private AttachmentManager attachMngr;
    private ODKCsv csv;
    private List<String> selectedTableIds;
    private Dimension tableIdsScrollPaneDimension;

    public PullPanel(IOPanel parent) {
        super(new GridBagLayout());

        this.parent = parent;
        this.attachMngr = null;
        this.csv = null;
        this.selectedTableIds = new ArrayList<>();
        this.sDownloadAttachment = new JCheckBox();
        this.sApplyScanFmt = new JCheckBox();
        this.sExtraMetadata = new JCheckBox();
        this.sPullButton = new JButton();
        this.sRefreshButton = new JButton();
        this.sTableIdDropdown = new JComboBox<>();
        this.savePathChooser = new PathChooserPanel(
                SAVE_PATH_LABEL,FILE_CHOOSER_LABEL ,FileUtils.getDefaultSavePath().toString()
        );
        this.tableIdsScrollPaneDimension = new Dimension(350,100);

        GridBagConstraints gbc = LayoutDefault.getDefaultGbc();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;

        comboBoxModel = new DefaultComboBoxModel<>();
        sTableIdDropdown.setModel(comboBoxModel);
        pullDropdown = new DropdownPanel(
                "Select Table ID",
                sTableIdDropdown,
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String action = ((JButton) e.getSource()).getActionCommand();
                        if (ButtonAction.ADD.getStringValueOfAction().equals(action)&&comboBoxModel.getSelectedItem()!=null) {
                            selectedTableIds.add( (String)comboBoxModel.getSelectedItem() );
                            selectedTablesListPanel.addNewTableId((String)comboBoxModel.getSelectedItem(),selectedTableIds.size());
                            comboBoxModel.removeElement(comboBoxModel.getSelectedItem());
                        }
                    }
                }

        );
        gbc.weighty = 1;
        this.add(pullDropdown, gbc);
        selectedTablesListPanel = new SelectedTablesListPanel(e -> {         //Action listener for remove button
            removeSelectedTableId(((RemoveButton)e.getSource()).getTableId());
            }

        );
        buildSelectedTableIdsArea(selectedTablesListPanel,gbc);

        JPanel pullPrefPanel = new CheckboxPanel(
                new String[]{"Download attachments?", "Apply Scan formatting?", "Extra metadata columns?"},
                new JCheckBox[]{sDownloadAttachment, sApplyScanFmt, sExtraMetadata}, 1, 3
        );
        gbc.weighty = 3;

        gbc.insets = new Insets(0,0,0,0);
        this.add(pullPrefPanel, gbc);

        gbc.weighty = 1;
        this.add(this.savePathChooser, gbc);

        JPanel pullButtonPanel = new JPanel(new GridLayout(1, 1));
        buildPullButtonArea(pullButtonPanel);
        gbc.weighty = 2;
        gbc.insets = new Insets(10, 0, 0, 0);
        this.add(pullButtonPanel, gbc);
    }

    private void buildSelectedTableIdsArea(SelectedTablesListPanel selectedTablesListPanel,GridBagConstraints gridBagConstraints){

        gridBagConstraints.weighty = 1;
        JPanel labelPanel = new JPanel(new GridLayout(1, 1));

        labelPanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,20));
        labelPanel.add(new JLabel("Selected Table Ids for download"));

        this.add(labelPanel,gridBagConstraints);

        JScrollPane tableIdsScrollPane = new JScrollPane(selectedTablesListPanel,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,   //Make JScrollPane scroll only vertically
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        tableIdsScrollPane.setMinimumSize(tableIdsScrollPaneDimension);
        tableIdsScrollPane.setPreferredSize(tableIdsScrollPaneDimension);
        tableIdsScrollPane.setMaximumSize(tableIdsScrollPaneDimension);
        tableIdsScrollPane.setSize(tableIdsScrollPaneDimension);
        tableIdsScrollPane.setBorder(BorderFactory.createEmptyBorder(0,10,0,20));
        gridBagConstraints.weighty = 3;
        this.add(tableIdsScrollPane,gridBagConstraints);
    }

    private void buildPullButtonArea(JPanel pullButtonPanel) {
        sPullButton.setText(DOWNLOAD_LABEL);
        sRefreshButton.setText(REFRESH_LABEL);
        sRefreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parent.setButtonsState(ButtonState.DISABLED,ButtonState.DISABLED,ButtonState.DISABLED,ButtonState.DISABLED, ButtonState.DISABLED, ButtonState.DISABLED);
                RefreshTask worker = new RefreshTask();
                worker.addPropertyChangeListener(parent.getProgressBar());
                worker.addPropertyChangeListener(PullPanel.this);
                worker.execute();
            }
        });
        sPullButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String error = FieldsValidatorUtils.checkDownloadFields(
                        selectedTableIds, savePathChooser.getPath(), parent.getCloudEndpointInfo());

                if (error != null) {
                    DialogUtils.showError(error, true);
                } else {
                    // disable download button
                    parent.setButtonsState(ButtonState.DISABLED, ButtonState.DISABLED, ButtonState.DISABLED, ButtonState.DISABLED, ButtonState.DISABLED, ButtonState.DISABLED);

                    sPullButton.setText(DOWNLOADING_LABEL);

                    for(String tableId:selectedTableIds)
                    {
                     CsvConfig config = new CsvConfig(sDownloadAttachment.isSelected(), sApplyScanFmt.isSelected(), sExtraMetadata.isSelected());

                     // create a new attachment manager for every table id
                     attachMngr = new AttachmentManager(parent.getCloudEndpointInfo(), tableId, savePathChooser.getPath());

                    // create a new csv instance when csv == null or when table id changed
                    if (csv == null || !csv.getTableId().equals(tableId)) {
                        try {
                            csv = new ODKCsv(attachMngr, parent.getCloudEndpointInfo(), tableId);
                        } catch (JSONException e1) { /*should never happen*/ }
                    }

                    DownloadTask worker = new DownloadTask(parent.getCloudEndpointInfo(), csv, config, savePathChooser.getPath(), true);
                    worker.addPropertyChangeListener(parent.getProgressBar());
                    worker.addPropertyChangeListener(PullPanel.this);
                    worker.execute();
                    }
                }
            }
        });
        pullButtonPanel.add(sRefreshButton);
        pullButtonPanel.add(sPullButton);
    }

    public void setButtonsState(ButtonState pullButtonState,ButtonState refreshButtonState,ButtonState addButtonState,ButtonState removeButtonState) {
        sPullButton.setEnabled(pullButtonState.getButtonStateBooleanValue());
        sRefreshButton.setEnabled(refreshButtonState.getButtonStateBooleanValue());
        pullDropdown.setButtonsState(addButtonState);
        selectedTablesListPanel.setRemoveButtonState(removeButtonState);
    }

    private void removeSelectedTableId(String tableId) {
        for(int i=0;i<selectedTableIds.size();i++) {
            if(selectedTableIds.get(i).equals(tableId))    //finds the selected table id in the list
            {
                selectedTablesListPanel.removeTableId(i,selectedTableIds.size()-1);                         // removes the component from UI
                comboBoxModel.addElement(selectedTableIds.get(i));         // Add element back to dropdown
                selectedTableIds.remove(i);                                // remove from the list of selected table ids
                break;
            }

        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() != null && evt.getPropertyName().equals(SuitcaseSwingWorker.DONE_PROPERTY)) {
            // re-enable download button and restore its label
            sPullButton.setText(DOWNLOAD_LABEL);
            parent.setButtonsState(ButtonState.ENABLED, ButtonState.ENABLED, ButtonState.ENABLED,ButtonState.ENABLED, ButtonState.ENABLED, ButtonState.ENABLED);
            ((DefaultComboBoxModel)comboBoxModel).removeAllElements();
            final Set<String> allTableIds = parent.getCloudEndpointInfo().getAllTableId();
            for (String s : allTableIds) {
                comboBoxModel.addElement(s);
            }
        }
    }
}
