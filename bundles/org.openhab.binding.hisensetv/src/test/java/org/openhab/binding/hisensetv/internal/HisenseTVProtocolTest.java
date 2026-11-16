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
package org.openhab.binding.hisensetv.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

/**
 * Tests the MQTT protocol used by the binding.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
class HisenseTVProtocolTest {
    @Test
    void topicsUseThePairedRemoteIdentity() {
        String device = "XX:XX:XX:XX:XX:XY$normal";
        assertEquals("/remoteapp/tv/remote_service/XX:XX:XX:XX:XX:XY$normal/actions/sendkey",
                HisenseTVProtocol.actionTopic(device, HisenseTVProtocol.REMOTE, "sendkey"));
        assertEquals("/remoteapp/mobile/XX:XX:XX:XX:XX:XY$normal/", HisenseTVProtocol.responsePrefix(device));
    }

    @Test
    void volumeAcceptsNumbersAndNumericStrings() {
        assertEquals(0, parseVolume("{\"volume_value\":0}"));
        assertEquals(100, parseVolume("{\"volume_value\":\"100\"}"));
    }

    @Test
    void missingAndMalformedVolumeAreNotTreatedAsZero() {
        for (String response : new String[] { "null", "{}", "{\"volume_value\":false}", "{\"volume_value\":101}",
                "{\"volume_value\":-1}", "{\"volume_value\":0.5}", "{\"volume_value\":null}" }) {
            assertEquals(-1, parseVolume(response), response);
        }
    }

    @Test
    void sourceListKeepsWireIdsAndNamesFromValidEntries() {
        var sources = JsonParser.parseString("""
                [
                  {"sourceid":"1","sourcename":"TV"},
                  {"sourceid":2,"sourcename":"HDMI 1","displayname":"Game console"},
                  {"sourceid":"3","displayname":"HDMI 2"},
                  {"sourceid":null,"sourcename":"Composite"},
                  null
                ]
                """).getAsJsonArray();
        assertEquals(Map.of("1", "TV", "2", "HDMI 1"), HisenseTVProtocol.sources(sources));
    }

    @Test
    void pairingResultAcceptsIntegersAndRejectsMalformedResponses() {
        assertEquals(1, HisenseTVProtocol.pairingStatus(JsonParser.parseString("{\"result\":1}")));
        assertEquals(100, HisenseTVProtocol.pairingStatus(JsonParser.parseString("{\"result\":\"100\"}")));
        assertEquals(-1, HisenseTVProtocol.pairingStatus(JsonParser.parseString("{\"result\":1.5}")));
        assertEquals(-1, HisenseTVProtocol.pairingStatus(JsonParser.parseString("{\"info\":\"ok\"}")));
    }

    private static int parseVolume(String json) {
        return HisenseTVProtocol.volume(JsonParser.parseString(json));
    }
}
