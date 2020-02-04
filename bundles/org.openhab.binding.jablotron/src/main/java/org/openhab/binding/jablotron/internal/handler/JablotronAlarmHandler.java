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

import com.google.gson.Gson;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.jablotron.internal.config.DeviceConfig;
import org.openhab.binding.jablotron.internal.Utils;
import org.openhab.binding.jablotron.internal.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.openhab.binding.jablotron.JablotronBindingConstants.*;

/**
 * The {@link JablotronAlarmHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public abstract class JablotronAlarmHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(JablotronAlarmHandler.class);

    protected Gson gson = new Gson();

    protected @Nullable DeviceConfig thingConfig;

    protected boolean inService = true;
    protected int lastHours = Utils.getHoursOfDay();

    @Nullable
    ScheduledFuture<?> future = null;

    public JablotronAlarmHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
    }

    final HttpClient httpClient;

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);
        if (ThingStatus.UNINITIALIZED == bridgeStatusInfo.getStatus()) {
            cleanup();
        }
    }

    private void cleanup() {
        logger.debug("doing cleanup...");
        if (future != null) {
            future.cancel(true);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        cleanup();
    }

    @Override
    public void initialize() {
        thingConfig = getConfigAs(DeviceConfig.class);
        scheduler.execute(() -> {
            doInit();
        });
        updateStatus(ThingStatus.ONLINE);
    }

    /*
    protected void logout() {
        logout(true);
    }

    protected synchronized void logout(boolean setOffline) {
        String url = JABLOTRON_URL + "logout";
        try {
            ContentResponse resp = httpClient.newRequest(url)
                    .method(HttpMethod.GET)
                    .header(HttpHeader.ACCEPT_LANGUAGE, "cs-CZ")
                    .header(HttpHeader.ACCEPT_ENCODING, "gzip, deflate")
                    .header(HttpHeader.REFERER, getServiceUrl())
                    .agent(AGENT)
                    .timeout(5, TimeUnit.SECONDS)
                    .send();
            String line = resp.getContentAsString();

            logger.debug("logout... {}", line);
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            //Silence
        } finally {
            //controlDisabled = true;
            inService = false;
            if (setOffline) {
                updateStatus(ThingStatus.OFFLINE);
            }
        }
    }

    protected void relogin() {
        logger.debug("Doing relogin");
        logout(false);
        login();
        initializeService();
    }*/

    protected State getCheckTime() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Calendar.getInstance().toInstant(), ZoneId.systemDefault());
        return new DateTimeType(zdt);
    }

    /*
    protected synchronized void setLanguage(String lang) throws InterruptedException, ExecutionException, TimeoutException {
        String url = JABLOTRON_URL + "lang/" + lang;

        ContentResponse resp = httpClient.newRequest(url)
                .method(HttpMethod.GET)
                .header(HttpHeader.ACCEPT_LANGUAGE, "cs-CZ")
                .header(HttpHeader.ACCEPT_ENCODING, "gzip, deflate")
                .header(HttpHeader.REFERER, JABLOTRON_URL)
                .agent(AGENT)
                .timeout(TIMEOUT, TimeUnit.SECONDS)
                .send();

        int status = resp.getStatus();
        logger.debug("Set language returned status: {}", status);
    }

    protected synchronized void login() {
        String url;

        try {
            //login
            JablotronBridgeHandler bridge = this.getBridge() != null ? (JablotronBridgeHandler) this.getBridge().getHandler() : null;
            if (bridge == null) {
                logger.error("Bridge handler is null!");
                return;
            }
            url = JABLOTRON_URL + "ajax/login.php";
            String urlParameters = "login=" + URLEncoder.encode(bridge.bridgeConfig.getLogin(), "UTF-8") + "&heslo=" + URLEncoder.encode(bridge.bridgeConfig.getPassword(), "UTF-8") + "&aStatus=200&loginType=Login";

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

            JablotronLoginResponse response = gson.fromJson(line, JablotronLoginResponse.class);

            if (!response.isOKStatus())
                return;

            logger.debug("Successfully logged to Jablonet cloud!");
            if (getLanguage() != null && !getLanguage().equals("cz")) {
                //czech language is default
                setLanguage(getLanguage());
            }
        } catch (TimeoutException e) {
            logger.debug("Timeout during getting login cookie", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to Jablonet cloud");
        } catch (UnsupportedEncodingException | ExecutionException | InterruptedException e) {
            logger.error("Cannot get Jablotron login cookie", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to Jablonet cloud");
        }
    }

    private String getLanguage() {
        JablotronBridgeHandler bridgeHandler = (JablotronBridgeHandler) getBridge().getHandler();
        return bridgeHandler.getBridgeConfig().getLang();
    }

    protected String getServiceUrl() {
        return JABLOTRON_URL + "app/" + thing.getThingTypeUID().getId() + "?service=" + thing.getUID().getId();
    }*/

    protected void doInit() {
        future = scheduler.scheduleWithFixedDelay(() -> {
            updateAlarmStatus();
        }, 1, thingConfig.getRefresh(), TimeUnit.SECONDS);
    }

    /*
    protected synchronized void initializeService() {
        String url = getServiceUrl();
        String serviceId = thing.getUID().getId();
        try {
            ContentResponse resp = httpClient.newRequest(url)
                    .method(HttpMethod.GET)
                    .header(HttpHeader.ACCEPT_LANGUAGE, "cs-CZ")
                    .header(HttpHeader.ACCEPT_ENCODING, "gzip, deflate")
                    .header(HttpHeader.REFERER, JABLOTRON_URL)
                    .agent(AGENT)
                    .timeout(TIMEOUT, TimeUnit.SECONDS)
                    .send();

            if (resp.getStatus() == 200) {
                logger.debug("Jablotron {} service: {} successfully initialized", thing.getThingTypeUID().getId(), serviceId);
                updateStatus(ThingStatus.ONLINE);
            } else {
                logger.debug("Cannot initialize Jablotron service: {}", serviceId);
                logger.debug("Got response code: {}", resp.getStatus());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Cannot initialize " + thing.getThingTypeUID().getId() + " service");
            }
        } catch (TimeoutException e) {
            logger.debug("Timeout during initializing Jablotron service: {}", serviceId, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot initialize " + thing.getThingTypeUID().getId() + " service");
        } catch (ExecutionException | InterruptedException ex) {
            logger.error("Cannot initialize Jablotron service: {}", serviceId, ex);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Cannot initialize " + thing.getThingTypeUID().getId() + " service");
        }
    }

    protected void updateLastTrouble(JablotronTrouble trouble) {
        updateState(CHANNEL_LAST_TROUBLE, new StringType(trouble.getMessage()));
        updateState(CHANNEL_LAST_TROUBLE_DETAIL, new StringType(trouble.getName()));
    }*/

    protected synchronized @Nullable JablotronDataUpdateResponse sendGetStatusRequest() {

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
            logger.debug("get status: {}", line);

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

    protected abstract @Nullable List<JablotronHistoryDataEvent> sendGetEventHistory();

    protected synchronized @Nullable List<JablotronHistoryDataEvent> sendGetEventHistory(String alarm) {

        String url = JABLOTRON_API_URL + alarm +"/eventHistoryGet.json";
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
            logger.debug("get event history: {}", line);
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

    private void updateLastEvent(JablotronHistoryDataEvent event) {
        updateState(CHANNEL_LAST_EVENT_TIME, new DateTimeType(getZonedDateTime(event.getDate())));
        updateState(CHANNEL_LAST_EVENT, new StringType(event.getEventText() + " " + event.getInvokerName()));
        updateState(CHANNEL_LAST_EVENT_CLASS, new StringType(event.getIconType()));
    }

    public ZonedDateTime getZonedDateTime(String date) {
        return ZonedDateTime.parse(date.substring(0,22) + ":" + date.substring(22,24), DateTimeFormatter.ISO_DATE_TIME);
    }

    protected abstract void updateSegmentStatus(JablotronServiceDetailSegment segment);
}
