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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.jablotron.internal.model.*;
import org.openhab.binding.jablotron.internal.model.oasis.OasisControlResponse;
import org.openhab.binding.jablotron.internal.model.oasis.OasisEvent;
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

    private synchronized @Nullable JablotronDataUpdateResponse sendGetStatusRequest() {

        String url = JABLOTRON_API_URL + "dataUpdate.json";
        String urlParameters = "data=[{ \"filter_data\":[{\"data_type\":\"section\"},{\"data_type\":\"pgm\"}],\"service_type\":\"" + thing.getThingTypeUID().getId() + "\",\"service_id\":" + thing.getUID().getId() + ",\"data_group\":\"serviceData\"}]&client_id=" + CLIENT;

        try {
            ContentResponse resp = httpClient.newRequest(url)
                    .method(HttpMethod.POST)
                    .header(HttpHeader.ACCEPT_LANGUAGE, "cs-CZ")
                    .header(HttpHeader.ACCEPT_ENCODING, "gzip, deflate")
                    //.header("X-Requested-With", "XMLHttpRequest")
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
            logger.error("sendGetStatusRequest exception", e);
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

        /*
        logger.debug("updating alarm status...");

        try {
            // relogin every hour
            int hours = Utils.getHoursOfDay();
            if (lastHours >= 0 && lastHours != hours) {
                relogin();
            }
            lastHours = hours;

            JablotronBridgeHandler handler = (JablotronBridgeHandler) getBridge().getHandler();
            JablotronWidgetsResponse widgets = handler.discoverServices();
            for (JablotronWidget widget : widgets.getWidgets()) {
                if (thing.getUID().getId().equals(String.valueOf(widget.getId()))) {
                    if (widget.getNoticeCount() > 0) {
                        break;
                    }

                    if (widget.getSekce().size() > 0) {
                        updateState(CHANNEL_LAST_CHECK_TIME, getCheckTime());
                        controlDisabled = false;
                        int status = widget.getSekce().get(0).getStatus();
                        if (status == 0) {
                            logger.info("updating alarm status to disarmed");
                            updateState(CHANNEL_STATUS_A, OnOffType.OFF);
                            updateState(CHANNEL_STATUS_B, OnOffType.OFF);
                            updateState(CHANNEL_STATUS_ABC, OnOffType.OFF);
                            initializeService();
                            return true;
                        } else if (status == 5) {
                            logger.info("updating alarm status to A armed");
                            updateState(CHANNEL_STATUS_A, OnOffType.ON);
                            updateState(CHANNEL_STATUS_B, OnOffType.OFF);
                            updateState(CHANNEL_STATUS_ABC, OnOffType.OFF);
                            initializeService();
                            return true;
                        }
                    }
                }
            }

            OasisStatusResponse response = sendGetStatusRequest();

            if (response == null || response.getStatus() != 200) {
                if (response != null) {
                    logger.info("Received response code: {}", response.getStatus());
                }
                controlDisabled = true;
                inService = false;
                login();
                initializeService();
                response = sendGetStatusRequest();
                if (response == null) {
                    return false;
                }
            }
            if (response.isBusyStatus()) {
                logger.warn("OASIS is busy...giving up");
                logout();
                return false;
            }
            if (response.hasTroubles()) {
                ArrayList<JablotronTrouble> troubles = response.getTroubles();
                for (JablotronTrouble trouble : troubles) {
                    logger.debug("Found trouble: {} {} {} ({})", trouble.getZekdy(), trouble.getCas(), trouble.getMessage(), trouble.getName());
                }
                updateLastTrouble(troubles.get(0));
            } else {
                clearTrouble();
            }
            if (response.hasEvents()) {
                ArrayList<OasisEvent> events = response.getEvents();
                for (OasisEvent event : events) {
                    logger.debug("Found event: {} {} {}", event.getDatum(), event.getCode(), event.getEvent());
                    updateLastEvent(event);
                }
            } else {
                ArrayList<OasisEvent> history = getServiceHistory();
                logger.debug("History log contains {} events", history.size());
                if (history.size() > 0) {
                    OasisEvent event = history.get(0);
                    updateLastEvent(event);
                    logger.debug("Last event: {} is of class: {} has code: {}", event.getEvent(), event.getEventClass(), event.getCode());
                }
            }

            inService = response.inService();

            if (inService) {
                logger.warn("Alarm is in service mode...");
                return false;
            }

            if (response.isOKStatus() && response.hasSectionStatus()) {
                readAlarmStatus(response);
            } else {
                logger.error("Cannot get alarm status!");
                return false;
            }
            //update last check time
            updateState(CHANNEL_LAST_CHECK_TIME, getCheckTime());

            return true;
        } catch (Exception ex) {
            logger.error("Error during alarm status update", ex);
            return false;
        }*/
        return true;
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

    private void clearTrouble() {
        updateState(CHANNEL_LAST_TROUBLE, new StringType(""));
        updateState(CHANNEL_LAST_TROUBLE_DETAIL, new StringType(""));
    }

    private void updateLastEvent(OasisEvent event) {
        updateState(CHANNEL_LAST_EVENT_CODE, new StringType(event.getCode()));
        updateState(CHANNEL_LAST_EVENT, new StringType(event.getEvent()));
        updateState(CHANNEL_LAST_EVENT_CLASS, new StringType(event.getEventClass()));
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
            String urlParameters = "service=oasis&serviceId=" + thing.getUID().getId() + "&segmentId=" + section + "&segmentKey=" + key + "&expected_status=" + status + "&control_time=0&control_code=" + code + "&client_id=" + CLIENT;
            logger.debug("Sending POST to url address: {} to control section: {}", url, section);

            ContentResponse resp = httpClient.newRequest(url)
                    .method(HttpMethod.POST)
                    .header(HttpHeader.ACCEPT_LANGUAGE, "cs-CZ")
                    .header(HttpHeader.ACCEPT_ENCODING, "gzip, deflate")
                    .header("X-Requested-With", "XMLHttpRequest")
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

    private @Nullable ArrayList<OasisEvent> getServiceHistory() {
        String serviceId = thing.getUID().getId();
        try {
            String url = "https://www.jablonet.net/app/oasis/ajax/historie.php";
            String urlParameters = "from=this_month&to=&gps=0&log=0&header=0";

            ContentResponse resp = httpClient.newRequest(url)
                    .method(HttpMethod.POST)
                    .header(HttpHeader.ACCEPT_LANGUAGE, "cs-CZ")
                    .header(HttpHeader.ACCEPT_ENCODING, "gzip, deflate")
                    .header(HttpHeader.REFERER, JABLOTRON_URL)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .agent(AGENT)
                    .content(new StringContentProvider(urlParameters), "application/x-www-form-urlencoded; charset=UTF-8")
                    .timeout(TIMEOUT, TimeUnit.SECONDS)
                    .send();

            String line = resp.getContentAsString();

            logger.debug("History: {}", line);

            ArrayList<OasisEvent> result = new ArrayList<>();

            JsonParser parser = new JsonParser();
            JsonObject jobject = parser.parse(line).getAsJsonObject();
            if (jobject.has("events")) {
                jobject = jobject.get("events").getAsJsonObject();

                for (Map.Entry<String, JsonElement> entry : jobject.entrySet()) {
                    String key = entry.getKey();
                    if (jobject.get(key) instanceof JsonArray) {
                        OasisEvent[] events = gson.fromJson(jobject.get(key), OasisEvent[].class);
                        result.addAll(Arrays.asList(events));
                    }
                }
            }
            return result;
        } catch (TimeoutException ex) {
            logger.debug("Timeout during getting Jablotron service history: {}", serviceId, ex);
        } catch (Exception ex) {
            logger.error("Cannot get Jablotron service history: {}", serviceId, ex);
        }
        return null;
    }

}
