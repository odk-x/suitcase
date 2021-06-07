package org.opendatakit.suitcase.utils;

public enum ButtonAction {
    REMOVE("remove"),
    ADD("add");
    String action;

    ButtonAction(String s) {
        this.action = s;
    }

    public String getStringValueOfAction()
    {
        return this.action;
    }
}
