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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

/** Tests for EasyControl endpoint capability metadata. */
@NonNullByDefault
public class EasyControlEndpointMetadataTest {

    @Test
    public void acceptsAnAvailableAndUsedWritableEndpoint() {
        assertTrue(EasyControlEndpointMetadata.isWritable(
                JsonParser.parseString("{\"writeable\":1,\"available\":true,\"used\":true}").getAsJsonObject()));
    }

    @Test
    public void rejectsUnavailableOrUnusedEndpoint() {
        assertFalse(EasyControlEndpointMetadata.isWritable(
                JsonParser.parseString("{\"writeable\":1,\"available\":false,\"used\":false}").getAsJsonObject()));
    }

    @Test
    public void detectsNumericAndStringValueTypes() {
        assertTrue(EasyControlEndpointMetadata
                .isNumericValue(JsonParser.parseString("{\"type\":\"floatValue\",\"value\":0}").getAsJsonObject()));
        assertTrue(EasyControlEndpointMetadata
                .isNumericValue(JsonParser.parseString("{\"type\":\"intValue\",\"value\":0}").getAsJsonObject()));
        assertFalse(EasyControlEndpointMetadata.isNumericValue(
                JsonParser.parseString("{\"type\":\"stringValue\",\"value\":\"off\"}").getAsJsonObject()));
    }

    @Test
    public void convertsHeatingPumpValuesInBothDirections() {
        var numeric = Objects.requireNonNull(EasyControlEndpointMetadata.heatingPumpValue(
                JsonParser.parseString("{\"type\":\"floatValue\",\"value\":42.5}").getAsJsonObject()));
        var on = Objects.requireNonNull(EasyControlEndpointMetadata.heatingPumpValue(
                JsonParser.parseString("{\"type\":\"stringValue\",\"value\":\"on\"}").getAsJsonObject()));
        var off = Objects.requireNonNull(EasyControlEndpointMetadata.heatingPumpValue(
                JsonParser.parseString("{\"type\":\"stringValue\",\"value\":\"off\"}").getAsJsonObject()));
        var unknown = Objects.requireNonNull(EasyControlEndpointMetadata.heatingPumpValue(
                JsonParser.parseString("{\"type\":\"stringValue\",\"value\":\"running\"}").getAsJsonObject()));

        assertEquals("on", numeric.state());
        assertEquals(new BigDecimal("42.5"), numeric.modulation());
        assertEquals(new BigDecimal("100"), on.modulation());
        assertEquals(BigDecimal.ZERO, off.modulation());
        assertEquals("running", unknown.state());
        assertNull(unknown.modulation());
    }

    @Test
    public void parsesOnlyExplicitBooleanValues() {
        assertTrue(EasyControlEndpointMetadata.booleanValue(JsonParser.parseString("true")));
        assertTrue(EasyControlEndpointMetadata.booleanValue(JsonParser.parseString("\"TRUE\"")));
        assertFalse(EasyControlEndpointMetadata.booleanValue(JsonParser.parseString("false")));
        assertThrows(IllegalArgumentException.class,
                () -> EasyControlEndpointMetadata.booleanValue(JsonParser.parseString("\"off\"")));
    }

    @Test
    public void comparesConfirmedValuesAcrossCompatibleRepresentations() {
        assertTrue(EasyControlEndpointMetadata.valuesEquivalent(JsonParser.parseString("true"),
                JsonParser.parseString("\"true\"")));
        assertTrue(EasyControlEndpointMetadata.valuesEquivalent(JsonParser.parseString("21"),
                JsonParser.parseString("21.0")));
        assertFalse(EasyControlEndpointMetadata.valuesEquivalent(JsonParser.parseString("20"),
                JsonParser.parseString("21")));
    }
}
