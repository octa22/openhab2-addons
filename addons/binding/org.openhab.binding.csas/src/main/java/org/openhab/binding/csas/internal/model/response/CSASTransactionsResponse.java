package org.openhab.binding.csas.internal.model.response;

import org.openhab.binding.csas.internal.model.CSASTransaction;

import java.util.ArrayList;

public class CSASTransactionsResponse {
    private ArrayList<CSASTransaction> transactions;

    public ArrayList<CSASTransaction> getTransactions() {
        return transactions;
    }
}
