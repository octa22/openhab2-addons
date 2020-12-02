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

import java.awt.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;

/**
 * The {@link SomfyTahomaDynamicLightHandler} is responsible for handling commands,
 * which are sent to one of the channels of the dynamic light thing.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class SomfyTahomaDynamicLightHandler extends SomfyTahomaBaseThingHandler {

    public SomfyTahomaDynamicLightHandler(Thing thing) {
        super(thing);
        stateNames.put(LIGHT_INTENSITY, "core:IntensityState");
        stateNames.put(SWITCH, "core:OnOffState");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        super.handleCommand(channelUID, command);
        if (command instanceof RefreshType) {
            return;
        } else if (LIGHT_INTENSITY.equals(channelUID.getId())) {
            if (command instanceof OnOffType) {
                sendCommand(command.toString().toLowerCase());
            } else {
                sendCommand("setIntensity", "[" + toInteger(command) + "]");
            }
        } else if (SWITCH.equals(channelUID.getId()) && command instanceof OnOffType) {
            getLogger().info("setting switch to: {}", command);
            sendCommand(command.toString().toLowerCase());
        } else if (COLOR.equals(channelUID.getId()) && command instanceof HSBType) {
            HSBType color = (HSBType) command;
            String param = "[" + toColor(color.getRed()) + "," + toColor(color.getGreen()) + ","
                    + toColor(color.getBlue()) + "]";
            getLogger().info("about to set color to: {}", param);
            sendCommand("setRGB", param);
        }
    }

    private int toColor(PercentType percent) {
        return Double.valueOf(percent.intValue() * 2.55).intValue();
    }
}
