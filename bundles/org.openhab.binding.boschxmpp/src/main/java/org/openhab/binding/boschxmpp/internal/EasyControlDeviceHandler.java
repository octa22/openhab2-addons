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

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.boschxmpp.internal.protocol.BoschXmppException;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/** Handles a thermostat or radiator valve paired with an EasyControl controller. */
@NonNullByDefault
public class EasyControlDeviceHandler extends EasyControlChildHandler {

    private String deviceId = "";

    public EasyControlDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        deviceId = getConfigAs(EasyControlDeviceConfiguration.class).deviceId;
        initializeChild(deviceId.matches("device[1-9][0-9]*"), "Device ID must have the form device1, device2, ...");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            scheduler.execute(this::refresh);
        } else if (CHANNEL_CHILD_LOCK.equals(channelUID.getId()) && command instanceof OnOffType onOff) {
            scheduler.execute(() -> writeChildLock(onOff == OnOffType.ON));
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
        String path = "/devices/" + deviceId;
        try {
            if (!verifyEndpoint(bridge, path + "/type")) {
                return;
            }
            readOptional(bridge, path + "/thermostat/childLock/enabled", CHANNEL_CHILD_LOCK,
                    value -> EasyControlEndpointMetadata.booleanValue(value) ? OnOffType.ON : OnOffType.OFF);
            readOptional(bridge, path + "/etrv/temperatureActual", CHANNEL_DEVICE_TEMPERATURE,
                    value -> new QuantityType<Temperature>(value.getAsBigDecimal(), SIUnits.CELSIUS));
            readOptional(bridge, path + "/etrv/valvePosition", CHANNEL_VALVE_POSITION,
                    value -> new PercentType(value.getAsBigDecimal()));
            readOptional(bridge, path + "/battery", CHANNEL_BATTERY, value -> new StringType(value.getAsString()));
            readOptional(bridge, path + "/signal", CHANNEL_SIGNAL, value -> new PercentType(value.getAsBigDecimal()));
            readOptional(bridge, path + "/type", CHANNEL_DEVICE_TYPE, value -> new StringType(value.getAsString()));
            readOptional(bridge, path + "/versionFirmware", CHANNEL_FIRMWARE_VERSION,
                    value -> new StringType(value.getAsString()));
            readOptional(bridge, path + "/zone", CHANNEL_DEVICE_ZONE,
                    value -> new DecimalType(value.getAsBigDecimal()));
            updateStatus(ThingStatus.ONLINE);
        } catch (BoschXmppException e) {
            updateFailureStatus(e);
        }
    }

    private void writeChildLock(boolean enabled) {
        EasyControlBridgeHandler bridge = getBridgeHandler();
        if (bridge == null) {
            return;
        }
        try {
            JsonObject response = bridge.putAndConfirm("/devices/" + deviceId + "/thermostat/childLock/enabled",
                    new JsonPrimitive(Boolean.toString(enabled)));
            updateValue(response, CHANNEL_CHILD_LOCK,
                    value -> EasyControlEndpointMetadata.booleanValue(value) ? OnOffType.ON : OnOffType.OFF);
        } catch (BoschXmppException e) {
            updateFailureStatus(e);
            refresh();
        }
    }
}
