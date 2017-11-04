package org.openhab.binding.csas.internal.model;

public class CSASAmount {
    private String value;
    private String currency;
    private int precision;

    public String getValue() {
        return value;
    }

    public String getCurrency() {
        return currency;
    }

    public int getPrecision() {
        return precision;
    }
}
