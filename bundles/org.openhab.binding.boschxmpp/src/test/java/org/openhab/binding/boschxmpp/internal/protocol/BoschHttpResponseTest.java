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
package org.openhab.binding.boschxmpp.internal.protocol;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Tests parsing of HTTP-like XMPP message bodies.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
class BoschHttpResponseTest {

    @Test
    void parsesLfResponse() throws BoschXmppException {
        BoschHttpResponse response = BoschHttpResponse.parse(
                "HTTP/1.1 200 OK\nContent-Type: application/json\nContent-Length: 24\n\nYW4tZW5jcnlwdGVkLWJvZHk=");

        assertEquals(200, response.statusCode());
        assertEquals("OK", response.statusMessage());
        assertEquals("application/json", response.headers().get("content-type"));
        assertEquals("YW4tZW5jcnlwdGVkLWJvZHk=", response.body());
    }

    @Test
    void parsesCrLfNoContentResponse() throws BoschXmppException {
        BoschHttpResponse response = BoschHttpResponse.parse("HTTP/1.1 204 No Content\r\nSeq-No: 2\r\n\r\n");

        assertEquals(204, response.statusCode());
        assertEquals("", response.body());
    }

    @Test
    void rejectsInvalidStatusLine() {
        assertThrows(BoschXmppException.class, () -> BoschHttpResponse.parse("not HTTP"));
    }
}
