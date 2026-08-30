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

import java.io.StringReader;
import java.time.Duration;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.UnparsedIQ;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.xpp3.Xpp3XmlPullParserFactory;
import org.junit.jupiter.api.Test;
import org.openhab.binding.boschxmpp.internal.protocol.BoschXmppException.Reason;

/** Tests correlation of EasyControl responses with their requests. */
@NonNullByDefault
class EasyControlClientTest {

    private static final String ACCESS_KEY = "abcdefghijklmnop";
    private static final String DEVICE_PASSWORD = "test-password";

    @Test
    void acceptsOnlyMatchingSequenceAndEndpointForGet() throws BoschXmppException {
        EasyControlClient client = newClient();

        assertTrue(client.matchesExpectedResponse(response(7, "/heatSources/CHpumpModulation"), 7,
                "/heatSources/CHpumpModulation"));
        assertFalse(client.matchesExpectedResponse(response(6, "/heatSources/CHpumpModulation"), 7,
                "/heatSources/CHpumpModulation"));
        assertFalse(client.matchesExpectedResponse(response(7, "/heatSources/flameIndication"), 7,
                "/heatSources/CHpumpModulation"));
    }

    @Test
    void correlatesPutAndErrorResponsesBySequence() throws BoschXmppException {
        EasyControlClient client = newClient();

        assertTrue(client.matchesExpectedResponse("HTTP/1.1 204 No Content\nSeq-No: 8\n\n", 8, null));
        assertFalse(client.matchesExpectedResponse("HTTP/1.1 204 No Content\nSeq-No: 7\n\n", 8, null));
        assertTrue(client.matchesExpectedResponse("HTTP/1.1 400 Bad Request\nSeq-No: 8\n\n", 8, "/zones/zn1/userMode"));
    }

    @Test
    void rejectsResponseWithoutSequence() throws BoschXmppException {
        EasyControlClient client = newClient();

        assertFalse(client.matchesExpectedResponse("HTTP/1.1 204 No Content\n\n", 8, null));
    }

    @Test
    void classifiesHttpFailuresByTheirOperationalMeaning() {
        assertEquals(Reason.INVALID_RESPONSE, EasyControlClient.reasonForStatus(400));
        assertEquals(Reason.AUTHENTICATION, EasyControlClient.reasonForStatus(401));
        assertEquals(Reason.COMMUNICATION, EasyControlClient.reasonForStatus(429));
        assertEquals(Reason.COMMUNICATION, EasyControlClient.reasonForStatus(503));
    }

    @Test
    void distinguishesMalformedJsonFromDecryptionFailure() throws BoschXmppException {
        EasyControlClient client = newClient();
        EasyControlCrypto crypto = new EasyControlCrypto(ACCESS_KEY, DEVICE_PASSWORD);
        BoschHttpResponse malformedJson = new BoschHttpResponse(200, "OK", java.util.Map.of(),
                crypto.encrypt("not-json"));

        BoschXmppException invalidResponse = assertThrows(BoschXmppException.class,
                () -> client.decodeGetResponse(malformedJson, "/test"));
        assertEquals(Reason.INVALID_RESPONSE, invalidResponse.getReason());

        BoschHttpResponse invalidCiphertext = new BoschHttpResponse(200, "OK", java.util.Map.of(), "not-base64");
        BoschXmppException decryption = assertThrows(BoschXmppException.class,
                () -> client.decodeGetResponse(invalidCiphertext, "/test"));
        assertEquals(Reason.DECRYPTION, decryption.getReason());
    }

    @Test
    void createsTheConfiguredXpp3Parser() throws Exception {
        XmlPullParser parser = new Xpp3XmlPullParserFactory().newXmlPullParser(new StringReader("<message/>"));

        assertNotNull(parser);
        assertEquals(XmlPullParser.Event.START_ELEMENT, parser.next());
    }

    @Test
    void sendsPresenceHeartbeatEveryThirtySeconds() {
        long interval = EasyControlClient.PRESENCE_INTERVAL.toNanos();

        assertTrue(EasyControlClient.isPresenceDue(0, 1));
        assertFalse(EasyControlClient.isPresenceDue(1, interval));
        assertTrue(EasyControlClient.isPresenceDue(1, interval + 1));
    }

    @Test
    void answersBoschVersionQueries() {
        IQ request = new UnparsedIQ("query", "jabber:iq:version", "");
        request.setType(IQ.Type.get);
        request.setStanzaId("version-1");

        IQ softwareResponse = EasyControlClient.softwareVersionResponse(request);
        assertEquals(IQ.Type.result, softwareResponse.getType());
        assertEquals("version-1", softwareResponse.getStanzaId());
        assertTrue(softwareResponse.toXML().toString().contains("<version>-1364755535</version>"));

        IQ boschResponse = EasyControlClient.boschClientResponse(request);
        String xml = boschResponse.toXML().toString();
        assertTrue(xml.contains("<name>3.6.0</name>"));
        assertTrue(xml.contains("<version>3.6.0</version>"));
        assertTrue(xml.contains("<os/>") || xml.contains("<os></os>"));
    }

    private static EasyControlClient newClient() throws BoschXmppException {
        return new EasyControlClient("example.org", 5222, "123456789", ACCESS_KEY, DEVICE_PASSWORD,
                Duration.ofSeconds(1));
    }

    private static String response(int sequence, String endpoint) throws BoschXmppException {
        EasyControlCrypto crypto = new EasyControlCrypto(ACCESS_KEY, DEVICE_PASSWORD);
        String body = crypto.encrypt("{\"id\":\"" + endpoint + "\",\"value\":0}");
        return "HTTP/1.1 200 OK\nSeq-No: " + sequence + "\nContent-Type: application/json\n\n" + body;
    }
}
