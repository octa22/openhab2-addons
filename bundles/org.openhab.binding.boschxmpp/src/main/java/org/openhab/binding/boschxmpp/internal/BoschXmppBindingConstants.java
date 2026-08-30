/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.binding.boschxmpp.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link BoschXmppBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class BoschXmppBindingConstants {

    private static final String BINDING_ID = "boschxmpp";

    public static final ThingTypeUID THING_TYPE_EASYCONTROL = new ThingTypeUID(BINDING_ID, "easycontrol");
    public static final ThingTypeUID THING_TYPE_ZONE = new ThingTypeUID(BINDING_ID, "zone");
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");
    public static final ThingTypeUID THING_TYPE_DHW = new ThingTypeUID(BINDING_ID, "dhw");

    public static final String CONFIG_ZONE_ID = "zoneId";
    public static final String CONFIG_DEVICE_ID = "deviceId";
    public static final String CONFIG_CIRCUIT_ID = "circuitId";

    public static final String CHANNEL_FIRMWARE_VERSION = "firmware-version";
    public static final String CHANNEL_ROOM_TEMPERATURE = "room-temperature";
    public static final String CHANNEL_TARGET_TEMPERATURE = "target-temperature";
    public static final String CHANNEL_OPERATION_MODE = "operation-mode";
    public static final String CHANNEL_HEATING_STATUS = "heating-status";
    public static final String CHANNEL_HUMIDITY = "humidity";
    public static final String CHANNEL_OUTDOOR_TEMPERATURE = "outdoor-temperature";
    public static final String CHANNEL_SYSTEM_PRESSURE = "system-pressure";
    public static final String CHANNEL_SUPPLY_TEMPERATURE = "supply-temperature";
    public static final String CHANNEL_MODULATION = "modulation";
    public static final String CHANNEL_FLAME_INDICATION = "flame-indication";
    public static final String CHANNEL_HEATING_PUMP = "heating-pump";
    public static final String CHANNEL_HEATING_PUMP_MODULATION = "heating-pump-modulation";
    public static final String CHANNEL_ROOM_INFLUENCE = "room-influence";
    public static final String CHANNEL_TARGET_SUPPLY_TEMPERATURE = "target-supply-temperature";
    public static final String CHANNEL_NOTIFICATION_LIGHT = "notification-light";
    public static final String CHANNEL_DHW_TEMPERATURE = "dhw-temperature";
    public static final String CHANNEL_DHW_TARGET_TEMPERATURE = "dhw-target-temperature";
    public static final String CHANNEL_DHW_MODE = "dhw-mode";
    public static final String CHANNEL_DHW_STATUS = "dhw-status";
    public static final String CHANNEL_AWAY_MODE = "away-mode";
    public static final String CHANNEL_BATTERY = "battery";
    public static final String CHANNEL_SIGNAL = "signal";
    public static final String CHANNEL_DEVICE_TYPE = "device-type";
    public static final String CHANNEL_DEVICE_ZONE = "device-zone";
    public static final String CHANNEL_CHILD_LOCK = "child-lock";
    public static final String CHANNEL_DEVICE_TEMPERATURE = "device-temperature";
    public static final String CHANNEL_VALVE_POSITION = "valve-position";
}
