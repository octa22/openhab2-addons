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

import static org.openhab.binding.jablotron.JablotronBindingConstants.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.jablotron.internal.model.*;
import org.openhab.binding.jablotron.internal.model.JablotronControlResponse;
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

    public JablotronJa100Handler(Thing thing, HttpClient httpClient) {
        super(thing, httpClient);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_COMMAND) && command instanceof StringType) {
            scheduler.execute(() -> {
                sendCommand(command.toString());
            });
        }

        if (channelUID.getId().equals(CHANNEL_STATUS_PGX) && command instanceof OnOffType) {
            scheduler.execute(() -> {
                controlSection("PGM_1", command.equals(OnOffType.ON) ? "set" : "unset");
            });
        }

        if (channelUID.getId().equals(CHANNEL_STATUS_PGY) && command instanceof OnOffType) {
            scheduler.execute(() -> {
                controlSection("PGM_2", command.equals(OnOffType.ON) ? "set" : "unset");
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
        }
    }

    public synchronized void controlSection(String section, String status) {
        logger.debug("Controlling section: {} with status: {}", section, status);
        JablotronControlResponse response = sendUserCode(section, section.toLowerCase(), status, "");

        updateAlarmStatus();
        if (response == null) {
            logger.warn("null response/status received");
            //logout();
        }
    }

    public synchronized void sendCommand(String code) {
        try {
            JablotronControlResponse response = sendUserCode(code);
            scheduler.schedule(this::updateAlarmStatus, 1, TimeUnit.SECONDS);

            if (response == null) {
                logger.warn("null response/status received");
                //logout();
            }
        } catch (Exception e) {
            logger.error("internalReceiveCommand exception", e);
        }
    }

    protected synchronized @Nullable JablotronControlResponse sendUserCode(String section, String key, String status, String code) {
        String url;

        try {
            url = JABLOTRON_API_URL + "controlSegment.json";
            String urlParameters = "service=oasis&serviceId=" + thing.getUID().getId() + "&segmentId=" + section + "&segmentKey=" + key + "&expected_status=" + status + "&control_time=0&control_code=" + code + "&system=" + SYSTEM;
            logger.debug("Sending POST to url address: {} to control section: {}", url, section);

            ContentResponse resp = httpClient.newRequest(url)
                    .method(HttpMethod.POST)
                    .header(HttpHeader.ACCEPT_LANGUAGE, "cs")
                    .header(HttpHeader.ACCEPT_ENCODING, "*")
                    .header("x-vendor-id", VENDOR)
                    .agent(AGENT)
                    .content(new StringContentProvider(urlParameters), "application/x-www-form-urlencoded; charset=UTF-8")
                    .timeout(TIMEOUT, TimeUnit.SECONDS)
                    .send();

            String line = resp.getContentAsString();

            logger.trace("Control response: {}", line);
            JablotronControlResponse response = gson.fromJson(line, JablotronControlResponse.class);
            if (!response.isStatus()) {
                logger.debug("Error during sending user code: {}", response.getErrorMessage());
            }
            return response;
        } catch (TimeoutException ex) {
            logger.debug("sendUserCode timeout exception", ex);
        } catch (Exception ex) {
            logger.debug("sendUserCode exception", ex);
        }
        return null;
    }

    private synchronized @Nullable JablotronControlResponse sendUserCode(String code) {
        return sendUserCode("sections", "button_1", "partialSet", code);
    }
}
