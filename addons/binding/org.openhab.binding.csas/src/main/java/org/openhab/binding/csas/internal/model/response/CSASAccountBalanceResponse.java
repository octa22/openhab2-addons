package org.openhab.binding.csas.internal.model.response;

import org.openhab.binding.csas.internal.model.CSASAmount;

public class CSASAccountBalanceResponse {
    private CSASAmount balance;
    private CSASAmount disposable;

    public CSASAmount getBalance() {
        return balance;
    }

    public CSASAmount getDisposable() {
        return disposable;
    }
}
