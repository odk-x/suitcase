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
    private DefaultComboBoxModel<String> comboBoxModel;
    private AttachmentManager attachMngr;
    private ODKCsv csv;
    private ArrayList<String> selectedTableIds;

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
            for(int i=0;i<selectedTableIds.size();i++) {
                if(selectedTableIds.get(i).equals(((RemoveButton)e.getSource()).getTableId()))    //finds the selected table id in the list
                {
                    selectedTablesListPanel.remove(i);                         // removes the component from UI
                    comboBoxModel.addElement(selectedTableIds.get(i));         // Add element back to dropdown
                    selectedTableIds.remove(i);                                // remove from the list of selected table ids
                    break;
                }

            }
            selectedTablesListPanel.updateSize(selectedTableIds.size());       // update the height of the JPanel to fit inside JScrollPane
            selectedTablesListPanel.revalidate();                              // Call to revalidate and repaint the UI
            selectedTablesListPanel.repaint();
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

        JScrollPane tableIdsScrollPane = new JScrollPane(selectedTablesListPanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,   //Make JScrollPane scroll only vertically
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        tableIdsScrollPane.setMinimumSize(new Dimension(350,100));
        tableIdsScrollPane.setPreferredSize(new Dimension(350,100));
        tableIdsScrollPane.setMaximumSize(new Dimension(350,100));
        tableIdsScrollPane.setSize(new Dimension(350,100));
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
                parent.setButtonsState(ButtonState.DISABLED,ButtonState.DISABLED,ButtonState.DISABLED,ButtonState.DISABLED, ButtonState.DISABLED);
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
                    parent.setButtonsState(ButtonState.DISABLED, ButtonState.DISABLED, ButtonState.DISABLED, ButtonState.DISABLED, ButtonState.DISABLED);

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

    public void setButtonsState(ButtonState pullButtonState,ButtonState refreshButtonState,ButtonState addButtonState) {
        sPullButton.setEnabled(pullButtonState.getButtonStateBooleanValue());
        sRefreshButton.setEnabled(refreshButtonState.getButtonStateBooleanValue());
        pullDropdown.setButtonsState(addButtonState);
    }

    private static String[] getAllTableIds(CloudEndpointInfo cloudEndpointInfo) {
        String[] tableIds;
        if (cloudEndpointInfo != null) {
            tableIds = cloudEndpointInfo.getAllTableId().toArray(new String[0]);
        } else {
            tableIds = new String[]{};
        }
        return tableIds;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() != null && evt.getPropertyName().equals(SuitcaseSwingWorker.DONE_PROPERTY)) {
            // re-enable download button and restore its label
            sPullButton.setText(DOWNLOAD_LABEL);
            parent.setButtonsState(ButtonState.ENABLED, ButtonState.ENABLED, ButtonState.ENABLED,ButtonState.ENABLED, ButtonState.ENABLED);
            comboBoxModel.removeAllElements();
            final String[] allTableIds = getAllTableIds(parent.getCloudEndpointInfo());
            for (String s : allTableIds) {
                comboBoxModel.addElement(s);
            }
        }
    }
}
