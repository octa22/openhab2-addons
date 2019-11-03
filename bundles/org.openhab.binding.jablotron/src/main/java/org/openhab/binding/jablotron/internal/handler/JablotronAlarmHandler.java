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

import com.google.gson.Gson;
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
import org.openhab.binding.jablotron.internal.model.JablotronLoginResponse;
import org.openhab.binding.jablotron.internal.model.JablotronTrouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
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
public abstract class JablotronAlarmHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(JablotronAlarmHandler.class);

    protected Gson gson = new Gson();

    protected DeviceConfig thingConfig;

    protected boolean inService = true;
    protected int lastHours = Utils.getHoursOfDay();

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
        logout();
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
    }

    protected abstract boolean updateAlarmStatus();

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
    }

    protected State getCheckTime() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Calendar.getInstance().toInstant(), ZoneId.systemDefault());
        return new DateTimeType(zdt);
    }

    protected void handleHttpRequestStatus(int status) {
        switch (status) {
            case 0:
                logout();
                break;
            case 201:
                logout();
                break;
            case 300:
                logger.error("Redirect not supported");
                break;
            case 800:
                login();
                initializeService();
                break;
            case 200:
                scheduler.schedule((Runnable) this::updateAlarmStatus, 1, TimeUnit.SECONDS);
                //scheduler.schedule((Runnable) this::updateAlarmStatus, 15, TimeUnit.SECONDS);
                break;
            default:
                logger.error("Unknown status code received: {}", status);
        }
    }

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
    }

    protected void doInit() {
        login();
        initializeService();

        future = scheduler.scheduleWithFixedDelay(() -> {
            updateAlarmStatus();
        }, 1, thingConfig.getRefresh(), TimeUnit.SECONDS);
    }

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
    }
}
