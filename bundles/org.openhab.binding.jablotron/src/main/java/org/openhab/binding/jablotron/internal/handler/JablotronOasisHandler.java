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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.jablotron.internal.model.*;
import org.openhab.binding.jablotron.internal.model.oasis.OasisControlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link JablotronOasisHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class JablotronOasisHandler extends JablotronAlarmHandler {

    private final Logger logger = LoggerFactory.getLogger(JablotronOasisHandler.class);

    public JablotronOasisHandler(Thing thing, HttpClient httpClient) {
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

    private synchronized @Nullable List<JablotronHistoryDataEvent> sendGetEventHistory() {

        String url = JABLOTRON_API_URL + "OASIS/eventHistoryGet.json";
        String urlParameters = "{\"limit\":1, \"service-id\":" + thing.getUID().getId() + "}";

        try {
            ContentResponse resp = httpClient.newRequest(url)
                    .method(HttpMethod.POST)
                    .header(HttpHeader.ACCEPT_LANGUAGE, "cs")
                    .header(HttpHeader.ACCEPT_ENCODING, "*")
                    .header(HttpHeader.ACCEPT, "application/json")
                    .header("x-vendor-id", VENDOR)
                    .agent(AGENT)
                    .content(new StringContentProvider(urlParameters), "application/json")
                    .timeout(LONG_TIMEOUT, TimeUnit.SECONDS)
                    .send();

            String line = resp.getContentAsString();
            logger.trace("get event history: {}", line);
            JablotronGetEventHistoryResponse response = gson.fromJson(line, JablotronGetEventHistoryResponse.class);
            if (200 != response.getHttpCode()) {
                logger.debug("Got error while getting history with http code: {}", response.getHttpCode());
            }
            return response.getData().getEvents();
        } catch (TimeoutException ste) {
            logger.debug("Timeout during getting alarm history!");
            return null;
        } catch (Exception e) {
            logger.debug("sendGetEventHistory exception", e);
            return null;
        }
    }

    private synchronized @Nullable JablotronDataUpdateResponse sendGetStatusRequest() {

        String url = JABLOTRON_API_URL + "dataUpdate.json";
        String urlParameters = "data=[{ \"filter_data\":[{\"data_type\":\"section\"},{\"data_type\":\"pgm\"}],\"service_type\":\"" + thing.getThingTypeUID().getId() + "\",\"service_id\":" + thing.getUID().getId() + ",\"data_group\":\"serviceData\"}]&system=" + SYSTEM;

        try {
            ContentResponse resp = httpClient.newRequest(url)
                    .method(HttpMethod.POST)
                    .header(HttpHeader.ACCEPT_LANGUAGE, "cs")
                    .header(HttpHeader.ACCEPT_ENCODING, "*")
                    .header("x-vendor-id", VENDOR)
                    .agent(AGENT)
                    .content(new StringContentProvider(urlParameters), "application/x-www-form-urlencoded; charset=UTF-8")
                    .timeout(LONG_TIMEOUT, TimeUnit.SECONDS)
                    .send();

            String line = resp.getContentAsString();
            logger.trace("get status: {}", line);

            return gson.fromJson(line, JablotronDataUpdateResponse.class);
        } catch (TimeoutException ste) {
            logger.debug("Timeout during getting alarm status!");
            return null;
        } catch (Exception e) {
            logger.debug("sendGetStatusRequest exception", e);
            return null;
        }
    }

    protected synchronized boolean updateAlarmStatus() {
        JablotronDataUpdateResponse dataUpdate = sendGetStatusRequest();
        if (dataUpdate == null) {
            return false;
        }

        if (dataUpdate.isStatus()) {
            updateState(CHANNEL_LAST_CHECK_TIME, getCheckTime());
            List<JablotronServiceData> serviceData = dataUpdate.getData().getServiceData();
            for (JablotronServiceData data : serviceData) {
                List<JablotronService> services = data.getData();
                for (JablotronService service : services) {
                    JablotronServiceDetail detail = service.getData();
                    for (JablotronServiceDetailSegment segment : detail.getSegments()) {
                        updateSegmentStatus(segment);
                    }
                }

            }
        } else {
            logger.debug("Error during alarm status update: {}", dataUpdate.getErrorMessage());
        }

        List<JablotronHistoryDataEvent> events = sendGetEventHistory();
        if (events != null && events.size() > 0) {
            JablotronHistoryDataEvent event = events.get(0);
            updateLastEvent(event);
        }

        return true;
    }

    private void updateLastEvent(JablotronHistoryDataEvent event) {
        updateState(CHANNEL_LAST_EVENT_TIME, new DateTimeType(getZonedDateTime(event.getDate())));
        updateState(CHANNEL_LAST_EVENT, new StringType(event.getEventText() + " " + event.getInvokerName()));
        updateState(CHANNEL_LAST_EVENT_CLASS, new StringType(event.getIconType()));
    }

    public ZonedDateTime getZonedDateTime(String date) {
        return ZonedDateTime.parse(date.substring(0,22) + ":" + date.substring(22,24), DateTimeFormatter.ISO_DATE_TIME);
    }

    private void updateSegmentStatus(JablotronServiceDetailSegment segment) {
        logger.debug("Segment id: {} and status: {}", segment.getSegmentId(), segment.getSegmentState());
        State newState = "unset".equals(segment.getSegmentState()) ? OnOffType.OFF : OnOffType.ON;
        switch (segment.getSegmentId()) {
            case "STATE_1":
                updateState(CHANNEL_STATUS_A, newState);
                break;
            case "STATE_2":
                updateState(CHANNEL_STATUS_B, newState);
                break;
            case "STATE_3":
                updateState(CHANNEL_STATUS_ABC, newState);
                break;
            case "PGM_1":
                updateState(CHANNEL_STATUS_PGX, newState);
                break;
            case "PGM_2":
                updateState(CHANNEL_STATUS_PGY, newState);
                break;
            default:
                logger.info("Unknown segment status received: {}", segment.getSegmentId());
        }
    }

    public synchronized void controlSection(String section, String status) {
        logger.debug("Controlling section: {} with status: {}", section, status);
        OasisControlResponse response = sendUserCode(section, section.toLowerCase(), status, "");

        updateAlarmStatus();
        if (response == null) {
            logger.warn("null response/status received");
            logout();
        }
    }

    @Override
    protected synchronized void logout(boolean setOffline) {
    }

    public synchronized void sendCommand(String code) {
        try {
            OasisControlResponse response = sendUserCode(code);
            scheduler.schedule(this::updateAlarmStatus, 1, TimeUnit.SECONDS);

            if (response == null) {
                logger.warn("null response/status received");
                logout();
            }
        } catch (Exception e) {
            logger.error("internalReceiveCommand exception", e);
        }
    }

    private String getControlTime() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    @Override
    protected synchronized void initializeService() {
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    protected synchronized void login() {
        updateStatus(ThingStatus.ONLINE);
    }

    protected synchronized @Nullable OasisControlResponse sendUserCode(String section, String key, String status, String code) {
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
            OasisControlResponse response = gson.fromJson(line, OasisControlResponse.class);
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

    private synchronized @Nullable OasisControlResponse sendUserCode(String code) {
        return sendUserCode("sections", "button_1", "partialSet", code);
    }
}
