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

public class ZonkyCurrentOverview {
    private Number totalInvestment;
    private Number principalPaid;
    private Number interestPaid;
    private Number penaltyPaid;
    private Number investmentCount;
    private Number principalLeft;
    private Number principalLeftToPay;
    private Number principalLeftDue;
    private Number interestPlanned;
    private Number interestLeft;
    private Number interestLeftToPay;
    private Number interestLeftDue;

    public Number getTotalInvestment() {
        return totalInvestment;
    }

    public Number getPrincipalPaid() {
        return principalPaid;
    }

    public Number getInterestPaid() {
        return interestPaid;
    }

    public Number getPenaltyPaid() {
        return penaltyPaid;
    }

    public Number getInvestmentCount() {
        return investmentCount;
    }

    public Number getPrincipalLeft() {
        return principalLeft;
    }

    public Number getPrincipalLeftToPay() {
        return principalLeftToPay;
    }

    public Number getPrincipalLeftDue() {
        return principalLeftDue;
    }

    public Number getInterestPlanned() {
        return interestPlanned;
    }

    public Number getInterestLeft() {
        return interestLeft;
    }

    public Number getInterestLeftToPay() {
        return interestLeftToPay;
    }

    public Number getInterestLeftDue() {
        return interestLeftDue;
    }
}
