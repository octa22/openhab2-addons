/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.goodwe.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link GoodWeBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class GoodWeBindingConstants {

    private static final String BINDING_ID = "goodwe";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_POWERSTATION = new ThingTypeUID(BINDING_ID, "powerstation");

    // List of all Channel ids
    public static final String CHANNEL_CAPACITY = "capacity";
    public static final String CHANNEL_BATTERY_CAPACITY = "battery_capacity";
    public static final String CHANNEL_PAC = "pac";
    public static final String CHANNEL_POWER = "power";
    public static final String CHANNEL_TOTAL_POWER = "total_power";
    public static final String CHANNEL_DAY_INCOME = "day_income";
    public static final String CHANNEL_TOTAL_INCOME = "total_income";
    public static final String CHANNEL_SUM = "sum";
    public static final String CHANNEL_BUY = "buy";
    public static final String CHANNEL_BUY_PERCENT = "buy_percent";
    public static final String CHANNEL_SELL = "sell";
    public static final String CHANNEL_SELL_PERCENT = "sell_percent";
    public static final String CHANNEL_SELF_USE_OF_PV = "self_use_of_pv";
    public static final String CHANNEL_CONSUMPTION_OF_LOAD = "consumption_of_load";

    // supported thing types for discovery
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>(
            Arrays.asList(THING_TYPE_POWERSTATION));

    // Other contants
    public static final String SEMS_PORTAL_API_URL = "https://eu.semsportal.com/api/";
    public static final String GOODWE_AGENT = "PVMaster/2.0.4 (iPhone; iOS 11.4.1; Scale/2.00)";
    public static final String APPLICATION_JSON = "application/json";
    public static final String AUTHORIZATION_EXPIRED = "The authorization has expired, please login again.";
    public static final int GOODWE_TIMEOUT = 5;
}
