package org.opendatakit.suitcase.utils;

public enum ButtonState {
    ENABLED(true),
    DISABLED(false);

    boolean state;

    public boolean getButtonStateBooleanValue() {
        return this.state;
    }

    ButtonState(boolean b) {
        this.state = b;
    }
}
