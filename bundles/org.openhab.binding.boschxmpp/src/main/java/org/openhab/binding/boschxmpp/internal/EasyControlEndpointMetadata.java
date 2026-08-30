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

import java.math.BigDecimal;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Interprets feature metadata returned alongside an EasyControl endpoint value. */
@NonNullByDefault
final class EasyControlEndpointMetadata {

    record HeatingPumpValue(String state, @Nullable BigDecimal modulation) {
    }

    private EasyControlEndpointMetadata() {
    }

    static boolean isWritable(JsonObject response) {
        return response.has("writeable") && response.get("writeable").getAsInt() == 1 && response.has("available")
                && response.get("available").getAsBoolean() && response.has("used")
                && response.get("used").getAsBoolean();
    }

    static boolean booleanValue(JsonElement value) {
        if (!value.isJsonPrimitive()) {
            throw new IllegalArgumentException("Expected a boolean value");
        }
        var primitive = value.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isString()) {
            String text = primitive.getAsString();
            if ("true".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
        }
        throw new IllegalArgumentException("Expected true or false");
    }

    static boolean valuesEquivalent(JsonElement actual, JsonElement requested) {
        if (!actual.isJsonPrimitive() || !requested.isJsonPrimitive()) {
            return actual.equals(requested);
        }
        var actualPrimitive = actual.getAsJsonPrimitive();
        var requestedPrimitive = requested.getAsJsonPrimitive();
        if (actualPrimitive.isNumber() && requestedPrimitive.isNumber()) {
            return actualPrimitive.getAsBigDecimal().compareTo(requestedPrimitive.getAsBigDecimal()) == 0;
        }
        try {
            return booleanValue(actual) == booleanValue(requested);
        } catch (IllegalArgumentException e) {
            return actualPrimitive.getAsString().equals(requestedPrimitive.getAsString());
        }
    }

    static boolean isNumericValue(JsonObject response) {
        @Nullable
        JsonElement type = response.get("type");
        if (type != null && type.isJsonPrimitive()) {
            String valueType = type.getAsString();
            if ("floatValue".equals(valueType) || "intValue".equals(valueType)) {
                return true;
            }
            if ("stringValue".equals(valueType)) {
                return false;
            }
        }
        @Nullable
        JsonElement value = response.get("value");
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
    }

    static @Nullable HeatingPumpValue heatingPumpValue(JsonObject response) {
        @Nullable
        JsonElement value = response.get("value");
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (isNumericValue(response)) {
            BigDecimal modulation = value.getAsBigDecimal();
            return new HeatingPumpValue(modulation.signum() > 0 ? "on" : "off", modulation);
        }
        String state = value.getAsString();
        return switch (state.toLowerCase(Locale.ROOT)) {
            case "on" -> new HeatingPumpValue("on", BigDecimal.valueOf(100));
            case "off" -> new HeatingPumpValue("off", BigDecimal.ZERO);
            default -> new HeatingPumpValue(state, null);
        };
    }
}
