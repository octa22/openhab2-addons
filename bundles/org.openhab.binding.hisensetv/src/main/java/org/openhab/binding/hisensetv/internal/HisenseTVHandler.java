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
package org.openhab.binding.hisensetv.internal;

import static org.openhab.binding.hisensetv.internal.HisenseTVProtocol.*;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.net.ssl.TrustManager;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection;
import org.openhab.core.io.transport.mqtt.MqttConnectionState;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Controls a Hisense television through its built-in MQTT broker.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
class HisenseTVHandler extends BaseThingHandler {
    private static final long PIN_WINDOW_NANOS = TimeUnit.MINUTES.toNanos(2);
    private static final String KEY = "key";
    private static final String VOLUME = "volume";
    private static final String SOURCE = "source";
    private static final String SOURCES = "sources";
    private static final Pattern REMOTE_ID = Pattern.compile("(?:[0-9A-Z]{2}:){5}[0-9A-Z]{2}");
    private static final Pattern KEY_CODE = Pattern.compile("KEY_[A-Z0-9_]{1,40}");
    private static final String LEGACY_VOLUME_TOPIC = "/remoteapp/mobile/broadcast/ui_service/volume";

    private final Logger logger = LoggerFactory.getLogger(HisenseTVHandler.class);
    private @Nullable Session session;

    HisenseTVHandler(Thing thing) {
        super(thing);
    }

