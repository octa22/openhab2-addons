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
package org.openhab.binding.jablotron.internal.handler;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.jablotron.internal.model.JablotronControlResponse;
import org.openhab.binding.jablotron.internal.model.JablotronHistoryDataEvent;
import org.openhab.binding.jablotron.internal.model.JablotronServiceDetailSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link JablotronJa100Handler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class JablotronJa100Handler extends JablotronAlarmHandler {

    private final Logger logger = LoggerFactory.getLogger(JablotronJa100Handler.class);

    public JablotronJa100Handler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        if (channelUID.getId().startsWith("STATE_") && command instanceof OnOffType) {
            scheduler.execute(() -> {
                controlSTATESection(channelUID.getId(), command.equals(OnOffType.ON) ? "set" : "unset");
            });
        }

        if (channelUID.getId().startsWith("PGM_") && command instanceof OnOffType) {
            scheduler.execute(() -> {
                controlPGMSection(channelUID.getId(), command.equals(OnOffType.ON) ? "set" : "unset");
            });
        }
    }

    private void createChannel(JablotronServiceDetailSegment section) {
        createChannel(section.getSegmentId(), "Switch", section.getSegmentName());
    }

    private void createChannel(String name, String type, String label) {
        ThingBuilder thingBuilder = editThing();
        Channel channel = ChannelBuilder.create(new ChannelUID(thing.getUID(), name), type).withLabel(label).build();
        thingBuilder.withChannel(channel);
        updateThing(thingBuilder.build());
    }

    protected synchronized @Nullable List<JablotronHistoryDataEvent> sendGetEventHistory() {
        return sendGetEventHistory("JA100");
    }

    protected void updateSegmentStatus(JablotronServiceDetailSegment segment) {
        logger.debug("Segment id: {} and status: {}", segment.getSegmentId(), segment.getSegmentState());
        State newState = "unset".equals(segment.getSegmentState()) ? OnOffType.OFF : OnOffType.ON;

        if (segment.getSegmentId().startsWith("STATE_") || segment.getSegmentId().startsWith("PGM_")) {
            String name = segment.getSegmentId();
            Channel channel = getThing().getChannel(name);
            if (channel == null) {
                logger.info("Creating a new channel: {}", name);
                createChannel(segment);
            }
            channel = getThing().getChannel(name);
            if (channel != null) {
                logger.info("Updating channel: {} to value: {}", channel.getUID(), segment.getSegmentState());
                updateState(channel.getUID(), newState);
            } else {
                logger.warn("The channel: {} still doesn't exist!", segment.getSegmentId());
            }
        } else {
            logger.info("Unknown segment received: {} with state: {}", segment.getSegmentId(), segment.getSegmentState());
        }
    }

    public synchronized void controlPGMSection(String section, String status) {
        logger.debug("Controlling section: {} with status: {}", section, status);
        JablotronControlResponse response = sendUserCode(section, section.toLowerCase(), status, thingConfig.getCode());

        updateAlarmStatus();
        if (response == null) {
            logger.warn("null response/status received");
            //logout();
        }
    }

    public synchronized void controlSTATESection(String section, String status) {
        logger.debug("Controlling section: {} with status: {}", section, status);
        JablotronControlResponse response = sendUserCode(section, section.toLowerCase().replace("state", "section"), status, thingConfig.getCode());

        updateAlarmStatus();
        if (response == null) {
            logger.warn("null response/status received");
            //logout();
        }
    }
}
