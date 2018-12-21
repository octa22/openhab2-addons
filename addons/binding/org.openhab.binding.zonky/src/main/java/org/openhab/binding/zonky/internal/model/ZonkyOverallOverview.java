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

public class ZonkyOverallOverview {
    private Number totalInvestment;
    private Number principalPaid;
    private Number interestPaid;
    private Number penaltyPaid;
    private Number investmentCount;
    private Number feesAmount;
    private Number feesDiscount;
    private Number netIncome;
    private Number principalLost;

    public Number getTotalInvestment() {
        return totalInvestment;
    }

    public Number getPrincipalPaid() {
        return principalPaid;
    }

    public Number getInterestPaid() {
        return interestPaid;
    }

    public Number getInvestmentCount() {
        return investmentCount;
    }

    public Number getFeesAmount() {
        return feesAmount;
    }

    public Number getNetIncome() {
        return netIncome;
    }

    public Number getPrincipalLost() {
        return principalLost;
    }

    public Number getFeesDiscount() {
        return feesDiscount;
    }

    public Number getPenaltyPaid() {
        return penaltyPaid;
    }
}
