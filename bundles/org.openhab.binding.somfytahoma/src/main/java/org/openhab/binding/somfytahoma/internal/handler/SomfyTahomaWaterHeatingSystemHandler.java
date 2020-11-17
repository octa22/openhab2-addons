/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.somfytahoma.internal.handler;

import static org.openhab.binding.somfytahoma.internal.SomfyTahomaBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;

/**
 * The {@link SomfyTahomaWaterHeatingSystemHandler} is responsible for handling commands,
 * which are sent to one of the channels of the water heating system thing.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class SomfyTahomaWaterHeatingSystemHandler extends SomfyTahomaBaseThingHandler {

    public SomfyTahomaWaterHeatingSystemHandler(Thing thing) {
        super(thing);
        stateNames.put(SWITCH, "core:DHWOnOffState");
        stateNames.put(BOOST, "core:BoostOnOffState");
        stateNames.put(WATER_HEATER_MODE, "io:PassAPCDHWModeState");
        stateNames.put(WATER_HEATER_PROFILE, "io:PassAPCDHWProfileState");
        stateNames.put(TARGET_TEMPERATURE, "core:TargetDHWTemperatureState");
        stateNames.put(TARGET_TEMP_ECO, "core:EcoTargetDHWTemperatureState");
        stateNames.put(TARGET_TEMP_COMFORT, "core:ComfortTargetDHWTemperatureState");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        super.handleCommand(channelUID, command);
        if (command instanceof RefreshType) {
            return;
        } else {
            String param = "[\"" + command.toString().toLowerCase() + "\"]";
            switch (channelUID.getId()) {
                case SWITCH:
                    sendCommand("setDHWOnOffState", param);
                    break;
                case BOOST:
                    sendCommand("setBoostOnOffState", param);
                    break;
                case WATER_HEATER_MODE:
                    sendCommand("setPassAPCDHWMode", param);
                    break;
                case TARGET_TEMP_ECO:
                    setTargetTemperature("setEcoTargetDHWTemperature", command);
                    break;
                case TARGET_TEMP_COMFORT:
                    setTargetTemperature("setComfortTargetDHWTemperature", command);
                    break;
                default:
                    getLogger().debug("The channel: {} is read only!", channelUID.getId());
            }
        }
    }

    private void setTargetTemperature(String cmd, Command value) {
        if (value instanceof QuantityType) {
            QuantityType type = (QuantityType) value;
            String param = "[" + type.doubleValue() + "]";
            sendCommand(cmd, param);
        } else {
            getLogger().debug("Received command: {} is not a QuantityType", value);
        }
    }
}
