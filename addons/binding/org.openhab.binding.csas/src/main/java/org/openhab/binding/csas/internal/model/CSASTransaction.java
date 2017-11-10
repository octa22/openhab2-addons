/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.csas.internal.model;

/**
 * The {@link CSASTransaction} is represents the model of the
 * CSAS account transaction.
 *
 * @author Ondrej Pecta - Initial contribution
 */
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
