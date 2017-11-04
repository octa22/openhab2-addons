package org.openhab.binding.csas.internal.model.response;

import org.openhab.binding.csas.internal.model.CSASSecuritiesAccount;

import java.util.ArrayList;

public class CSASSecuritiesResponse {
    private ArrayList<CSASSecuritiesAccount> securitiesAccounts;

    public ArrayList<CSASSecuritiesAccount> getSecuritiesAccounts() {
        return securitiesAccounts;
    }
}
