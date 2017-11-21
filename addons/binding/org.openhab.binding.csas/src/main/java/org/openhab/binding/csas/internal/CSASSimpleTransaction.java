/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.csas.internal;

/**
 * Created by Ondřej Pečta on 22. 11. 2016.
 */
public class CSASSimpleTransaction {
    private String balance = "-";
    private String accountPartyDescription = "";
    private String accountPartyInfo = "";
    private String description = "";
    private String variableSymbol = "";

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getAccountPartyDescription() {
        return accountPartyDescription;
    }

    public void setAccountPartyDescription(String accountPartyDescription) {
        this.accountPartyDescription = accountPartyDescription;
    }

    public String getAccountPartyInfo() {
        return accountPartyInfo;
    }

    public void setAccountPartyInfo(String accountPartyInfo) {
        this.accountPartyInfo = accountPartyInfo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVariableSymbol() {
        return variableSymbol;
    }

    public void setVariableSymbol(String variableSymbol) {
        this.variableSymbol = variableSymbol;
    }

    @Override
    public String toString() {
        return balance + " " + accountPartyInfo + " " + accountPartyDescription + " " + description + " " + variableSymbol;
    }
}
