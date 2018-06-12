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
package org.openhab.binding.mintos.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link MintosAccountOverview} class holds model for overview of the
 * Mintos account.
 *
 * @author Ondrej Pecta - Initial contribution
 */

@NonNullByDefault
public class MintosAccountOverview {
    private double accountBalance;
    private double investedFunds;
    private double availableFunds;
    private double netAnnualReturn;
    private double interest;
    private double latePaymentFees;
    private double badDebt;
    private double secMarketTransactions;
    private double serviceFees;
    private double campaignRewards;
    private double totalProfit;

    public MintosAccountOverview() {
        this.accountBalance = -1;
        this.investedFunds = -1;
        this.availableFunds = -1;
        this.netAnnualReturn = -1;
        this.interest = -1;
        this.latePaymentFees = -1;
        this.badDebt = -1;
        this.secMarketTransactions = -1;
    }


    public double getAccountBalance() {
        return accountBalance;
    }

    public double getNetAnnualReturn() {
        return netAnnualReturn;
    }

    public double getInvestedFunds() {
        return investedFunds;
    }

    public double getAvailableFunds() {
        return availableFunds;
    }

    public double getInterest() {
        return interest;
    }

    public double getLatePaymentFees() {
        return latePaymentFees;
    }

    public double getBadDebt() {
        return badDebt;
    }

    public double getSecMarketTransactions() {
        return secMarketTransactions;
    }

    public double getServiceFees() {
        return serviceFees;
    }

    public double getCampaignRewards() {
        return campaignRewards;
    }

    public double getTotalProfit() {
        return totalProfit;
    }

    public void setAccountBalance(double accountBalance) {
        this.accountBalance = accountBalance;
    }

    public void setInvestedFunds(double investedFunds) {
        this.investedFunds = investedFunds;
    }

    public void setAvailableFunds(double availableFunds) {
        this.availableFunds = availableFunds;
    }

    public void setNetAnnualReturn(double netAnnualReturn) {
        this.netAnnualReturn = netAnnualReturn;
    }

    public void setInterest(double interest) {
        this.interest = interest;
    }

    public void setLatePaymentFees(double latePaymentFees) {
        this.latePaymentFees = latePaymentFees;
    }

    public void setBadDebt(double badDebt) {
        this.badDebt = badDebt;
    }

    public void setSecMarketTransactions(double secMarketTransactions) {
        this.secMarketTransactions = secMarketTransactions;
    }

    public void setServiceFees(double serviceFees) {
        this.serviceFees = serviceFees;
    }

    public void setCampaignRewards(double campaignRewards) {
        this.campaignRewards = campaignRewards;
    }

    public void setTotalProfit(double totalProfit) {
        this.totalProfit = totalProfit;
    }
}