    @Override
    public synchronized void initialize() {
        HisenseTVConfiguration config = getConfigAs(HisenseTVConfiguration.class);
        String remoteId = config.remoteId.trim().toUpperCase(Locale.ROOT);
        String fingerprint = config.certificateFingerprint.replace(":", "").trim();
        if (config.host.isBlank() || config.port < 1 || config.port > 65535 || config.refreshInterval < 1
                || !REMOTE_ID.matcher(remoteId).matches()
                || (!fingerprint.isEmpty() && (!config.tls || !fingerprint.matches("[0-9a-fA-F]{64}")))) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "@text/offline.configuration-error");
            return;
        }

        String device = remoteId + "$normal";
        MqttBrokerConnection broker = new MqttBrokerConnection(config.host.trim(), config.port, config.tls,
                fingerprint.isEmpty(), null);
        if (!fingerprint.isEmpty()) {
            broker.setTrustManagers(new TrustManager[] { new HisenseTVTrustManager(fingerprint) });
        }
        broker.setCredentials(config.username, config.password);
        broker.setTimeoutExecutor(scheduler, 10_000);
        broker.setUnsubscribeOnStop(false);

        Session current = new Session(broker, device);
        session = current;
        broker.subscribe(current.responsePrefix + "#",
                (topic, bytes) -> scheduler.execute(() -> receive(current, topic, bytes)));
        broker.subscribe(LEGACY_VOLUME_TOPIC,
                (topic, bytes) -> scheduler.execute(() -> receive(current, topic, bytes)));
        broker.addConnectionObserver((state, error) -> {
            if (state != MqttConnectionState.CONNECTING) {
                scheduler.execute(() -> connectionChanged(current, state));
            }
        });
        updateStatus(ThingStatus.UNKNOWN);
        broker.start();
        current.poll = scheduler.scheduleWithFixedDelay(() -> pollVolume(current), config.refreshInterval,
                config.refreshInterval, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        Session previous;
        synchronized (this) {
            previous = session;
            session = null;
            if (previous != null) {
                previous.cancelPairing();
            }
        }
        if (previous != null) {
            ScheduledFuture<?> poll = previous.poll;
            if (poll != null) {
                poll.cancel(false);
            }
            previous.broker.stop();
        }
    }

    private synchronized void connectionChanged(Session current, MqttConnectionState state) {
        if (!current.equals(session) || current.broker.connectionState() != state) {
            return;
        }
        current.ready = state == MqttConnectionState.CONNECTED;
        if (current.ready) {
            updateStatus(ThingStatus.ONLINE);
            publish(current, PLATFORM, "getvolume", "");
            publish(current, UI, "sourcelist", "");
        } else {
            current.cancelPairing();
            current.sources = Map.of();
            updateState(VOLUME, UnDefType.UNDEF);
            updateState(SOURCES, UnDefType.UNDEF);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "@text/offline.mqtt-disconnected");
        }
    }

    private synchronized void pollVolume(Session current) {
        if (current.equals(session) && current.ready && isLinked(VOLUME)) {
            publish(current, PLATFORM, "getvolume", "");
        }
    }

    @Override
    public synchronized void handleCommand(ChannelUID channelUID, Command command) {
        Session current = session;
        if (current == null || !current.ready) {
            return;
        }
        switch (channelUID.getId()) {
            case VOLUME -> {
                if (command == RefreshType.REFRESH) {
                    publish(current, PLATFORM, "getvolume", "");
                } else if (command instanceof PercentType percent) {
                    publish(current, PLATFORM, "changevolume", Integer.toString(percent.intValue()));
                } else if (command instanceof IncreaseDecreaseType direction) {
                    sendKey(current, direction == IncreaseDecreaseType.INCREASE ? "KEY_VOLUMEUP" : "KEY_VOLUMEDOWN");
                }
            }
            case KEY -> {
                if (command instanceof StringType key && KEY_CODE.matcher(key.toString()).matches()) {
                    sendKey(current, key.toString());
                }
            }
            case SOURCES, SOURCE -> {
                if (command == RefreshType.REFRESH) {
                    publish(current, UI, "sourcelist", "");
                } else if (SOURCE.equals(channelUID.getId()) && command instanceof StringType id) {
                    String name = current.sources.get(id.toString());
                    if (name != null) {
                        JsonObject payload = new JsonObject();
                        payload.addProperty("sourceid", id.toString());
                        payload.addProperty("sourcename", name);
                        publish(current, UI, "changesource", payload.toString());
                    }
                }
            }
        }
    }

    synchronized CompletableFuture<Boolean> requestPairing() {
        Session current = session;
        if (current == null || !current.ready || current.pairingResult != null) {
            return CompletableFuture.completedFuture(false);
        }
        current.pinDeadline = System.nanoTime() + PIN_WINDOW_NANOS;
        return publish(current, UI, "gettvstate", "").orTimeout(10, TimeUnit.SECONDS).exceptionally(error -> false);
    }

    synchronized CompletableFuture<PairingResult> submitPairingPin(String pin) {
        if (!pin.matches("[0-9]{4}")) {
            return CompletableFuture.completedFuture(PairingResult.INVALID_PIN);
        }
        Session current = session;
        if (current == null || !current.ready || current.pinDeadline == 0
                || current.pinDeadline - System.nanoTime() <= 0 || current.pairingResult != null) {
            return CompletableFuture.completedFuture(PairingResult.REQUEST_REQUIRED);
        }

        current.pinDeadline = 0;
        CompletableFuture<PairingResult> result = new CompletableFuture<>();
        current.pairingResult = result;
        result.completeOnTimeout(PairingResult.NO_RESPONSE, 10, TimeUnit.SECONDS).whenComplete((value, error) -> {
            synchronized (this) {
                if (result.equals(current.pairingResult)) {
                    current.pairingResult = null;
                }
            }
        });

        JsonObject payload = new JsonObject();
        payload.addProperty("authNum", pin);
        publish(current, UI, "authenticationcode", payload.toString()).whenComplete((sent, error) -> {
            if (!Boolean.TRUE.equals(sent)) {
                result.complete(PairingResult.DISCONNECTED);
            }
        });
        return result;
    }

    enum PairingResult {
        ACCEPTED,
        REJECTED,
        INVALID_PIN,
        REQUEST_REQUIRED,
        NO_RESPONSE,
        DISCONNECTED
    }

    private void sendKey(Session current, String key) {
        publish(current, REMOTE, "sendkey", key);
    }

    private CompletableFuture<Boolean> publish(Session current, String service, String action, String payload) {
        if (current.broker.connectionState() != MqttConnectionState.CONNECTED) {
            return CompletableFuture.completedFuture(false);
        }
        return current.broker.publish(actionTopic(current.device, service, action),
                payload.getBytes(StandardCharsets.UTF_8), 0, false).exceptionally(error -> {
                    logger.debug("Unable to send Hisense TV action {}", action);
                    return false;
                });
    }

    private synchronized void receive(Session current, String topic, byte[] bytes) {
        if (!current.equals(session) || current.broker.connectionState() != MqttConnectionState.CONNECTED) {
            return;
        }
        boolean volumeTopic = (current.responsePrefix + VOLUME_RESPONSE).equals(topic)
                || LEGACY_VOLUME_TOPIC.equals(topic);
        boolean sourcesTopic = (current.responsePrefix + SOURCES_RESPONSE).equals(topic);
        boolean pairingTopic = (current.responsePrefix + PAIRING_RESPONSE).equals(topic);
        if (!volumeTopic && !sourcesTopic && !pairingTopic) {
            return;
        }
        try {
            JsonElement payload = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            if (pairingTopic) {
                CompletableFuture<PairingResult> result = current.pairingResult;
                int status = pairingStatus(payload);
                if (result != null && status >= 0) {
                    result.complete(status == 1 ? PairingResult.ACCEPTED : PairingResult.REJECTED);
                }
            } else if (volumeTopic) {
                int volume = volume(payload);
                if (volume >= 0) {
                    updateState(VOLUME, new PercentType(volume));
                }
            } else if (payload instanceof JsonArray array) {
                Map<String, String> sources = sources(array);
                if (array.isEmpty() || !sources.isEmpty()) {
                    current.sources = sources;
                    updateState(SOURCES, new StringType(array.toString()));
                }
            }
        } catch (JsonParseException e) {
            logger.debug("Ignoring malformed Hisense TV response");
        }
    }

    private static final class Session {
        final MqttBrokerConnection broker;
        final String device;
        final String responsePrefix;
        boolean ready;
        long pinDeadline;
        @Nullable
        CompletableFuture<PairingResult> pairingResult;
        Map<String, String> sources = Map.of();
        @Nullable
        ScheduledFuture<?> poll;

        Session(MqttBrokerConnection broker, String device) {
            this.broker = broker;
            this.device = device;
            this.responsePrefix = responsePrefix(device);
        }

        void cancelPairing() {
            pinDeadline = 0;
            CompletableFuture<PairingResult> pending = pairingResult;
            if (pending != null) {
                pending.complete(PairingResult.DISCONNECTED);
            }
        }
    }
}
