package org.openhab.binding.csas.internal.model;

public class CSASTransaction {
    private String bookingDate;
    private CSASAmount amount;
    private String description;
    private String variableSymbol;
    private CSASAccountParty accountParty;

    public String getBookingDate() {
        return bookingDate;
    }

    public CSASAmount getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getVariableSymbol() {
        return variableSymbol;
    }

    public CSASAccountParty getAccountParty() {
        return accountParty;
    }
}
