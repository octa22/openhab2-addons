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
package org.openhab.binding.boschxmpp.internal.discovery;

import static org.junit.jupiter.api.Assertions.*;
import static org.openhab.binding.boschxmpp.internal.BoschXmppBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

@NonNullByDefault
class EasyControlDiscoveryParserTest {

    @Test
    void parsesZoneListAndRejectsInvalidIds() {
        var response = JsonParser.parseString("""
                {"value":[{"id":1,"name":"Living"},{"id":0},{"id":"bad"}]}
                """).getAsJsonObject();

        var children = EasyControlDiscoveryParser.parseZones(response, THING_TYPE_ZONE, CONFIG_ZONE_ID);

        assertEquals(1, children.size());
        assertEquals("zn1", children.getFirst().id());
        assertEquals("EasyControl Zone 'Living'", children.getFirst().label());
    }

    @Test
    void convertsDeviceListIdsToEndpointIds() {
        var response = JsonParser.parseString("""
                {"value":[{"id":2,"name":"Valve","type":"thermostat_valve","zone":3}]}
                """).getAsJsonObject();

        var children = EasyControlDiscoveryParser.parseDevices(response, THING_TYPE_DEVICE, CONFIG_DEVICE_ID);

        assertEquals(1, children.size());
        assertEquals("device2", children.getFirst().id());
        assertEquals("thermostat_valve", children.getFirst().properties().get("deviceType"));
        assertEquals("3", children.getFirst().properties().get("zone"));
    }

    @Test
    void parsesOnlyDhwReferences() {
        var response = JsonParser.parseString("""
                {"references":[{"id":"/dhwCircuits/dhw1"},{"id":"/dhwCircuits/switchPrograms"}]}
                """).getAsJsonObject();

        var children = EasyControlDiscoveryParser.parseDhwCircuits(response, THING_TYPE_DHW, CONFIG_CIRCUIT_ID);

        assertEquals(1, children.size());
        assertEquals("dhw1", children.getFirst().id());
    }
}
