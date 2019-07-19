/**
 * Copyright (c) 2014,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.zonky.internal.model;

public class ZonkyWalletResponse {
    private int id;
    private Number balance;
    private Number availableBalance;
    private Number blockedBalance;
    private Number creditSum;
    private Number debitSum;
    private String variableSymbol;
    private ZonkyAccount account;

    public int getId() {
        return id;
    }

    public Number getBalance() {
        return balance;
    }

    public Number getAvailableBalance() {
        return availableBalance;
    }

    public Number getBlockedBalance() {
        return blockedBalance;
    }

    public Number getCreditSum() {
        return creditSum;
    }

    public Number getDebitSum() {
        return debitSum;
    }

    public String getVariableSymbol() {
        return variableSymbol;
    }

    public ZonkyAccount getAccount() {
        return account;
    }
}
