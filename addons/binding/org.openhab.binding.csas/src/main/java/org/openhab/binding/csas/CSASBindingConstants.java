/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.csas;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link CSASBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Ondrej Pecta - Initial contribution
 */
 @NonNullByDefault
public class CSASBindingConstants {

    private static final String BINDING_ID = "csas";

    // Bridge
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");

    // Account
    public static final ThingTypeUID THING_TYPE_ACCOUNT = new ThingTypeUID(BINDING_ID, "account");

    // Card account
    public static final ThingTypeUID THING_TYPE_CARD_ACCOUNT = new ThingTypeUID(BINDING_ID, "card_account");

    // Building saving account
    public static final ThingTypeUID THING_TYPE_BS_ACCOUNT = new ThingTypeUID(BINDING_ID, "bs_account");

    // Securities account
    public static final ThingTypeUID THING_TYPE_SECURITIES_ACCOUNT = new ThingTypeUID(BINDING_ID, "securities_account");

    // Pension contract
    public static final ThingTypeUID THING_TYPE_PENSION_CONTRACT = new ThingTypeUID(BINDING_ID, "pension_contract");

    // Insurance contract
    public static final ThingTypeUID THING_TYPE_INSURANCE_CONTRACT = new ThingTypeUID(BINDING_ID, "insurance_contract");

    // Loyalty contract
    public static final ThingTypeUID THING_TYPE_LOYALTY_CONTRACT = new ThingTypeUID(BINDING_ID, "loyalty_contract");

    // List of all Channel ids
    public static final String CHANNEL_POINTS = "points";
    public static final String CHANNEL_CURRENCY = "currency";
    public static final String CHANNEL_BALANCE = "balance";
    public static final String CHANNEL_BALANCE_FULL = "balance_full";
    public static final String CHANNEL_DISPOSABLE = "disposable";
    public static final String CHANNEL_DISPOSABLE_FULL = "disposable_full";

    //public static final String CHANNEL_TRAN2 = "tran2";

    //OTHER
    public static final String NETBANKING_V3 = "https://www.csas.cz/webapi/api/v3/netbanking/";
    public static final String CREDIT = "CREDIT";
    public static final String REGISTERED = "REGISTERED";
    public static final String ACTIVE = "ACTIVE";
    public static final String IBOD = "IBOD";
    public static final String TRAN = "tran";
    public static final int DISCOVERY_TIMEOUT_SEC = 10;
    public static final int CACHE_EXPIRY = 10 * 1000;
    public static final int HISTORY_INTERVAL = 14;
}
