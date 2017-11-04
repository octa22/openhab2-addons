package org.openhab.binding.csas.internal.model;

import com.google.gson.annotations.SerializedName;

public class CSASReservation {
    private String creationDate;
    private CSASAmount amount;
    private String description;
    private String merchantName;

    @SerializedName("cz-merchantAddress")
    private String merchantAddress;

    public String getCreationDate() {
        return creationDate;
    }

    public CSASAmount getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getMerchantAddress() {
        return merchantAddress;
    }

    public String getMerchantName() {
        return merchantName;
    }
}
