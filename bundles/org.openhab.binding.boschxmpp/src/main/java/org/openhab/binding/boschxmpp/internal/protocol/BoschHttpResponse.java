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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.boschxmpp.internal.protocol.BoschXmppException.Reason;

/**
 * Parsed HTTP-like response transported in an XMPP message body.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public record BoschHttpResponse(int statusCode, String statusMessage, Map<String, String> headers, String body) {

    public BoschHttpResponse {
        headers = Collections.unmodifiableMap(headers);
    }

    public static BoschHttpResponse parse(String response) throws BoschXmppException {
        String normalized = response.replace("\r\n", "\n").replace('\r', '\n');
        int separator = normalized.indexOf("\n\n");
        String headerBlock = separator >= 0 ? normalized.substring(0, separator) : normalized;
        String body = separator >= 0 ? normalized.substring(separator + 2).strip() : "";
        String[] lines = headerBlock.split("\n");
        if (lines.length == 0) {
            throw new BoschXmppException(Reason.INVALID_RESPONSE, "Empty EasyControl response");
        }
        String[] status = lines[0].split(" ", 3);
        if (status.length < 2 || !status[0].startsWith("HTTP/1.")) {
            throw new BoschXmppException(Reason.INVALID_RESPONSE, "Invalid EasyControl response status");
        }
        int statusCode;
        try {
            statusCode = Integer.parseInt(status[1]);
        } catch (NumberFormatException e) {
            throw new BoschXmppException(Reason.INVALID_RESPONSE, "Invalid EasyControl response code", e);
        }
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon > 0) {
                headers.put(lines[i].substring(0, colon).strip().toLowerCase(Locale.ROOT),
                        lines[i].substring(colon + 1).strip());
            }
        }
        return new BoschHttpResponse(statusCode, status.length == 3 ? status[2] : "", headers, body);
    }
}
