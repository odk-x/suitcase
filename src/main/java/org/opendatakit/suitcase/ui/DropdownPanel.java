package org.opendatakit.suitcase.ui;

import org.opendatakit.suitcase.utils.ButtonAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class DropdownPanel extends JPanel{
    private static final String ADD_BUTTON_LABEL = "Add";

    public DropdownPanel(String label, JComboBox<String> dropdown, ActionListener addActionListener) {
        super(new FlowLayout(FlowLayout.LEFT));  // flow layout with left alignment of elements

        buildLabelPanel(label);
        buildDropDown(dropdown,addActionListener);

    }

    // build label on the left of the dropdown
    private void buildLabelPanel(String label) {
        JPanel labelPanel = new JPanel(new GridLayout(1, 1));

        labelPanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,20));

        this.add(labelPanel);
        labelPanel.add(new JLabel(label));
    }

    // build the drop down of table ids with an add button
    private void buildDropDown(JComboBox<String> dropdown,ActionListener addActionListener) {
        JPanel inputPanel = new JPanel(new GridLayout(1,2));

        inputPanel.add(dropdown);
        JButton addButton = new JButton();
        dropdown.setMaximumSize(new Dimension(200,25));
        dropdown.setMinimumSize(new Dimension(150,25));
        dropdown.setPreferredSize(new Dimension(200,25));
        addButton.setActionCommand(ButtonAction.ADD.getStringValueOfAction());
        addButton.setText(ADD_BUTTON_LABEL);
        addButton.addActionListener(addActionListener);

        inputPanel.add(addButton);

        this.add(inputPanel);
    }
}
