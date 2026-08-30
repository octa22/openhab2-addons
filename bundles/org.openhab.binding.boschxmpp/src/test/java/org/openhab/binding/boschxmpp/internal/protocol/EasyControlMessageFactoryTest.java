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
 * Tests the exact EasyControl request wire format.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
class EasyControlMessageFactoryTest {

    @Test
    void buildsGetAndPutRequestsWithIncreasingSequenceNumbers() {
        EasyControlMessageFactory factory = new EasyControlMessageFactory();

        EasyControlMessageFactory.Request get = factory.get("/gateway/versionFirmware");
        EasyControlMessageFactory.Request put = factory.put("/zones/zn1/userMode", "YWJj");

        assertEquals(1, get.sequence());
        assertEquals("GET /gateway/versionFirmware HTTP/1.1\nUser-Agent: rrc2\nSeq-No: 1\n\n", get.body());
        assertEquals(2, put.sequence());
        assertEquals(
                "PUT /zones/zn1/userMode HTTP/1.1\nUser-Agent: rrc2\nContent-Type: application/json\nContent-Length: 4\nSeq-No: 2\n\nYWJj",
                put.body());
    }

    @Test
    void wrapsSequenceNumberAfterUnsignedByteMaximum() {
        EasyControlMessageFactory factory = new EasyControlMessageFactory();

        for (int expected = 1; expected <= 255; expected++) {
            assertEquals(expected, factory.get("/test").sequence());
        }
        assertEquals(0, factory.get("/test").sequence());
        assertEquals(1, factory.get("/test").sequence());
    }
}
