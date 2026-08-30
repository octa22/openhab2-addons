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
import java.util.Locale;
import java.util.Set;

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.boschxmpp.internal.protocol.BoschXmppException;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonPrimitive;

/** Handles one heating zone exposed by an EasyControl controller. */
@NonNullByDefault
public class EasyControlZoneHandler extends EasyControlChildHandler {

    private static final Set<String> OPERATION_MODES = Set.of("clock", "manual");

    private final Logger logger = LoggerFactory.getLogger(EasyControlZoneHandler.class);

    private String zoneId = "";

    public EasyControlZoneHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        zoneId = getConfigAs(EasyControlZoneConfiguration.class).zoneId;
        initializeChild(zoneId.matches("zn[1-9][0-9]*"), "Zone ID must have the form zn1, zn2, ...");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            scheduler.execute(this::refresh);
            return;
        }

        if (CHANNEL_TARGET_TEMPERATURE.equals(channelUID.getId())) {
            @Nullable
            Double temperature = temperatureFrom(command);
            if (temperature == null || temperature < 5 || temperature > 30 || !isHalfDegree(temperature)) {
                logger.warn("Target temperature must be between 5 and 30 °C in 0.5 °C steps");
                return;
            }
            scheduler.execute(() -> writeTargetTemperature(temperature));
        } else if (CHANNEL_OPERATION_MODE.equals(channelUID.getId())) {
            String mode = command.toString().toLowerCase(Locale.ROOT);
            if (!OPERATION_MODES.contains(mode)) {
                logger.warn("Unsupported EasyControl operation mode: {}", mode);
                return;
            }
            scheduler.execute(() -> write("/zones/" + zoneId + "/userMode", mode));
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
        String path = "/zones/" + zoneId;
        try {
            if (!verifyEndpoint(bridge, path + "/userMode")) {
                return;
            }
            readOptional(bridge, path + "/temperatureActual", CHANNEL_ROOM_TEMPERATURE,
                    value -> new QuantityType<Temperature>(value.getAsBigDecimal(), SIUnits.CELSIUS));
            readOptional(bridge, path + "/temperatureHeatingSetpoint", CHANNEL_TARGET_TEMPERATURE,
                    value -> new QuantityType<Temperature>(value.getAsBigDecimal(), SIUnits.CELSIUS));
            readOptional(bridge, path + "/userMode", CHANNEL_OPERATION_MODE,
                    value -> new StringType(value.getAsString()));
            readOptional(bridge, path + "/status", CHANNEL_HEATING_STATUS,
                    value -> new StringType(value.getAsString()));
            readOptional(bridge, path + "/humidity", CHANNEL_HUMIDITY, this::humidityState);
            updateStatus(ThingStatus.ONLINE);
        } catch (BoschXmppException e) {
            updateFailureStatus(e);
        }
    }

    private void writeTargetTemperature(double temperature) {
        EasyControlBridgeHandler bridge = getBridgeHandler();
        if (bridge == null) {
            return;
        }
        try {
            String mode = bridge.get("/zones/" + zoneId + "/userMode").get("value").getAsString();
            String endpoint = "manual".equals(mode) ? "/zones/" + zoneId + "/manualTemperatureHeating"
                    : "/zones/" + zoneId + "/clockOverride/temperatureHeating";
            bridge.putAndConfirm(endpoint, new JsonPrimitive(BigDecimal.valueOf(temperature)));
            refreshTargetTemperature(bridge);
        } catch (BoschXmppException e) {
            updateFailureStatus(e);
            refresh();
        } catch (RuntimeException e) {
            logger.warn("Invalid EasyControl zone mode response while changing target temperature", e);
            refresh();
        }
    }

    private void write(String endpoint, String value) {
        EasyControlBridgeHandler bridge = getBridgeHandler();
        if (bridge == null) {
            return;
        }
        try {
            updateValue(bridge.putAndConfirm(endpoint, new JsonPrimitive(value)), CHANNEL_OPERATION_MODE,
                    confirmed -> new StringType(confirmed.getAsString()));
            refreshTargetTemperature(bridge);
        } catch (BoschXmppException e) {
            updateFailureStatus(e);
            refresh();
        }
    }

    private void refreshTargetTemperature(EasyControlBridgeHandler bridge) throws BoschXmppException {
        readOptional(bridge, "/zones/" + zoneId + "/temperatureHeatingSetpoint", CHANNEL_TARGET_TEMPERATURE,
                value -> new QuantityType<Temperature>(value.getAsBigDecimal(), SIUnits.CELSIUS));
    }

    private static @Nullable Double temperatureFrom(Command command) {
        if (command instanceof QuantityType<?> quantity) {
            @Nullable
            QuantityType<?> celsius = quantity.toUnit(SIUnits.CELSIUS);
            return celsius == null ? null : celsius.doubleValue();
        }
        return command instanceof DecimalType decimal ? decimal.doubleValue() : null;
    }

    private static boolean isHalfDegree(double value) {
        return Math.abs(value * 2 - Math.rint(value * 2)) < 0.0001;
    }

    private PercentType humidityState(com.google.gson.JsonElement value) {
        BigDecimal humidity = value.getAsBigDecimal();
        // EasyControl represents humidity in tenths of a percent (for example 496 means 49.6 %).
        return new PercentType(humidity.compareTo(BigDecimal.valueOf(100)) > 0 ? humidity.movePointLeft(1) : humidity);
    }
}
