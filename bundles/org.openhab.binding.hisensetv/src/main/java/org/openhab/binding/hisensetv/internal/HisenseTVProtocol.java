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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Remote app MQTT topics and response payloads used by Hisense TVs.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
final class HisenseTVProtocol {
    static final String PLATFORM = "platform_service";
    static final String UI = "ui_service";
    static final String REMOTE = "remote_service";
    static final String VOLUME_RESPONSE = PLATFORM + "/data/volume";
    static final String PAIRING_RESPONSE = UI + "/data/authenticationcode";
    static final String SOURCES_RESPONSE = UI + "/data/sourcelist";

    static String actionTopic(String device, String service, String action) {
        return "/remoteapp/tv/" + service + "/" + device + "/actions/" + action;
    }

    static String responsePrefix(String device) {
        return "/remoteapp/mobile/" + device + "/";
    }

    static int pairingStatus(JsonElement payload) {
        if (payload instanceof JsonObject object && object.get("result") instanceof JsonElement value
                && value.isJsonPrimitive()) {
            try {
                return value.getAsBigDecimal().intValueExact();
            } catch (NumberFormatException | ArithmeticException e) {
                return -1;
            }
        }
        return -1;
    }

    static int volume(JsonElement payload) {
        if (payload instanceof JsonObject object && object.get("volume_value") instanceof JsonElement value
                && value.isJsonPrimitive()) {
            try {
                int volume = value.getAsBigDecimal().intValueExact();
                if (volume >= 0 && volume <= 100) {
                    return volume;
                }
            } catch (NumberFormatException | ArithmeticException e) {
                return -1;
            }
        }
        return -1;
    }

    static Map<String, String> sources(JsonArray array) {
        Map<String, String> sources = new LinkedHashMap<>();
        for (JsonElement element : array) {
            if (element instanceof JsonObject source && source.get("sourceid") instanceof JsonElement id
                    && id.isJsonPrimitive() && source.get("sourcename") instanceof JsonElement name
                    && name.isJsonPrimitive() && name.getAsJsonPrimitive().isString() && !name.getAsString().isBlank()
                    && id.getAsString().matches("[0-9]+")) {
                sources.put(id.getAsString(), name.getAsString());
            }
        }
        return sources;
    }
}
