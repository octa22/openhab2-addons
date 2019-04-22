/**
 * Copyright (c) 2014,2019 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.orbitbhyve.internal.handler;

import static org.openhab.binding.orbitbhyve.internal.OrbitBHyveBindingConstants.*;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.orbitbhyve.internal.model.OrbitBHyveDevice;
import org.openhab.binding.orbitbhyve.internal.model.OrbitBHyveDeviceStatus;
import org.openhab.binding.orbitbhyve.internal.model.OrbitBHyveProgram;
import org.openhab.binding.orbitbhyve.internal.model.OrbitBHyveZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OrbitBHyveSprinklerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class OrbitBHyveSprinklerHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(OrbitBHyveSprinklerHandler.class);

    public OrbitBHyveSprinklerHandler(Thing thing) {
        super(thing);
    }

    private int wateringTime = 5;
    private HashMap<String, OrbitBHyveProgram> programs = new HashMap<>();

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        OrbitBHyveBridgeHandler handler = getBridgeHandler();
        if (handler != null) {
            String deviceId = thing.getUID().getId();
            if (CHANNEL_CONTROL.equals(channelUID.getId()) && command instanceof OnOffType) {
                String mode = OnOffType.ON.equals(command) ? "auto" : "off";
                handler.changeRunMode(deviceId, mode);
                return;
            }
            if (CHANNEL_SMART_WATERING.equals(channelUID.getId()) && command instanceof OnOffType) {
                boolean enable = OnOffType.ON.equals(command);
                handler.setSmartWatering(deviceId, enable);
                return;
            }
            if (!channelUID.getId().startsWith("enable_program") && OnOffType.OFF.equals(command)) {
                handler.stopWatering(deviceId);
                return;
            }
            if (CHANNEL_WATERING_TIME.equals(channelUID.getId()) && command instanceof DecimalType) {
                wateringTime = ((DecimalType) command).intValue();
                updateState(CHANNEL_WATERING_TIME, (DecimalType) command);
                return;
            }
            if (channelUID.getId().startsWith("zone")) {
                if (OnOffType.ON.equals(command)) {
                    handler.runZone(deviceId, channelUID.getId().replace("zone_", ""), wateringTime);
                }
                return;
            }
            if (channelUID.getId().startsWith("program")) {
                if (OnOffType.ON.equals(command)) {
                    handler.runProgram(deviceId, channelUID.getId().replace("program_", ""));
                }
                return;
            }
            if (channelUID.getId().startsWith("enable_program") && command instanceof OnOffType) {
                String id = channelUID.getId().replace("enable_program_", "");
                handler.enableProgram(programs.get(id), OnOffType.ON.equals(command));
                return;
            }
            if (CHANNEL_RAIN_DELAY.equals(channelUID.getId()) && command instanceof DecimalType) {
                handler.setRainDelay(deviceId, ((DecimalType) command).intValue());
            }
        }
    }

    private @Nullable OrbitBHyveBridgeHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            return (OrbitBHyveBridgeHandler) bridge.getHandler();
        }
        return null;
    }

    @Override
    public void initialize() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            logger.debug("Initializing, bridge is {}", bridge.getStatus());
            if (ThingStatus.ONLINE == bridge.getStatus()) {
                doInit();
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        }
    }

    private synchronized void doInit() {
        OrbitBHyveBridgeHandler handler = getBridgeHandler();
        if (handler != null) {
            OrbitBHyveDevice device = handler.getDevice(thing.getUID().getId());
            if (device != null) {
                setDeviceOnline(device.isConnected());
                createChannels(device.getZones());
                updateDeviceStatus(device.getStatus());
            }
            List<OrbitBHyveProgram> programs = handler.getPrograms();
            for (OrbitBHyveProgram program : programs) {
                if (thing.getUID().getId().equals(program.getDeviceId())) {
                    cacheProgram(program);
                    createProgram(program);
                }
            }

            updateState(CHANNEL_WATERING_TIME, new DecimalType(wateringTime));
            logger.debug("Finished initializing of sprinkler!");
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            doInit();
        }
    }

    private synchronized void cacheProgram(OrbitBHyveProgram program) {
        if (!programs.containsKey(program.getProgram())) {
            programs.put(program.getProgram(), program);
        }
    }

    public void updateDeviceStatus(OrbitBHyveDeviceStatus status) {
        if (StringUtils.isNotEmpty(status.getMode())) {
            updateState(CHANNEL_MODE, new StringType(status.getMode()));
            updateState(CHANNEL_CONTROL, "off".equals(status.getMode()) ? OnOffType.OFF : OnOffType.ON);
        }
        if (StringUtils.isNotEmpty(status.getNextStartTime())) {
            DateTimeType dt = new DateTimeType(status.getNextStartTime());
            updateState(CHANNEL_NEXT_START, dt);
            logger.debug("Next start time: {}", status.getNextStartTime());
        }
        updateState(CHANNEL_RAIN_DELAY, new DecimalType(status.getDelay()));
    }

    private void createProgram(OrbitBHyveProgram program) {
        String channelName = "program_" + program.getProgram();
        if (thing.getChannel(channelName) == null) {
            logger.debug("Creating channel for program: {} with name: {}", program.getProgram(), program.getName());
            createChannel(channelName, "Switch", "Program " + program.getName());
        }
        String enableChannelName = "enable_" + channelName;
        if (thing.getChannel(enableChannelName) == null) {
            logger.debug("Creating enable channel for program: {} with name: {}", program.getProgram(),
                    program.getName());
            createChannel(enableChannelName, "Switch", "Enable program " + program.getName());
        }
        Channel ch = thing.getChannel(enableChannelName);
        if (ch != null) {
            updateState(ch.getUID(), program.isEnabled() ? OnOffType.ON : OnOffType.OFF);
        }
    }

    private void createChannels(List<OrbitBHyveZone> zones) {
        for (OrbitBHyveZone zone : zones) {
            String channelName = "zone_" + zone.getStation();
            if (thing.getChannel(channelName) == null) {
                logger.debug("Creating channel for zone: {} with name: {}", zone.getStation(), zone.getName());
                createChannel(channelName, "Switch", "Zone " + zone.getName());
            }
        }
    }

    private void createChannel(String name, String type, String label) {
        ThingBuilder thingBuilder = editThing();
        Channel channel = ChannelBuilder.create(new ChannelUID(thing.getUID(), name), type).withLabel(label).build();
        thingBuilder.withChannel(channel);
        updateThing(thingBuilder.build());
    }

    public void setDeviceOnline(boolean connected) {
        if (!connected) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Not connected to Orbit BHyve Cloud");
        } else {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    public void updateProgram(OrbitBHyveProgram program) {
        String enableChannelName = "enable_program_" + program.getProgram();
        Channel ch = thing.getChannel(enableChannelName);
        if (ch != null) {
            updateState(ch.getUID(), program.isEnabled() ? OnOffType.ON : OnOffType.OFF);
        }
    }

    public void updateSmartWatering(String senseMode) {
        updateState(CHANNEL_SMART_WATERING, (senseMode.equals("auto")) ? OnOffType.ON : OnOffType.OFF);
    }
}
