package org.opendatakit.suitcase.ui;

import org.opendatakit.suitcase.utils.ButtonAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class DropdownPanel extends JPanel{
    private GridBagConstraints gbc;
    private ActionListener addActionListener;
    private JComboBox dropdown;

    public DropdownPanel(String label, JComboBox dropdown, ActionListener addActionListener) {
        super(new FlowLayout(FlowLayout.LEFT));  // flow layout with left alignment of elements

        this.gbc = LayoutDefault.getDefaultGbc();
        this.gbc.gridx = 0;
        this.gbc.gridy = GridBagConstraints.RELATIVE;
        this.addActionListener=addActionListener;   // action listener for the add button
        this.dropdown=dropdown;

        buildLabelPanel(label);
        buildDropDown();

    }

    // build label on the left of the dropdown
    private void buildLabelPanel(String label) {
        JPanel labelPanel = new JPanel(new GridLayout(1, 1));

        labelPanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,20));

        this.add(labelPanel);
        labelPanel.add(new JLabel(label));
    }

    // build the drop down of table ids with an add button
    public void buildDropDown() {
        JPanel inputPanel = new JPanel(new GridLayout(1,2));

        inputPanel.add(dropdown);
        JButton addButton = new JButton();
        addButton.setActionCommand(ButtonAction.ADD.getStringValueOfAction());
        addButton.setText(ButtonAction.ADD.getStringValueOfAction());
        addButton.addActionListener(addActionListener);

        inputPanel.add(addButton);

        this.add(inputPanel);
    }
}
