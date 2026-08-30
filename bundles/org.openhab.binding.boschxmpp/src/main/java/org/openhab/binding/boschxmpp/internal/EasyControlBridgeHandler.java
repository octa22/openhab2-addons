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

import static org.openhab.binding.boschxmpp.internal.BoschXmppBindingConstants.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.boschxmpp.internal.EasyControlEndpointMetadata.HeatingPumpValue;
import org.openhab.binding.boschxmpp.internal.discovery.EasyControlDiscoveryService;
import org.openhab.binding.boschxmpp.internal.protocol.BoschXmppException;
import org.openhab.binding.boschxmpp.internal.protocol.BoschXmppException.Reason;
import org.openhab.binding.boschxmpp.internal.protocol.EasyControlClient;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/** Handles the XMPP connection and controller-wide data for one EasyControl gateway. */
@NonNullByDefault
public class EasyControlBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(EasyControlBridgeHandler.class);

    private @Nullable EasyControlClient client;
    private @Nullable ScheduledFuture<?> pollingJob;
    private final AtomicBoolean refreshRunning = new AtomicBoolean();
    private volatile boolean disposed;
    private volatile boolean firmwareReadAttempted;

    public EasyControlBridgeHandler(Thing thing) {
        super((Bridge) thing);
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return List.of(EasyControlDiscoveryService.class);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            scheduler.execute(this::refresh);
        } else if (CHANNEL_AWAY_MODE.equals(channelUID.getId()) && command instanceof OnOffType onOff) {
            scheduler.execute(() -> {
                try {
                    // The EasyControl API exposes this boolean as a string value ("true"/"false").
                    // A JSON boolean is accepted when reading but rejected with HTTP 400 when writing.
                    JsonObject response = putAndConfirm("/system/awayMode/enabled",
                            new JsonPrimitive(Boolean.toString(onOff == OnOffType.ON)));
                    readValue(response, CHANNEL_AWAY_MODE,
                            value -> EasyControlEndpointMetadata.booleanValue(value) ? OnOffType.ON : OnOffType.OFF);
                } catch (BoschXmppException e) {
                    updateFailureStatus(e);
                    refresh();
                } catch (RuntimeException e) {
                    logger.warn("Invalid EasyControl away-mode confirmation", e);
                    updateState(CHANNEL_AWAY_MODE, UnDefType.UNDEF);
                }
            });
        } else if (CHANNEL_ROOM_INFLUENCE.equals(channelUID.getId())) {
            try {
                BigDecimal roomInfluence = new BigDecimal(command.toString());
                scheduler.execute(() -> writeRoomInfluence(roomInfluence));
            } catch (NumberFormatException e) {
                logger.warn("Room influence must be a number: {}", command);
            }
        } else if (CHANNEL_NOTIFICATION_LIGHT.equals(channelUID.getId()) && command == OnOffType.ON) {
            scheduler.execute(this::activateNotificationLight);
        }
    }

    @Override
    public void initialize() {
        disposed = false;
        firmwareReadAttempted = false;
        BoschXmppConfiguration config = getConfigAs(BoschXmppConfiguration.class);
        if (config.serialNumber.isBlank() || config.accessKey.isBlank() || config.devicePassword.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Serial number, access key and device password are required");
            return;
        }
        if (config.refreshInterval < 10 || config.requestTimeout < 1 || config.port < 1 || config.port > 65535) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Invalid refresh interval, request timeout or port");
            return;
        }

        try {
            client = new EasyControlClient(config.hostname, config.port, config.serialNumber, config.accessKey,
                    config.devicePassword, Duration.ofSeconds(config.requestTimeout));
        } catch (BoschXmppException e) {
            updateFailureStatus(e);
            return;
        }

        getThing().setProperty(Thing.PROPERTY_SERIAL_NUMBER, config.serialNumber);
        getThing().setProperty(Thing.PROPERTY_MODEL_ID, "EasyControl");
        updateStatus(ThingStatus.UNKNOWN);
        pollingJob = scheduler.scheduleWithFixedDelay(this::refresh, 0, config.refreshInterval, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        disposed = true;
        ScheduledFuture<?> job = pollingJob;
        pollingJob = null;
        if (job != null) {
            job.cancel(true);
        }
        EasyControlClient current = client;
        client = null;
        if (current != null) {
            current.close();
        }
        super.dispose();
    }

    public JsonObject get(String endpoint) throws BoschXmppException {
        EasyControlClient current = client;
        if (current == null) {
            throw new BoschXmppException(Reason.COMMUNICATION, "EasyControl bridge is not connected");
        }
        return current.get(endpoint);
    }

    public void put(String endpoint, JsonElement value) throws BoschXmppException {
        EasyControlClient current = client;
        if (current == null) {
            throw new BoschXmppException(Reason.COMMUNICATION, "EasyControl bridge is not connected");
        }
        current.put(endpoint, value);
    }

    /** Writes a value and reads it back for trace-level command confirmation. */
    public JsonObject putAndConfirm(String endpoint, JsonElement value) throws BoschXmppException {
        try {
            put(endpoint, value);
        } catch (BoschXmppException writeError) {
            if (writeError.getReason() != Reason.COMMUNICATION) {
                throw writeError;
            }
            // The connection can fail after the gateway has applied a PUT but before its acknowledgement arrives.
            // Read the endpoint after reconnecting instead of blindly repeating a potentially non-idempotent command.
            try {
                JsonObject response = get(endpoint);
                @Nullable
                JsonElement actual = response.get("value");
                if (actual != null && EasyControlEndpointMetadata.valuesEquivalent(actual, value)) {
                    logger.trace("EasyControl command for {} succeeded despite a lost acknowledgement", endpoint);
                    return response;
                }
            } catch (BoschXmppException confirmationError) {
                writeError.addSuppressed(confirmationError);
            }
            throw writeError;
        }
        JsonObject response = get(endpoint);
        @Nullable
        JsonElement confirmedValue = response.get("value");
        logger.trace("EasyControl command confirmed for {}: {}", endpoint,
                confirmedValue == null || confirmedValue.isJsonNull() ? "null" : confirmedValue);
        return response;
    }

    private void writeRoomInfluence(BigDecimal roomInfluence) {
        try {
            JsonObject response = putAndConfirm("/heatingCircuits/hc1/roomInfluence", new JsonPrimitive(roomInfluence));
            readValue(response, CHANNEL_ROOM_INFLUENCE, value -> new DecimalType(value.getAsBigDecimal()));
        } catch (BoschXmppException e) {
            updateFailureStatus(e);
            refresh();
        } catch (RuntimeException e) {
            logger.warn("Invalid EasyControl room-influence confirmation", e);
            updateState(CHANNEL_ROOM_INFLUENCE, UnDefType.UNDEF);
        }
    }

    private void activateNotificationLight() {
        JsonObject notification = new JsonObject();
        notification.addProperty("color", "green");
        notification.addProperty("mode", "on");
        notification.addProperty("time", 10);
        JsonArray notifications = new JsonArray();
        notifications.add(notification);
        try {
            put("/gateway/notificationLight/activate", notifications);
            updateState(CHANNEL_NOTIFICATION_LIGHT, OnOffType.OFF);
        } catch (BoschXmppException e) {
            updateFailureStatus(e);
            refresh();
        }
    }

    private void refresh() {
        if (disposed || !refreshRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            EasyControlClient current = client;
            if (current == null) {
                return;
            }
            // Keep the Bridge status authoritative even when no channels are linked and therefore no GET is needed.
            current.connect();
            readFirmware();
            readOptional("/system/sensors/temperatures/outdoor_t1", CHANNEL_OUTDOOR_TEMPERATURE,
                    this::temperatureState);
            readOptional("/system/appliance/systemPressure", CHANNEL_SYSTEM_PRESSURE,
                    value -> new QuantityType<>(value.getAsBigDecimal(), Units.BAR));
            readOptional("/heatSources/actualSupplyTemperature", CHANNEL_SUPPLY_TEMPERATURE, this::temperatureState);
            readOptional("/heatSources/actualModulation", CHANNEL_MODULATION,
                    value -> new PercentType(value.getAsBigDecimal()));
            readOptional("/heatSources/flameIndication", CHANNEL_FLAME_INDICATION,
                    value -> new StringType(value.getAsString()));
            readHeatingPump();
            readOptional("/heatingCircuits/hc1/roomInfluence", CHANNEL_ROOM_INFLUENCE,
                    value -> new DecimalType(value.getAsBigDecimal()));
            readOptional("/heatingCircuits/hc1/supplyTemperatureSetpoint", CHANNEL_TARGET_SUPPLY_TEMPERATURE,
                    this::temperatureState);
            readOptional("/system/awayMode/enabled", CHANNEL_AWAY_MODE,
                    value -> EasyControlEndpointMetadata.booleanValue(value) ? OnOffType.ON : OnOffType.OFF);
            updateStatus(ThingStatus.ONLINE);

            for (Thing child : getThing().getThings()) {
                if (child.isEnabled() && child.getHandler() instanceof EasyControlChildHandler handler) {
                    handler.refresh();
                }
            }
        } catch (BoschXmppException e) {
            updateFailureStatus(e);
            updateChildrenOffline("EasyControl bridge communication failed");
        } catch (RuntimeException e) {
            logger.debug("Could not process EasyControl response", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Could not process an EasyControl response");
            updateChildrenOffline("EasyControl bridge communication failed");
        } finally {
            refreshRunning.set(false);
        }
    }

    private void readFirmware() throws BoschXmppException {
        boolean linked = isChannelLinked(CHANNEL_FIRMWARE_VERSION);
        if (!linked && firmwareReadAttempted) {
            return;
        }
        try {
            JsonObject firmware = get("/gateway/versionFirmware");
            firmwareReadAttempted = true;
            if (linked) {
                readValue(firmware, CHANNEL_FIRMWARE_VERSION, value -> new StringType(value.getAsString()));
            }
            @Nullable
            JsonElement firmwareValue = firmware.get("value");
            if (firmwareValue != null && !firmwareValue.isJsonNull()) {
                getThing().setProperty(Thing.PROPERTY_FIRMWARE_VERSION, firmwareValue.getAsString());
            }
        } catch (BoschXmppException e) {
            if (e.getReason() == Reason.INVALID_RESPONSE) {
                firmwareReadAttempted = true;
                if (linked) {
                    updateState(CHANNEL_FIRMWARE_VERSION, UnDefType.UNDEF);
                }
            } else {
                throw e;
            }
        } catch (RuntimeException e) {
            firmwareReadAttempted = true;
            logger.warn("Invalid EasyControl value from /gateway/versionFirmware for channel {}",
                    CHANNEL_FIRMWARE_VERSION, e);
            if (linked) {
                updateState(CHANNEL_FIRMWARE_VERSION, UnDefType.UNDEF);
            }
        }
    }

    private void readHeatingPump() throws BoschXmppException {
        boolean stateLinked = isChannelLinked(CHANNEL_HEATING_PUMP);
        boolean modulationLinked = isChannelLinked(CHANNEL_HEATING_PUMP_MODULATION);
        if (!stateLinked && !modulationLinked) {
            return;
        }
        try {
            JsonObject response = get("/heatSources/CHpumpModulation");
            @Nullable
            HeatingPumpValue pumpValue = EasyControlEndpointMetadata.heatingPumpValue(response);
            if (pumpValue == null) {
                updateHeatingPumpStates(stateLinked, modulationLinked, UnDefType.UNDEF, UnDefType.UNDEF);
            } else {
                @Nullable
                BigDecimal modulation = pumpValue.modulation();
                updateHeatingPumpStates(stateLinked, modulationLinked, new StringType(pumpValue.state()),
                        modulation == null ? UnDefType.UNDEF : new PercentType(modulation));
            }
        } catch (BoschXmppException e) {
            if (e.getReason() == Reason.INVALID_RESPONSE) {
                updateHeatingPumpStates(stateLinked, modulationLinked, UnDefType.UNDEF, UnDefType.UNDEF);
            } else {
                throw e;
            }
        } catch (RuntimeException e) {
            logger.warn("Invalid EasyControl value from /heatSources/CHpumpModulation", e);
            updateHeatingPumpStates(stateLinked, modulationLinked, UnDefType.UNDEF, UnDefType.UNDEF);
        }
    }

    private void updateHeatingPumpStates(boolean stateLinked, boolean modulationLinked, State state, State modulation) {
        if (stateLinked) {
            updateState(CHANNEL_HEATING_PUMP, state);
        }
        if (modulationLinked) {
            updateState(CHANNEL_HEATING_PUMP_MODULATION, modulation);
        }
    }

    private void readOptional(String endpoint, String channel, Function<JsonElement, State> converter)
            throws BoschXmppException {
        if (!isChannelLinked(channel)) {
            return;
        }
        try {
            readValue(get(endpoint), channel, converter);
        } catch (BoschXmppException e) {
            if (e.getReason() == Reason.INVALID_RESPONSE) {
                updateState(channel, UnDefType.UNDEF);
            } else {
                throw e;
            }
        } catch (RuntimeException e) {
            logger.warn("Invalid EasyControl value from {} for channel {}", endpoint, channel, e);
            updateState(channel, UnDefType.UNDEF);
        }
    }

    private void readValue(JsonObject response, String channel, Function<JsonElement, State> converter) {
        @Nullable
        JsonElement value = response.get("value");
        updateState(channel,
                value == null || value.isJsonNull() ? UnDefType.UNDEF : Objects.requireNonNull(converter.apply(value)));
    }

    private State temperatureState(JsonElement value) {
        return new QuantityType<Temperature>(value.getAsBigDecimal(), SIUnits.CELSIUS);
    }

    private void updateFailureStatus(BoschXmppException e) {
        logger.debug("EasyControl communication failed: {}", e.getMessage(), e);
        if (e.getReason() == Reason.INVALID_RESPONSE) {
            logger.warn("EasyControl rejected a request: {}", e.getMessage());
            return;
        }
        ThingStatusDetail detail = e.getReason() == Reason.AUTHENTICATION || e.getReason() == Reason.DECRYPTION
                ? ThingStatusDetail.CONFIGURATION_ERROR
                : ThingStatusDetail.COMMUNICATION_ERROR;
        updateStatus(ThingStatus.OFFLINE, detail, e.getMessage());
    }

    private void updateChildrenOffline(String description) {
        for (Thing child : getThing().getThings()) {
            if (child.getHandler() instanceof EasyControlChildHandler handler) {
                handler.bridgeOffline(description);
            }
        }
    }

    private boolean isChannelLinked(String channelId) {
        return isLinked(channelId);
    }
}
