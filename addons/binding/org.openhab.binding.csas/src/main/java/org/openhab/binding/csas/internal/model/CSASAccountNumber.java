package org.openhab.binding.csas.internal.model;

import com.google.gson.annotations.SerializedName;

public class CSASAccountNumber {
    private String number;
    private String bankCode;

    @SerializedName("cz-iban")
    private String iban;

    public String getNumber() {
        return number;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getIban() {
        return iban;
    }

    public String getFullAccount() {
        return number + "/" + bankCode;
    }
}
