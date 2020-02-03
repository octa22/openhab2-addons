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

    // Common channels
    public static final String CHANNEL_COMMAND = "command";
    public static final String CHANNEL_ALARM = "alarm";
    public static final String CHANNEL_LAST_TROUBLE = "lastTrouble";
    public static final String CHANNEL_LAST_TROUBLE_DETAIL = "lastTroubleDetail";
    public static final String CHANNEL_LAST_EVENT = "lastEvent";
    public static final String CHANNEL_LAST_EVENT_CLASS = "lastEventClass";
    public static final String CHANNEL_LAST_EVENT_TIME = "lastEventTime";
    public static final String CHANNEL_LAST_CHECK_TIME = "lastCheckTime";

    // List of all OASIS Channel ids
    public static final String CHANNEL_LAST_EVENT_CODE = "lastEventCode";
    public static final String CHANNEL_STATUS_A = "statusA";
    public static final String CHANNEL_STATUS_B = "statusB";
    public static final String CHANNEL_STATUS_ABC = "statusABC";
    public static final String CHANNEL_STATUS_PGX = "statusPGX";
    public static final String CHANNEL_STATUS_PGY = "statusPGY";

    // JA-100 channels
    public static final String CHANNEL_LAST_EVENT_SECTION = "lastEventSection";

    // Constants
    public static final String JABLOTRON_URL = "https://www.jablonet.net/";
    public static final String JABLOTRON_API_URL = "https://api.jablonet.net/api/1.6/";
    public static final String OASIS_SERVICE_URL = "app/oasis?service=";
    public static final String JA100_SERVICE_URL = "app/ja100?service=";
    public static final String AGENT = "Swagger-Codegen/1.0.0/android";
    public static final int TIMEOUT = 10;
    public static final int LONG_TIMEOUT = 20;
    public static final String SYSTEM = "openHAB";
    public static final String VENDOR = "JABLOTRON:Jablotron";
}
