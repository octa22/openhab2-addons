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

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.IQChildElementXmlStringBuilder;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.xml.SmackXmlParser;
import org.jivesoftware.smack.xml.xpp3.Xpp3XmlPullParserFactory;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.openhab.binding.boschxmpp.internal.protocol.BoschXmppException.Reason;
import org.openhab.binding.boschxmpp.internal.protocol.EasyControlMessageFactory.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Persistent XMPP client for a Bosch EasyControl controller.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class EasyControlClient implements AutoCloseable {

    private record PendingRequest(CompletableFuture<String> response, int sequence, @Nullable String expectedEndpoint) {
    }

    static {
        // The add-on embeds Smack. Register its parser directly, as the OSGi thread context class loader can otherwise
        // discover a provider from another revision of the add-on during a hot deployment.
        SmackXmlParser.setXmlPullParserFactory(new Xpp3XmlPullParserFactory());
    }

    private static final String CONTACT_PREFIX = "rrc2contact_";
    private static final String GATEWAY_PREFIX = "rrc2gateway_";
    private static final String ACCESS_KEY_PREFIX = "C42i9NNp_";
    // Bosch' XMPP endpoint does not support TLS 1.3.
    private static final String[] ENABLED_TLS_PROTOCOLS = { "TLSv1.2" };
    // The endpoint only accepts this TLS 1.2 cipher suite.
    private static final String[] ENABLED_TLS_CIPHER_SUITES = { "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384" };
    // Bosch' backend expects an available presence stanza periodically, even while application traffic is flowing.
    static final Duration PRESENCE_INTERVAL = Duration.ofSeconds(30);
    private static final String XMPP_PING_ELEMENT = "ping";
    private static final String XMPP_PING_NAMESPACE = "urn:xmpp:ping";
    private static final String XMPP_QUERY_ELEMENT = "query";
    private static final String XMPP_VERSION_NAMESPACE = "jabber:iq:version";
    private static final String BOSCH_CLIENT_NAMESPACE = "com.bosch.tt.buderus.controlng";
    private static final String BOSCH_CLIENT_VERSION = "3.6.0";

    private final String hostname;
    private final int port;
    private final String username;
    private final String xmppPassword;
    private final EntityBareJid gatewayJid;
    private final Resourcepart sessionResource = Resourcepart.fromOrThrowUnchecked("openhab-" + UUID.randomUUID());
    private final Duration requestTimeout;
    private final EasyControlCrypto crypto;
    private final EasyControlMessageFactory messageFactory = new EasyControlMessageFactory();
    private final ReentrantLock requestLock = new ReentrantLock(true);
    private final X509TrustManager trustManager;
    private final Logger logger = LoggerFactory.getLogger(EasyControlClient.class);

    private volatile @Nullable AbstractXMPPConnection connection;
    private volatile @Nullable PendingRequest pendingRequest;
    private volatile long lastPresenceNanos;

    public EasyControlClient(String hostname, int port, String serialNumber, String accessKey, String devicePassword,
            Duration requestTimeout) throws BoschXmppException {
        this.hostname = hostname;
        this.port = port;
        username = CONTACT_PREFIX + serialNumber;
        xmppPassword = ACCESS_KEY_PREFIX + accessKey.replace("-", "");
        this.requestTimeout = requestTimeout;
        crypto = new EasyControlCrypto(accessKey, devicePassword);
        try {
            gatewayJid = JidCreate.entityBareFrom(GATEWAY_PREFIX + serialNumber + "@" + hostname);
            trustManager = createTrustManager();
        } catch (GeneralSecurityException | IOException e) {
            throw new BoschXmppException(Reason.COMMUNICATION, "Could not initialize EasyControl connection", e);
        }
    }

    public JsonObject get(String endpoint) throws BoschXmppException {
        BoschHttpResponse response = request(messageFactory.get(endpoint), endpoint, true);
        if (response.statusCode() != 200) {
            throw new BoschXmppException(reasonForStatus(response.statusCode()),
                    "EasyControl returned HTTP " + response.statusCode() + " for " + endpoint);
        }
        return decodeGetResponse(response, endpoint);
    }

    JsonObject decodeGetResponse(BoschHttpResponse response, String endpoint) throws BoschXmppException {
        String decrypted = crypto.decrypt(response.body());
        try {
            JsonElement json = JsonParser.parseString(decrypted);
            if (!json.isJsonObject()) {
                throw new BoschXmppException(Reason.INVALID_RESPONSE, "EasyControl returned non-object JSON");
            }
            JsonObject result = json.getAsJsonObject();
            JsonElement id = result.get("id");
            if (id == null || !id.isJsonPrimitive() || !endpoint.equals(id.getAsString())) {
                throw new BoschXmppException(Reason.INVALID_RESPONSE,
                        "EasyControl returned a response for a different endpoint while waiting for " + endpoint);
            }
            return result;
        } catch (BoschXmppException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BoschXmppException(Reason.INVALID_RESPONSE, "EasyControl returned malformed JSON", e);
        }
    }

    public void put(String endpoint, JsonElement value) throws BoschXmppException {
        JsonObject payload = new JsonObject();
        payload.add("value", value);
        String encrypted = crypto.encrypt(payload.toString());
        BoschHttpResponse response = request(messageFactory.put(endpoint, encrypted), null, false);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BoschXmppException(reasonForStatus(response.statusCode()), "EasyControl returned HTTP "
                    + response.statusCode() + " " + response.statusMessage() + " for " + endpoint);
        }
    }

    public synchronized void connect() throws BoschXmppException {
        AbstractXMPPConnection existing = connection;
        if (existing != null && existing.isAuthenticated()) {
            try {
                sendPresenceIfDue(existing, System.nanoTime());
                return;
            } catch (InterruptedException e) {
                disconnect();
                Thread.currentThread().interrupt();
                throw new BoschXmppException(Reason.COMMUNICATION,
                        "Interrupted while refreshing the EasyControl XMPP presence", e);
            } catch (NotConnectedException e) {
                disconnect();
                throw new BoschXmppException(Reason.COMMUNICATION, "Could not refresh the EasyControl XMPP presence",
                        e);
            }
        }
        disconnect();
        try {
            XMPPTCPConnectionConfiguration configuration = XMPPTCPConnectionConfiguration.builder().setHost(hostname)
                    .setPort(port).setUsernameAndPassword(username, xmppPassword).setXmppDomain(hostname)
                    .setResource(sessionResource).setSecurityMode(SecurityMode.required)
                    .setEnabledSSLProtocols(ENABLED_TLS_PROTOCOLS).setEnabledSSLCiphers(ENABLED_TLS_CIPHER_SUITES)
                    .setCustomX509TrustManager(trustManager).setConnectTimeout((int) requestTimeout.toMillis()).build();
            AbstractXMPPConnection newConnection = new XMPPTCPConnection(configuration);
            connection = newConnection;
            lastPresenceNanos = 0;
            newConnection.addConnectionListener(new ConnectionListener() {
                @Override
                public void connectionClosed() {
                    connectionLost(newConnection, new IOException("EasyControl XMPP connection was closed"));
                }

                @Override
                public void connectionClosedOnError(@Nullable Exception error) {
                    connectionLost(newConnection, error);
                }
            });
            newConnection.addAsyncStanzaListener(stanza -> onMessage(newConnection, (Message) stanza),
                    StanzaTypeFilter.MESSAGE);
            newConnection.addAsyncStanzaListener(stanza -> {
                IQ iq = (IQ) stanza;
                if (iq.getType() == Type.get || iq.getType() == Type.set) {
                    logger.trace("Received EasyControl XMPP IQ request {}/{}", iq.getChildElementName(),
                            iq.getChildElementNamespace());
                }
            }, StanzaTypeFilter.IQ);
            newConnection.registerIQRequestHandler(
                    new AbstractIqRequestHandler(XMPP_PING_ELEMENT, XMPP_PING_NAMESPACE, Type.get, Mode.async) {
                        @Override
                        public @Nullable IQ handleIQRequest(@Nullable IQ request) {
                            if (request == null) {
                                return null;
                            }
                            logger.trace("Answering EasyControl XMPP ping");
                            return IQ.createResultIQ(request);
                        }
                    });
            newConnection.registerIQRequestHandler(
                    new AbstractIqRequestHandler(XMPP_QUERY_ELEMENT, XMPP_VERSION_NAMESPACE, Type.get, Mode.async) {
                        @Override
                        public @Nullable IQ handleIQRequest(@Nullable IQ request) {
                            return request == null ? null : softwareVersionResponse(request);
                        }
                    });
            newConnection.registerIQRequestHandler(
                    new AbstractIqRequestHandler(XMPP_QUERY_ELEMENT, BOSCH_CLIENT_NAMESPACE, Type.get, Mode.async) {
                        @Override
                        public @Nullable IQ handleIQRequest(@Nullable IQ request) {
                            return request == null ? null : boschClientResponse(request);
                        }
                    });
            newConnection.connect().login();
            sendPresenceIfDue(newConnection, System.nanoTime());
        } catch (SASLErrorException e) {
            disconnect();
            throw new BoschXmppException(Reason.AUTHENTICATION,
                    "EasyControl authentication failed; check serial number and access key", e);
        } catch (InterruptedException e) {
            disconnect();
            Thread.currentThread().interrupt();
            throw new BoschXmppException(Reason.COMMUNICATION,
                    "Interrupted while connecting to the EasyControl XMPP service", e);
        } catch (Exception e) {
            disconnect();
            throw new BoschXmppException(Reason.COMMUNICATION, "Could not connect to the EasyControl XMPP service", e);
        }
    }

    public synchronized boolean isConnected() {
        AbstractXMPPConnection current = connection;
        return current != null && current.isAuthenticated();
    }

    @Override
    public synchronized void close() {
        disconnect();
    }

    private BoschHttpResponse request(Request request, @Nullable String expectedEndpoint, boolean retryAfterSend)
            throws BoschXmppException {
        try {
            requestLock.lockInterruptibly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BoschXmppException(Reason.COMMUNICATION, "Interrupted while waiting to send request", e);
        }
        try {
            connectWithRetry();
            try {
                return BoschHttpResponse.parse(sendAndReceive(request, expectedEndpoint));
            } catch (BoschXmppException e) {
                if (!retryAfterSend || e.getReason() != Reason.COMMUNICATION) {
                    throw e;
                }
                disconnect();
                connectWithRetry();
                try {
                    return BoschHttpResponse.parse(sendAndReceive(request, expectedEndpoint));
                } catch (BoschXmppException retryError) {
                    disconnect();
                    throw retryError;
                }
            }
        } finally {
            requestLock.unlock();
        }
    }

    private void connectWithRetry() throws BoschXmppException {
        BoschXmppException firstError;
        try {
            connect();
            return;
        } catch (BoschXmppException e) {
            if (e.getReason() != Reason.COMMUNICATION) {
                throw e;
            }
            firstError = e;
        }
        try {
            connect();
        } catch (BoschXmppException e) {
            e.addSuppressed(firstError);
            throw e;
        }
    }

    private String sendAndReceive(Request messageRequest, @Nullable String expectedEndpoint) throws BoschXmppException {
        AbstractXMPPConnection current = connection;
        if (current == null || !current.isAuthenticated()) {
            throw new BoschXmppException(Reason.COMMUNICATION, "EasyControl XMPP connection is not authenticated");
        }
        CompletableFuture<String> response = new CompletableFuture<>();
        PendingRequest request = new PendingRequest(response, messageRequest.sequence(), expectedEndpoint);
        pendingRequest = request;
        try {
            Message message = StanzaBuilder.buildMessage().to(gatewayJid).ofType(Message.Type.chat)
                    .setBody(messageRequest.body()).build();
            current.sendStanza(message);
            return response.get(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new BoschXmppException(Reason.COMMUNICATION, "EasyControl request timed out", e);
        } catch (ExecutionException e) {
            throw new BoschXmppException(Reason.COMMUNICATION, "EasyControl request failed",
                    Objects.requireNonNullElse(e.getCause(), e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BoschXmppException(Reason.COMMUNICATION, "EasyControl request was interrupted", e);
        } catch (Exception e) {
            throw new BoschXmppException(Reason.COMMUNICATION, "Could not send EasyControl request", e);
        } finally {
            pendingRequest = null;
        }
    }

    private void sendPresenceIfDue(AbstractXMPPConnection current, long nowNanos)
            throws NotConnectedException, InterruptedException {
        if (isPresenceDue(lastPresenceNanos, nowNanos)) {
            current.sendStanza(StanzaBuilder.buildPresence().build());
            lastPresenceNanos = nowNanos;
            logger.trace("Sent EasyControl XMPP presence heartbeat");
        }
    }

    static boolean isPresenceDue(long lastPresenceNanos, long nowNanos) {
        return lastPresenceNanos == 0 || nowNanos - lastPresenceNanos >= PRESENCE_INTERVAL.toNanos();
    }

    static IQ softwareVersionResponse(IQ request) {
        return new QueryResultIQ(request, XMPP_VERSION_NAMESPACE, null, "-1364755535", null);
    }

    static IQ boschClientResponse(IQ request) {
        return new QueryResultIQ(request, BOSCH_CLIENT_NAMESPACE, BOSCH_CLIENT_VERSION, BOSCH_CLIENT_VERSION, "");
    }

    private void connectionLost(AbstractXMPPConnection source, @Nullable Exception error) {
        Exception connectionError = Objects.requireNonNullElseGet(error,
                () -> new IOException("EasyControl XMPP connection failed"));
        synchronized (this) {
            if (source != connection) {
                return;
            }
            connection = null;
            lastPresenceNanos = 0;
        }
        PendingRequest request = pendingRequest;
        if (request != null) {
            request.response().completeExceptionally(connectionError);
        }
        logger.debug("EasyControl XMPP connection closed: {}", connectionError.getMessage());
    }

    private void onMessage(AbstractXMPPConnection source, Message message) {
        Jid from = message.getFrom();
        @Nullable
        String body = message.getBody();
        PendingRequest request = pendingRequest;
        // A response from an earlier connection can arrive after a timeout. It must not satisfy a request on the
        // replacement connection.
        if (source.equals(connection) && from != null && from.asBareJid().equals(gatewayJid) && body != null
                && request != null) {
            if (message.getType() == Message.Type.error) {
                request.response().completeExceptionally(new IOException("EasyControl returned an XMPP error"));
            } else if (matchesExpectedResponse(body, request.sequence(), request.expectedEndpoint())) {
                request.response().complete(body);
            }
        }
    }

    private static final class QueryResultIQ extends IQ {

        private final @Nullable String name;
        private final String version;
        private final @Nullable String operatingSystem;

        private QueryResultIQ(IQ request, String namespace, @Nullable String name, String version,
                @Nullable String operatingSystem) {
            super(XMPP_QUERY_ELEMENT, namespace);
            this.name = name;
            this.version = version;
            this.operatingSystem = operatingSystem;
            setType(Type.result);
            setStanzaId(request.getStanzaId());
            setTo(request.getFrom());
            setFrom(request.getTo());
        }

        @Override
        protected @Nullable IQChildElementXmlStringBuilder getIQChildElementBuilder(
                @Nullable IQChildElementXmlStringBuilder xml) {
            if (xml != null) {
                xml.rightAngleBracket();
                if (name != null) {
                    xml.element("name", name);
                }
                xml.element("version", version);
                if (operatingSystem != null) {
                    xml.element("os", operatingSystem);
                }
            }
            return xml;
        }
    }

    boolean matchesExpectedResponse(String body, int expectedSequence, @Nullable String expectedEndpoint) {
        try {
            BoschHttpResponse response = BoschHttpResponse.parse(body);
            @Nullable
            String sequenceHeader = response.headers().get("seq-no");
            if (sequenceHeader == null) {
                logger.trace("Ignoring EasyControl response without Seq-No while waiting for {}", expectedSequence);
                return false;
            }
            int actualSequence;
            try {
                actualSequence = Integer.parseInt(sequenceHeader);
            } catch (NumberFormatException e) {
                logger.trace("Ignoring EasyControl response with invalid Seq-No {}", sequenceHeader);
                return false;
            }
            if (actualSequence != expectedSequence) {
                logger.trace("Ignoring EasyControl response with Seq-No {} while waiting for {}", actualSequence,
                        expectedSequence);
                return false;
            }
            if (expectedEndpoint == null || response.statusCode() != 200) {
                return true;
            }
            JsonElement json = JsonParser.parseString(crypto.decrypt(response.body()));
            if (!json.isJsonObject()) {
                return true;
            }
            JsonElement id = json.getAsJsonObject().get("id");
            if (id == null || !id.isJsonPrimitive()) {
                logger.trace("Ignoring EasyControl response without an endpoint id while waiting for {}",
                        expectedEndpoint);
                return false;
            }
            String actualEndpoint = id.getAsString();
            if (!expectedEndpoint.equals(actualEndpoint)) {
                logger.trace("Ignoring EasyControl response for {} while waiting for {}", actualEndpoint,
                        expectedEndpoint);
                return false;
            }
        } catch (BoschXmppException | RuntimeException e) {
            // Let the request path report malformed or undecryptable responses instead of hiding them as a timeout.
            return true;
        }
        return true;
    }

    static Reason reasonForStatus(int statusCode) {
        if (statusCode == 401) {
            return Reason.AUTHENTICATION;
        }
        if (statusCode == 408 || statusCode == 425 || statusCode == 429 || statusCode >= 500) {
            return Reason.COMMUNICATION;
        }
        return Reason.INVALID_RESPONSE;
    }

    private synchronized void disconnect() {
        AbstractXMPPConnection current = connection;
        connection = null;
        lastPresenceNanos = 0;
        if (current != null) {
            current.disconnect();
        }
    }

    private static X509TrustManager createTrustManager() throws GeneralSecurityException, IOException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Certificate certificate;
        try (InputStream input = EasyControlClient.class.getResourceAsStream("/bosch-easycontrol-root-ca.pem")) {
            if (input == null) {
                throw new IOException("Bundled EasyControl root CA certificate is missing");
            }
            certificate = certificateFactory.generateCertificate(input);
        }
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null);
        trustStore.setCertificateEntry("bosch-easycontrol-root", certificate);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        for (var manager : trustManagerFactory.getTrustManagers()) {
            if (manager instanceof X509TrustManager x509TrustManager) {
                return x509TrustManager;
            }
        }
        throw new GeneralSecurityException("Could not create EasyControl X.509 trust manager");
    }
}
