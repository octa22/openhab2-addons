package org.openhab.binding.csas.internal.model;

public class CSASCard {
    private CSASAccount mainAccount;
    private String type;
    private String state;

    public CSASAccount getMainAccount() {
        return mainAccount;
    }

    public String getType() {
        return type;
    }

    public String getState() {
        return state;
    }
}
