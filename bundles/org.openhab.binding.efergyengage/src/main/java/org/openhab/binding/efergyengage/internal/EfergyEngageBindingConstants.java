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
package org.openhab.binding.efergyengage.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link EfergyEngageBindingConstants} class defines common constants, which are
 * used across the whole binding.
 * 
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class EfergyEngageBindingConstants {

    public static final String BINDING_ID = "efergyengage";

    // List of all Thing Type UIDs
    public static final ThingTypeUID EFERGY_ENGAGE_HUB = new ThingTypeUID(BINDING_ID, "hub");
    public static final ThingTypeUID EFERGY_ENGAGE_SENSOR = new ThingTypeUID(BINDING_ID, "sensor");

    // List of all Channel ids
    // hub
    public static final String CHANNEL_ESTIMATE = "estimate";
    public static final String CHANNEL_COST = "cost";
    public static final String CHANNEL_DAYTOTAL = "daytotal";
    public static final String CHANNEL_WEEKTOTAL = "weektotal";
    public static final String CHANNEL_MONTHTOTAL = "monthtotal";
    public static final String CHANNEL_YEARTOTAL = "yeartotal";

    // sensor
    public static final String CHANNEL_INSTANT = "instant";
    public static final String CHANNEL_LAST_MEASUREMENT = "last_measurement";

    // other constants
    public static final String EFERGY_URL = "https://engage.efergy.com/mobile_proxy/";
    public static final String DAY = "day";
    public static final String WEEK = "week";
    public static final String MONTH = "month";
    public static final String YEAR = "year";
    public static final String PWER = "PWER";

    public static final int CACHE_EXPIRY = 5000;
    public static final int READ_TIMEOUT = 10000;
}
