package org.openhab.binding.csas.internal.model.response;

import org.openhab.binding.csas.internal.model.CSASAccount;

import java.util.ArrayList;

public class CSASAccountsResponse {
    private ArrayList<CSASAccount> accounts;

    public ArrayList<CSASAccount> getAccounts() {
        return accounts;
    }
}
