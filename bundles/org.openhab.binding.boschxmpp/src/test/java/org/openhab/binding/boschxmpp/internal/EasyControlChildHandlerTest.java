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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.binding.boschxmpp.internal.protocol.BoschXmppException;
import org.openhab.binding.boschxmpp.internal.protocol.BoschXmppException.Reason;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.types.Command;

import com.google.gson.JsonObject;

/** Tests one-time validation of child endpoints. */
@NonNullByDefault
class EasyControlChildHandlerTest {

    @Test
    void verifiesAChildEndpointOnlyOnce() throws BoschXmppException {
        EasyControlBridgeHandler bridge = mock(EasyControlBridgeHandler.class);
        when(bridge.get("/devices/device1/type")).thenReturn(new JsonObject());
        TestChildHandler handler = new TestChildHandler(thing());
        handler.setCallback(mock(ThingHandlerCallback.class));

        assertTrue(handler.verify(bridge, "/devices/device1/type"));
        assertTrue(handler.verify(bridge, "/devices/device1/type"));
        verify(bridge, times(1)).get("/devices/device1/type");
    }

    @Test
    void rejectsANonexistentChildEndpoint() throws BoschXmppException {
        EasyControlBridgeHandler bridge = mock(EasyControlBridgeHandler.class);
        when(bridge.get("/devices/device99/type"))
                .thenThrow(new BoschXmppException(Reason.INVALID_RESPONSE, "HTTP 404"));
        TestChildHandler handler = new TestChildHandler(thing());
        handler.setCallback(mock(ThingHandlerCallback.class));

        assertFalse(handler.verify(bridge, "/devices/device99/type"));
    }

    private static Thing thing() {
        Thing thing = mock(Thing.class);
        when(thing.getUID()).thenReturn(new ThingUID("boschxmpp:device:test"));
        return thing;
    }

    private static class TestChildHandler extends EasyControlChildHandler {

        TestChildHandler(Thing thing) {
            super(thing);
        }

        boolean verify(EasyControlBridgeHandler bridge, String endpoint) throws BoschXmppException {
            return verifyEndpoint(bridge, endpoint);
        }

        @Override
        public void handleCommand(ChannelUID channelUID, Command command) {
        }

        @Override
        public void initialize() {
        }

        @Override
        void refresh() {
        }
    }
}
