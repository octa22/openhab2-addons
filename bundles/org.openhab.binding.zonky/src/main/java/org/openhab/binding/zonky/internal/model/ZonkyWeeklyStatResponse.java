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

public class ZonkyWeeklyStatResponse {
    private int newInvestments;
    private Number newInvestmentsAmount;
    private int paidInstalments;
    private Number paidInstalmentsAmount;
    private int soldInvestments;
    private Number soldInvestmentsAmount;
    private int boughtInvestments;
    private Number boughtInvestmentsAmount;

    public int getNewInvestments() {
        return newInvestments;
    }

    public Number getNewInvestmentsAmount() {
        return newInvestmentsAmount;
    }

    public int getPaidInstalments() {
        return paidInstalments;
    }

    public Number getPaidInstalmentsAmount() {
        return paidInstalmentsAmount;
    }

    public int getSoldInvestments() {
        return soldInvestments;
    }

    public Number getSoldInvestmentsAmount() {
        return soldInvestmentsAmount;
    }

    public int getBoughtInvestments() {
        return boughtInvestments;
    }

    public Number getBoughtInvestmentsAmount() {
        return boughtInvestmentsAmount;
    }
}
