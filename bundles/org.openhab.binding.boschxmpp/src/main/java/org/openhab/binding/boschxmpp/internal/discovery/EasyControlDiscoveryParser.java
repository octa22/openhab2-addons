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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingTypeUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Parses the compact list endpoints used by EasyControl discovery. */
@NonNullByDefault
final class EasyControlDiscoveryParser {

    record Child(ThingTypeUID type, String id, String label, String configurationProperty,
            Map<String, String> properties) {
    }

    private EasyControlDiscoveryParser() {
    }

    static List<Child> parseZones(JsonObject response, ThingTypeUID type, String configurationProperty) {
        List<Child> children = new ArrayList<>();
        for (JsonElement element : array(response, "value")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject zone = element.getAsJsonObject();
            @Nullable
            String numericId = string(zone, "id");
            if (numericId == null || !numericId.matches("[1-9][0-9]*")) {
                continue;
            }
            @Nullable
            String name = string(zone, "name");
            children.add(
                    new Child(type, "zn" + numericId, name == null || name.isBlank() ? "EasyControl Zone " + numericId
                            : "EasyControl Zone '" + name + "'", configurationProperty, Map.of()));
        }
        return children;
    }

    static List<Child> parseDevices(JsonObject response, ThingTypeUID type, String configurationProperty) {
        List<Child> children = new ArrayList<>();
        for (JsonElement element : array(response, "value")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject device = element.getAsJsonObject();
            @Nullable
            String numericId = string(device, "id");
            if (numericId == null || !numericId.matches("[1-9][0-9]*")) {
                continue;
            }
            @Nullable
            String name = string(device, "name");
            @Nullable
            String deviceType = string(device, "type");
            @Nullable
            String zone = string(device, "zone");
            Map<String, String> properties = deviceType == null ? Map.of()
                    : zone == null ? Map.of("deviceType", deviceType) : Map.of("deviceType", deviceType, "zone", zone);
            children.add(new Child(type, "device" + numericId,
                    name == null || name.isBlank() ? "EasyControl Device " + numericId
                            : "EasyControl Device '" + name + "'",
                    configurationProperty, properties));
        }
        return children;
    }

    static List<Child> parseDhwCircuits(JsonObject response, ThingTypeUID type, String configurationProperty) {
        List<Child> children = new ArrayList<>();
        for (JsonElement element : array(response, "references")) {
            if (!element.isJsonObject()) {
                continue;
            }
            @Nullable
            String path = string(element.getAsJsonObject(), "id");
            if (path == null || !path.matches("/dhwCircuits/dhw[1-9][0-9]*")) {
                continue;
            }
            String id = path.substring(path.lastIndexOf('/') + 1);
            children.add(
                    new Child(type, id, "EasyControl Hot Water " + id.substring(3), configurationProperty, Map.of()));
        }
        return children;
    }

    private static JsonArray array(JsonObject object, String member) {
        @Nullable
        JsonElement value = object.get(member);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : new JsonArray();
    }

    private static @Nullable String string(JsonObject object, String member) {
        @Nullable
        JsonElement value = object.get(member);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }
}
