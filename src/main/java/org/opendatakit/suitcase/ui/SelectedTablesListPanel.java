package org.opendatakit.suitcase.ui;

import org.opendatakit.suitcase.utils.ButtonAction;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;

public class SelectedTablesListPanel extends JPanel {
    private final ActionListener removeActionListener;

    SelectedTablesListPanel(ActionListener removeActionListener) {
        this.setLayout(new FlowLayout(FlowLayout.LEFT));
        this.setSize(new Dimension(400, 100));
        this.setMaximumSize(new Dimension(400, 1000));
        this.setPreferredSize(new Dimension(400, 150));
        this.setMinimumSize(new Dimension(400,150));
        this.removeActionListener = removeActionListener;

    }

    void addNewTableId(String tableId,int totalSelectedTableIds) {      // Method to add new Table Id in UI.

        updateSize(totalSelectedTableIds);
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel();                                    // JLabel containing the tableId
        label.setPreferredSize(new Dimension(200, 25));
        label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        label.setText(tableId);
        inputPanel.add(label);
        RemoveButton removeButton = new RemoveButton(tableId);          // Remove Button on the left side of JLabel

        removeButton.addActionListener(removeActionListener);

        inputPanel.add(removeButton);

        this.add(inputPanel);
        this.revalidate();
        this.repaint();
    }

    public void updateSize(int totalSelectedTableIds) {                                    // Method to update size of the panel inside the JScrollPane. Height increases with increasing number of elements.
        this.setPreferredSize(new Dimension(400, 25*totalSelectedTableIds));
    }
}
