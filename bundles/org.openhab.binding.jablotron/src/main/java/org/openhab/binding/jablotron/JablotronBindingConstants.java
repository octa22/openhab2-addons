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
package org.openhab.binding.jablotron;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link JablotronBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class JablotronBindingConstants {

    private static final String BINDING_ID = "jablotron";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_OASIS = new ThingTypeUID(BINDING_ID, "oasis");
    public static final ThingTypeUID THING_TYPE_JA100 = new ThingTypeUID(BINDING_ID, "ja100");
    public static final ThingTypeUID THING_TYPE_JA100F = new ThingTypeUID(BINDING_ID, "ja100f");

    // Common alarm channels
    public static final String CHANNEL_ALARM = "alarm";
    //public static final String CHANNEL_LAST_TROUBLE = "lastTrouble";
    //public static final String CHANNEL_LAST_TROUBLE_DETAIL = "lastTroubleDetail";
    public static final String CHANNEL_LAST_CHECK_TIME = "lastCheckTime";
    public static final String CHANNEL_LAST_EVENT = "lastEvent";
    public static final String CHANNEL_LAST_EVENT_CLASS = "lastEventClass";
    public static final String CHANNEL_LAST_EVENT_TIME = "lastEventTime";
    public static final String CHANNEL_LAST_EVENT_INVOKER = "lastEventInvoker";
    public static final String CHANNEL_LAST_EVENT_SECTION = "lastEventSection";

    // List of all OASIS Channel ids
    public static final String CHANNEL_COMMAND = "command";
    public static final String CHANNEL_STATUS_A = "statusA";
    public static final String CHANNEL_STATUS_B = "statusB";
    public static final String CHANNEL_STATUS_ABC = "statusABC";
    public static final String CHANNEL_STATUS_PGX = "statusPGX";
    public static final String CHANNEL_STATUS_PGY = "statusPGY";

    // Constants
    public static final String JABLOTRON_API_URL = "https://api.jablonet.net/api/1.6/";
    public static final String AGENT = "Swagger-Codegen/1.0.0/android";
    public static final int TIMEOUT = 10;
    public static final String SYSTEM = "openHAB";
    public static final String VENDOR = "JABLOTRON:Jablotron";
    public static final String APPLICATION_JSON = "application/json";

    // supported thing types for discovery
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>(Arrays.asList(THING_TYPE_OASIS,
            THING_TYPE_JA100, THING_TYPE_JA100F));
}
