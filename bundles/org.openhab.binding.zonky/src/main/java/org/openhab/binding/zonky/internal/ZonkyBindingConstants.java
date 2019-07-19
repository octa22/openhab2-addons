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
package org.openhab.binding.zonky.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link ZonkyBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class ZonkyBindingConstants {

    private static final String BINDING_ID = "zonky";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_ACCOUNT = new ThingTypeUID(BINDING_ID, "account");

    // List of all Channel ids
    public static final String BALANCE = "balance";
    public static final String AVAILABLE_BALANCE = "availableBalance";
    public static final String BLOCKED_BALANCE = "blockedBalance";
    public static final String CREDIT_SUM = "creditSum";
    public static final String DEBIT_SUM = "debitSum";
    public static final String CURRENT_PROFITABILITY = "currentProfitability";
    public static final String EXPECTED_PROFITABILITY = "expectedProfitability";
    public static final String FEES_AMOUNT = "feesAmount";
    public static final String FEES_DISCOUNT = "feesDiscount";
    public static final String INVESTMENT_COUNT = "investmentCount";
    public static final String NET_INCOME = "netIncome";
    public static final String TOTAL_INVESTMENT = "totalInvestment";
    public static final String INTEREST_PAID = "interestPaid";
    public static final String PENALTY_PAID = "penaltyPaid";
    public static final String INTEREST_LEFT = "interestLeft";
    public static final String INTEREST_LEFT_DUE = "interestLeftDue";
    public static final String INTEREST_LEFT_TO_PAY = "interestLeftToPay";
    public static final String INTEREST_PLANNED = "interestPlanned";
    public static final String PRINCIPAL_LOST = "principalLost";
    public static final String PRINCIPAL_PAID = "principalPaid";
    public static final String PRINCIPAL_LEFT = "principalLeft";
    public static final String PRINCIPAL_LEFT_DUE = "principalLeftDue";
    public static final String PRINCIPAL_LEFT_TO_PAY = "principalLeftToPay";
    public static final String NEW_INVESTMENTS = "newInvestments";
    public static final String NEW_INVESTMENTS_AMOUNT = "newInvestmentsAmount";
    public static final String PAID_INSTALMENTS = "paidInstalments";
    public static final String PAID_INSTALMENTS_AMOUNT = "paidInstalmentsAmount";
    public static final String SOLD_INVESTMENTS = "soldInvestments";
    public static final String SOLD_INVESTMENTS_AMOUNT = "soldInvestmentsAmount";
    public static final String BOUGHT_INVESTMENTS = "boughtInvestments";
    public static final String BOUGHT_INVESTMENTS_AMOUNT = "boughtInvestmentsAmount";

    // Channel groups
    public static final String STATISTICS = "statistics#";
    public static final String CURRENT_OVERVIEW = "currentOverview#";
    public static final String OVERALL_OVERVIEW = "overallOverview#";
    public static final String WALLET = "wallet#";
    public static final String WEEKLY_STATS = "weeklyStats#";

    // Other constants
    public static final String ZONKY_URL = "https://api.zonky.cz/";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.59 Safari/537.36";


}
