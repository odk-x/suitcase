package org.opendatakit.suitcase.ui;

import org.opendatakit.suitcase.utils.ButtonAction;

import javax.swing.*;
import java.awt.*;

public class RemoveButton extends JButton {
    private final String tableId;
    private static final String REMOVE_BUTTON_LABEL="Remove";

    public RemoveButton(String tableId) {
        super();
        this.tableId = tableId;
        this.setActionCommand(ButtonAction.REMOVE.getStringValueOfAction());
        this.setText(REMOVE_BUTTON_LABEL);
        this.setSize(LayoutConsts.ADD_AND_REMOVE_BUTTON_DIMENSION);
    }

    public String getTableId() {
        return tableId;
    }
}
