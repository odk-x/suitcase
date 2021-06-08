package org.opendatakit.suitcase.utils;

public enum ButtonAction {
    REMOVE("Remove"),
    ADD("Add");
    String action;

    ButtonAction(String s) {
        this.action = s;
    }

    public String getStringValueOfAction()
    {
        return this.action;
    }
}
