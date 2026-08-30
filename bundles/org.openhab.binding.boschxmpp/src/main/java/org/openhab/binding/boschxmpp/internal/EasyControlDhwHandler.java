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
import java.util.Set;

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.boschxmpp.internal.protocol.BoschXmppException;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/** Handles one domestic hot-water circuit. */
@NonNullByDefault
public class EasyControlDhwHandler extends EasyControlChildHandler {

    private static final Set<String> MODES = Set.of("Off", "high", "ownprogram", "eco");

    private final Logger logger = LoggerFactory.getLogger(EasyControlDhwHandler.class);

    private String circuitId = "";
    private volatile boolean targetTemperatureWritable;

    public EasyControlDhwHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        circuitId = getConfigAs(EasyControlDhwConfiguration.class).circuitId;
        initializeChild(circuitId.matches("dhw[1-9][0-9]*"), "Circuit ID must have the form dhw1, dhw2, ...");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            scheduler.execute(this::refresh);
        } else if (CHANNEL_DHW_MODE.equals(channelUID.getId())) {
            String mode = command.toString();
            if (MODES.contains(mode)) {
                scheduler.execute(() -> writeMode(mode));
            }
        } else if (CHANNEL_DHW_TARGET_TEMPERATURE.equals(channelUID.getId())) {
            @Nullable
            Double temperature = temperatureFrom(command);
            if (temperature == null || temperature < 30 || temperature > 80) {
                return;
            }
            scheduler.execute(() -> writeTargetTemperature(temperature));
        }
    }

    @Override
    void refresh() {
        if (isDisposed()) {
            return;
        }
        EasyControlBridgeHandler bridge = getBridgeHandler();
        if (bridge == null) {
            return;
        }
        String path = "/dhwCircuits/" + circuitId;
        try {
            if (!verifyEndpoint(bridge, path + "/operationMode")) {
                return;
            }
            readOptional(bridge, path + "/actualTemp", CHANNEL_DHW_TEMPERATURE,
                    value -> new QuantityType<Temperature>(value.getAsBigDecimal(), SIUnits.CELSIUS));
            readTargetTemperature(bridge, path);
            readOptional(bridge, path + "/operationMode", CHANNEL_DHW_MODE,
                    value -> new StringType(value.getAsString()));
            readOptional(bridge, path + "/state", CHANNEL_DHW_STATUS, value -> new StringType(value.getAsString()));
            updateStatus(ThingStatus.ONLINE);
        } catch (BoschXmppException e) {
            updateFailureStatus(e);
        }
    }

    private void writeMode(String mode) {
        EasyControlBridgeHandler bridge = getBridgeHandler();
        if (bridge == null) {
            return;
        }
        try {
            updateValue(bridge.putAndConfirm("/dhwCircuits/" + circuitId + "/operationMode", new JsonPrimitive(mode)),
                    CHANNEL_DHW_MODE, value -> new StringType(value.getAsString()));
        } catch (BoschXmppException e) {
            updateFailureStatus(e);
            refresh();
        }
    }

    private void writeTargetTemperature(double temperature) {
        if (!targetTemperatureWritable) {
            logger.warn("EasyControl does not currently allow changing the hot-water target temperature");
            return;
        }
        EasyControlBridgeHandler bridge = getBridgeHandler();
        if (bridge == null) {
            return;
        }
        try {
            applyTargetTemperatureResponse(bridge.putAndConfirm("/dhwCircuits/" + circuitId + "/temperatureLevels/high",
                    new JsonPrimitive(BigDecimal.valueOf(temperature))));
        } catch (BoschXmppException e) {
            // A rejected write does not mean the bridge lost connectivity. Refresh the authoritative state so an
            // optimistic UI update is corrected without taking the whole DHW Thing offline.
            logger.warn("EasyControl rejected the hot-water target temperature change: {}", e.getMessage());
            refresh();
        } catch (RuntimeException e) {
            logger.warn("Invalid EasyControl hot-water target temperature confirmation", e);
            targetTemperatureWritable = false;
            updateState(CHANNEL_DHW_TARGET_TEMPERATURE, UnDefType.UNDEF);
        }
    }

    private void readTargetTemperature(EasyControlBridgeHandler bridge, String path) throws BoschXmppException {
        if (!isChannelLinked(CHANNEL_DHW_TARGET_TEMPERATURE)) {
            return;
        }
        targetTemperatureWritable = false;
        try {
            applyTargetTemperatureResponse(bridge.get(path + "/temperatureLevels/high"));
        } catch (BoschXmppException e) {
            if (e.getReason() == BoschXmppException.Reason.INVALID_RESPONSE) {
                updateState(CHANNEL_DHW_TARGET_TEMPERATURE, UnDefType.UNDEF);
            } else {
                throw e;
            }
        } catch (RuntimeException e) {
            logger.warn("Invalid EasyControl hot-water target temperature response", e);
            updateState(CHANNEL_DHW_TARGET_TEMPERATURE, UnDefType.UNDEF);
        }
    }

    private void applyTargetTemperatureResponse(JsonObject response) {
        targetTemperatureWritable = EasyControlEndpointMetadata.isWritable(response);
        @Nullable
        JsonElement value = response.get("value");
        updateState(CHANNEL_DHW_TARGET_TEMPERATURE, value == null || value.isJsonNull() ? UnDefType.UNDEF
                : new QuantityType<Temperature>(value.getAsBigDecimal(), SIUnits.CELSIUS));
        if (!targetTemperatureWritable) {
            logger.trace("EasyControl hot-water target temperature is read-only: {}", response);
        }
    }

    private static @Nullable Double temperatureFrom(Command command) {
        if (command instanceof QuantityType<?> quantity) {
            @Nullable
            QuantityType<?> celsius = quantity.toUnit(SIUnits.CELSIUS);
            return celsius == null ? null : celsius.doubleValue();
        }
        return command instanceof DecimalType decimal ? decimal.doubleValue() : null;
    }
}
