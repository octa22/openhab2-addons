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

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Builds the HTTP-like request bodies used by EasyControl.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class EasyControlMessageFactory {

    record Request(String body, int sequence) {
    }

    private final AtomicInteger sequence = new AtomicInteger(1);

    public Request get(String endpoint) {
        int requestSequence = nextSequence();
        return new Request("GET " + endpoint + " HTTP/1.1\nUser-Agent: rrc2\nSeq-No: " + requestSequence + "\n\n",
                requestSequence);
    }

    public Request put(String endpoint, String encryptedPayload) {
        int requestSequence = nextSequence();
        return new Request(
                "PUT " + endpoint + " HTTP/1.1\nUser-Agent: rrc2\nContent-Type: application/json\nContent-Length: "
                        + encryptedPayload.length() + "\nSeq-No: " + requestSequence + "\n\n" + encryptedPayload,
                requestSequence);
    }

    private int nextSequence() {
        // EasyControl treats Seq-No as an unsigned byte. Values above 255 are silently ignored by the gateway.
        return sequence.getAndUpdate(current -> current == 255 ? 0 : current + 1);
    }
}
