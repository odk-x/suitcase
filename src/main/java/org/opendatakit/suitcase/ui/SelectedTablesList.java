package org.opendatakit.suitcase.ui;

import org.opendatakit.suitcase.utils.ButtonAction;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;

public class SelectedTablesList extends JScrollPane{
    ActionListener removeActionListener;
    SelectedTablesList(ActionListener removeActionListener) {
        this.setLayout(new ScrollPaneLayout());
        this.removeActionListener=removeActionListener;
        addNewTableId("h");
    }

    void addNewTableId(String tableId){

            JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

            JLabel label = new JLabel();
            label.setPreferredSize(new Dimension(200,25));
            label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            label.setText(tableId);
            inputPanel.add(label);
            JButton addButton = new JButton();
            addButton.setActionCommand(ButtonAction.REMOVE.getStringValueOfAction());
            addButton.setText(ButtonAction.REMOVE.getStringValueOfAction());
            addButton.setSize(new Dimension(100,25));
            addButton.addActionListener(removeActionListener);

            inputPanel.add(addButton);

            this.add(inputPanel);

    }
}
