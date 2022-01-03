/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.abbegon.internal;

import static org.openhab.binding.abbegon.internal.ABBEgonBindingConstants.BINDING_ID;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * The {@link ABBEgonHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class ABBEgonHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(ABBEgonHandler.class);

    private @NonNullByDefault ABBEgonConfiguration config = new ABBEgonConfiguration();
    private String device = "";
    private @Nullable ScheduledFuture<?> future = null;
    private Map<ChannelUID, Command> cache = new HashMap<>();

    /**
     * The shared HttpClient
     */
    private final HttpClient httpClient;

    public ABBEgonHandler(Thing thing, HttpClientFactory httpClientFactory) {
        super(thing);
        this.httpClient = httpClientFactory.getCommonHttpClient();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received command: {} for channel: {}", command, channelUID);
        if (command instanceof RefreshType) {
            // there is no forcing of channel refreshing
            return;
        }
        ChannelTypeUID tid = getThing().getChannel(channelUID).getChannelTypeUID();
        if ("rollershutter".equals(tid.getId())) {
            if (command.equals(StopMoveType.STOP)) {
                doEgonAction(channelUID.getId(), getLastCommand(channelUID));
                return;
            } else {
                cacheCommand(channelUID, command);
            }
        }
        doEgonAction(channelUID.getId(), command);
    }

    private void cacheCommand(ChannelUID channelUID, Command command) {
        // Caching of the last command so the STOP can send the last command and stop the device
        if (cache.containsKey(channelUID)) {
            logger.debug("Replacing cached command: {} for device id: {}", command, channelUID.getId());
            cache.replace(channelUID, command);
        } else {
            logger.debug("Caching command: {} for device id: {}", command, channelUID.getId());
            cache.put(channelUID, command);
        }
    }

    private @Nullable Command getLastCommand(ChannelUID channelUID) {
        logger.debug("Getting cached command for device id: {}", channelUID.getId());
        return cache.get(channelUID);
    }

    private synchronized void doEgonAction(String id, @Nullable Command command) {
        if (command != null) {
            logger.debug("About to send command: {} to device id: {}", command, id);
            String url = getHost() + "/action.html?action=" + command + "&device=" + device + "&id=" + id;
            String line = getHttpResponse(url);
            if (!"OK".equals(line)) {
                logger.debug("Received invalid response: {}", line);
            }
        } else {
            logger.debug("Received null command for device id: {}", id);
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(ABBEgonConfiguration.class);

        updateStatus(ThingStatus.UNKNOWN);

        // Background initialization:
        scheduler.execute(() -> {
            if (doInit()) {
                updateStatus(ThingStatus.ONLINE);
                logout();
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            }
        });
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> localFuture = future;
        if (localFuture != null) {
            localFuture.cancel(true);
        }
        logout();
        cache.clear();
        super.dispose();
    }

    private boolean doInit() {
        future = scheduler.scheduleWithFixedDelay(() -> {
            execute();
        }, 5, config.refreshInterval, TimeUnit.SECONDS);
        return login();
    }

    private void execute() {
        String status = getEgonStatus();
        if (status == null) {
            login();
            status = getEgonStatus();
        }

        if (status != null) {
            updateStatus(ThingStatus.ONLINE);
            parseXML(status);
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    private void parseXML(String status) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(status)));

            NodeList list = document.getElementsByTagName("element_states");
            for (Node childNode = list.item(0).getFirstChild(); childNode != null;) {
                Node nextChild = childNode.getNextSibling();
                logger.trace("NodeName: {}, attributes: {}", childNode.getNodeName(), childNode.hasAttributes());
                if ("element_state".equals(childNode.getNodeName()) && childNode.hasAttributes()) {
                    String id = childNode.getAttributes().item(0).getNodeValue();
                    String value = childNode.getAttributes().item(1).getNodeValue();
                    logger.debug("id: {}, value: {}", id, value);
                    createChannel(id, value);
                    updateChannelStatus(id, value);
                }
                childNode = nextChild;
            }
        } catch (Exception ex) {
            logger.error(ex.toString());
        }
    }

    private void updateChannelStatus(String id, String value) {
        Channel channel = thing.getChannel(id);
        if (channel != null) {
            State state = null;
            if ("on".equals(value) || "off".equals(value)) {
                state = ("on".equals(value)) ? OnOffType.ON : OnOffType.OFF;
            } else if (value.startsWith("down") || value.startsWith("up")) {
                state = new PercentType(value.startsWith("down") ? "100" : "0");
            } else if (value.contains(".")) {
                state = new DecimalType(value);
            }

            if (state != null) {
                updateState(channel.getUID(), state);
            }
        }
    }

    private void createChannel(String id, String value) {
        if (thing.getChannel(id) == null) {
            logger.debug("Creating channel for id: {} with value: {}", id, value);
            if ("on".equals(value) || "off".equals(value)) {
                createSwitchChannel(id);
            } else if (value.startsWith("down") || value.startsWith("up")) {
                createRollershutterChannel(id);
            } else if (value.contains(".")) {
                createNumberChannel(id);
            } else {
                logger.debug("Unable to guess channel for id: {} with value: {}", id, value);
            }
        }
    }

    private void createNumberChannel(String id) {
        createChannel(id, "Number", "Number " + id, new ChannelTypeUID(BINDING_ID, "number"));
    }

    private void createRollershutterChannel(String id) {
        createChannel(id, "Rollershutter", "Roller shutter " + id, new ChannelTypeUID(BINDING_ID, "rollershutter"));
    }

    private void createSwitchChannel(String id) {
        createChannel(id, "Switch", "Switch " + id, new ChannelTypeUID(BINDING_ID, "switch"));
    }

    private @Nullable String getEgonStatus() {
        String url = getHost() + "/state.html?device=" + device;
        String line = getHttpResponse(url);

        if (line != null && line.startsWith("<?xml ")) {
            return line;
        } else {
            logger.debug("Not a XML status response!");
            return null;
        }
    }

    private void logout() {
        logger.debug("Doing logout...");
        String url = getHost() + "/revoke.html?device=" + device;

        getHttpResponse(url);
    }

    private boolean login() {
        String url = null;

        logger.debug("Logging in...");
        url = getHost() + "/authorize.html?user=" + config.user + "&password=" + config.password;
        String line = getHttpResponse(url);
        if (line != null) {
            device = line.replace("device=", "").replace("\n", "");

            if (!tryParseInt(device)) {
                logger.debug("device response is not a number: {}", device);
                device = "";
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    private String getHost() {
        return "http://" + config.hostname + ":" + config.port;
    }

    private @Nullable String getHttpResponse(String url) {
        try {
            logger.trace("Sending HTTP GET request: {}", url);
            ContentResponse response = httpClient.GET(url);
            String text = response.getContentAsString();
            if (logger.isTraceEnabled()) {
                logger.trace("HTTP response: {}", text);
            }
            return text;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.debug("Got an exception", e);
            return null;
        }
    }

    boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void createChannel(String name, String type, String label, ChannelTypeUID typeUID) {
        ThingBuilder thingBuilder = editThing();
        Channel channel = ChannelBuilder.create(new ChannelUID(thing.getUID(), name), type).withLabel(label)
                .withType(typeUID).build();
        thingBuilder.withChannel(channel);
        updateThing(thingBuilder.build());
    }
}
