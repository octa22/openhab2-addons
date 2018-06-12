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
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link MintosBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class MintosBindingConstants {

    private static final String BINDING_ID = "mintos";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_ACCOUNT = new ThingTypeUID(BINDING_ID, "account");

    // List of all Channel ids
    public static final String ACCOUNT_BALANCE = "account_balance";
    public static final String AVAILABLE_FUNDS = "available_funds";
    public static final String INVESTED_FUNDS = "invested_funds";
    public static final String NET_ANNUAL_RETURN = "net_annual_return";
    public static final String BAD_DEBT = "bad_debt";
    public static final String LATE_PAYMENT_FEES = "late_payment_fees";
    public static final String INTEREST = "interest";
    public static final String SEC_MARKET_TRANSACTIONS = "sec_market_transactions";
    public static final String SERVICE_FEES = "service_fees";
    public static final String CAMPAIGN_REWARDS = "campaign_rewards";
    public static final String TOTAL_PROFIT = "total_profit";

    // Other
    public static final String AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.117 Safari/537.36";
    public static final String LOGOUT_URL = "<a href=\"https://www.mintos.com/en/logout?";

    //indexes
    final static String TOOLTIP_INDEX = "</i></td><td>";
    final static String OVERVIEW_INDEX = "<div class=\"value\">";
    final static String OVERVIEW_TAB_INDEX = "</td><td>";
    final static String END_INDEX = "</td>";
    final static String END_DIV_INDEX = "</div>";
}
