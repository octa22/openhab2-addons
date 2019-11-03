/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.smarthome.core.library.types.*;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.jablotron.internal.Utils;
import org.openhab.binding.jablotron.internal.model.JablotronTrouble;
import org.openhab.binding.jablotron.internal.model.ja100.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.openhab.binding.jablotron.JablotronBindingConstants.*;

/**
 * The {@link JablotronJa100Handler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class JablotronJa100Handler extends JablotronAlarmHandler {

    private final Logger logger = LoggerFactory.getLogger(JablotronJa100Handler.class);

    public JablotronJa100Handler(Thing thing, HttpClient httpClient) {
        super(thing, httpClient);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String section = channelUID.getId();
        logger.debug("handleCommand(): Command Received - {} {}.", channelUID, command);
        if (!isAlarmSection(section) && command instanceof OnOffType) {
            logger.info("sending command: {} to PG section: {}", command.toString(), section);
            scheduler.execute(() -> sendCommand(section, command));
        }

        if (isAlarmSection(section)) {
            logger.info("sending command: {} to STATUS section: {}", command.toString(), section);
            if (command instanceof StringType || command instanceof DecimalType) {
                scheduler.execute(() -> sendCommand(section, command.toString()));
            }
        }
    }

    private boolean isAlarmSection(String channel) {
        return channel.contains("STATE");
    }

    private synchronized Ja100StatusResponse sendGetStatusRequest() {

        String url = JABLOTRON_URL + "app/ja100/ajax/stav.php?" + Utils.getBrowserTimestamp();
        try {
            ContentResponse resp = httpClient.newRequest(url)
                    .method(HttpMethod.GET)
                    .header(HttpHeader.ACCEPT_LANGUAGE, "cs-CZ")
                    .header(HttpHeader.ACCEPT_ENCODING, "gzip, deflate")
                    .header(HttpHeader.REFERER, JABLOTRON_URL + JA100_SERVICE_URL + thing.getUID().getId())
                    .header("X-Requested-With", "XMLHttpRequest")
                    .agent(AGENT)
                    .timeout(TIMEOUT, TimeUnit.SECONDS)
                    .send();

            String line = resp.getContentAsString();

            logger.info("getStatus response: {}", line);
            return gson.fromJson(line, Ja100StatusResponse.class);
        } catch (TimeoutException ste) {
            logger.error("Timeout during getting alarm status!");
            return null;
        } catch (ExecutionException | InterruptedException e) {
            logger.error("sendGetStatusRequest exception", e);
            return null;
        }
    }

    protected synchronized boolean updateAlarmStatus() {
        logger.debug("updating alarm status...");

        try {
            // relogin every hour
            int hours = Utils.getHoursOfDay();
            if (lastHours >= 0 && lastHours != hours) {
                relogin();
            }
            lastHours = hours;

            Ja100StatusResponse response = sendGetStatusRequest();

            if (response == null || response.getStatus() != 200) {
                login();
                initializeService();
                response = sendGetStatusRequest();
            }
            if (response == null) {
                logger.error("Null status response received");
                return false;
            }

            if (response.isBusyStatus()) {
                logger.warn("JA100 is busy...giving up");
                logout();
                return false;
            }

            ArrayList<Ja100Event> history = getServiceHistory();
            if (history != null) {
                logger.debug("History log contains {} events", history.size());
                if (history.size() > 0) {
                    Ja100Event event = history.get(0);
                    updateLastEvent(event);
                    logger.info("Last event: {} is of class: {} has section: {}", event.getEvent(), event.getEventClass(), event.getSection());
                }
            }

            if (response.isAlarm()) {
                logger.info("It seems that alarm has been triggered!!!");
            }

            if (response.hasTroubles()) {
                ArrayList<JablotronTrouble> troubles = response.getTroubles();
                for (JablotronTrouble trouble : troubles) {
                    logger.debug("Found trouble: {} {} {} ({})", trouble.getZekdy(), trouble.getCas(), trouble.getMessage(), trouble.getName());
                }
                updateLastTrouble(troubles.get(0));
            }

            inService = response.inService();

            if (inService) {
                logger.warn("Alarm is in service mode...");
                return false;
            }

            if (!response.isOKStatus()) {
                logger.error("Cannot get alarm status!");
                return false;
            }

            if (response.hasSectionStatus()) {
                processSections(response);
            }
            if (response.hasPGMStatus()) {
                processPGMs(response);
            }

            if (response.hasTemperature()) {
                processTemperatures(response);
            }
            //update last check time
            updateState(CHANNEL_LAST_CHECK_TIME, getCheckTime());

            return true;
        } catch (Exception ex) {
            logger.error("Error during alarm status update", ex);
            return false;
        }
    }

    private void processTemperatures(Ja100StatusResponse response) {
        ArrayList<Ja100Temperature> temps = response.getTemperatures();
        if (temps == null) {
            return;
        }

        for (Ja100Temperature temp : temps) {
            logger.debug("Found a temperature sensor: {} with value: {}", temp.getStateName(), temp.getValue());
            Channel channel = getThing().getChannel(temp.getStateName());
            if (channel == null) {
                logger.debug("Creating a new channel: {}", temp.getStateName());
                createTemperatureChannel(temp);
            }
            channel = getThing().getChannel(temp.getStateName());
            if (channel != null) {
                logger.debug("Updating channel: {} to value: {}", channel.getUID(), temp.getValue());
                updateState(channel.getUID(), new DecimalType(temp.getValue()));
            } else {
                logger.warn("The channel: {} still doesn't exist!", temp.getStateName());
            }
        }
    }

    private void processSections(Ja100StatusResponse response) {
        ArrayList<Ja100Section> secs = response.getSections();
        for (Ja100Section section : secs) {
            logger.info("Found a section: {} with status: {}", section.getNazev(), section.getStav());
            Channel channel = getThing().getChannel(section.getStateName());
            if (channel == null) {
                logger.info("Creating a new channel: {}", section.getStateName());
                createSectionChannel(section);
            }
            channel = getThing().getChannel(section.getStateName());
            if (channel != null) {
                logger.info("Updating channel: {} to value: {}", channel.getUID(), section.getStav());
                updateState(channel.getUID(), new DecimalType(section.getStav()));
            } else {
                logger.warn("The channel: {} still doesn't exist!", section.getStateName());
            }
        }
    }

    private void processPGMs(Ja100StatusResponse response) {
        ArrayList<Ja100Section> secs = response.getPGMs();
        for (Ja100Section section : secs) {
            logger.info("Found a PGM: {} with status: {}", section.getNazev(), section.getStav());
            Channel channel = getThing().getChannel(section.getStateName());
            if (channel == null) {
                logger.info("Creating a new channel: {}", section.getStateName());
                createPGMChannel(section);
            }
            channel = getThing().getChannel(section.getStateName());
            if (channel != null) {
                logger.info("Updating channel: {} to value: {}", channel.getUID(), section.getStav());
                updateState(channel.getUID(), section.getStav() == 1 ? OnOffType.ON : OnOffType.OFF);
            } else {
                logger.warn("The channel: {} still doesn't exist!", section.getStateName());
            }
        }
    }

    private void createSectionChannel(Ja100Section section) {
        createChannel(section.getStateName(), "Number", section.getNazev());
    }

    private void createPGMChannel(Ja100Section section) {
        createChannel(section.getStateName(), "Switch", section.getNazev());
    }

    private void createTemperatureChannel(Ja100Temperature temp) {
        createChannel(temp.getStateName(), "Number", temp.getStateName());
    }

    private void createChannel(String name, String type, String label) {
        ThingBuilder thingBuilder = editThing();
        Channel channel = ChannelBuilder.create(new ChannelUID(thing.getUID(), name), type).withLabel(label).build();
        thingBuilder.withChannel(channel);
        updateThing(thingBuilder.build());
    }

    private void updateLastEvent(Ja100Event event) {
        updateState(CHANNEL_LAST_EVENT_TIME, new DateTimeType(event.getZonedDateTime()));
        updateState(CHANNEL_LAST_EVENT_SECTION, new StringType(event.getSection()));
        updateState(CHANNEL_LAST_EVENT, new StringType(event.getEvent()));
        updateState(CHANNEL_LAST_EVENT_CLASS, new StringType(event.getEventClass()));
    }

    public void sendCommand(String section, Command command) {
        sendCommand(section, command.equals(OnOffType.ON) ? "1" : "0");
    }

    public synchronized void sendCommand(String section, String code) {
        int status;
        Integer result;
        try {
            if (!isReady()) {
                return;
            }

            Ja100ControlResponse response = sendUserCode(section, code);
            if (response == null) {
                logger.warn("null response received");
                return;
            }

            status = response.getResponseCode();
            result = response.getResult();
            if (result != null) {
                handleHttpRequestStatus(status);
            } else {
                logger.warn("null status received");
                logout();
            }
        } catch (Exception e) {
            logger.error("internalReceiveCommand exception", e);
        }
    }

    private boolean isReady() {
        if (!getThing().getStatus().equals(ThingStatus.ONLINE)) {
            login();
            initializeService();
        }
        if (!getThing().getStatus().equals(ThingStatus.ONLINE)) {
            logger.error("Cannot send user code - alarm is not online!");
            return false;
        }
        return true;
    }

    protected synchronized Ja100ControlResponse sendUserCode(String section, String status) {
        String url;

        try {
            url = JABLOTRON_URL + "app/" + thing.getThingTypeUID().getId() + "/ajax/ovladani2.php";
            String urlParameters = "section=" + section + "&status=" + status + "&code=" + thingConfig.getCode();

            logger.info("Sending POST to url address: {} to control section: {}", url, section);

            ContentResponse resp = httpClient.newRequest(url)
                    .method(HttpMethod.POST)
                    .header(HttpHeader.ACCEPT_LANGUAGE, "cs-CZ")
                    .header(HttpHeader.ACCEPT_ENCODING, "gzip, deflate")
                    .header(HttpHeader.REFERER, getServiceUrl())
                    .header("X-Requested-With", "XMLHttpRequest")
                    .agent(AGENT)
                    .content(new StringContentProvider(urlParameters), "application/x-www-form-urlencoded; charset=UTF-8")
                    .timeout(TIMEOUT, TimeUnit.SECONDS)
                    .send();

            String line = resp.getContentAsString();


            logger.info("Control response: {}", line);
            Ja100ControlResponse response = gson.fromJson(line, Ja100ControlResponse.class);
            logger.debug("sendUserCode result: {}", response.getResult());
            return response;
        } catch (TimeoutException ex) {
            logger.error("sendUserCode timeout exception", ex);
        } catch (ExecutionException | InterruptedException ex) {
            logger.error("sendUserCode exception", ex);
        }
        return null;
    }

    private ArrayList<Ja100Event> getServiceHistory() {
        String serviceId = thing.getUID().getId();
        try {
            String url = "https://www.jablonet.net/app/ja100/ajax/historie.php";
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

            logger.debug("History response: {}", line);

            ArrayList<Ja100Event> result = new ArrayList<>();

            JsonParser parser = new JsonParser();
            JsonObject jobject = parser.parse(line).getAsJsonObject();
            if (jobject.has("ResponseCode") && jobject.get("ResponseCode").getAsInt() == 200) {
                logger.trace("History successfully retrieved with total of {} events.", jobject.get("EventsCount").getAsInt());
                if (jobject.has("HistoryData")) {
                    jobject = jobject.get("HistoryData").getAsJsonObject();
                    if (jobject.has("Events")) {
                        JsonArray jarray = jobject.get("Events").getAsJsonArray();
                        logger.trace("Parsing events...");
                        Ja100Event[] events = gson.fromJson(jarray, Ja100Event[].class);
                        result.addAll(Arrays.asList(events));
                        logger.trace("Last event: {}", events[0].toString());
                    }
                }
            }
            return result;
        } catch (ExecutionException | InterruptedException | TimeoutException ex) {
            logger.error("Cannot get Jablotron service history: {}", serviceId, ex);
        }
        return null;
    }
}
