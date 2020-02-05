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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.jablotron.internal.config.JablotronConfig;
import org.openhab.binding.jablotron.internal.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link JablotronBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class JablotronBridgeHandler extends BaseThingHandler implements BridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(JablotronBridgeHandler.class);

    private Gson gson = new Gson();

    final HttpClient httpClient;

    /**
     * Our configuration
     */
    public @Nullable JablotronConfig bridgeConfig;

    public JablotronBridgeHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
    }

    @Override
    public void childHandlerInitialized(ThingHandler thingHandler, Thing thing) {
    }

    @Override
    public void childHandlerDisposed(ThingHandler thingHandler, Thing thing) {
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

    }

    @Override
    public void initialize() {
        bridgeConfig = getConfigAs(JablotronConfig.class);
        scheduler.execute(this::login);
    }

    @Override
    public void dispose() {
        super.dispose();
        logout();
    }

    protected synchronized void login() {
        try {
            String url = JABLOTRON_API_URL + "userAuthorize.json";
            String urlParameters = "{\"login\":\"" + bridgeConfig.getLogin() + "\", \"password\":\"" + bridgeConfig.getPassword() + "\"}";

            ContentResponse resp = createRequest(url)
                    .header(HttpHeader.ACCEPT, APPLICATION_JSON)
                    .content(new StringContentProvider(urlParameters), APPLICATION_JSON)
                    .send();

            String line = resp.getContentAsString();
            logger.trace("login response: {}", line);
            JablotronLoginResponseAPI response = gson.fromJson(line, JablotronLoginResponseAPI.class);
            if (response.getHttpCode() != 200) {
                logger.debug("Error during login, got http error: {}", response.getHttpCode());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Http error: " + String.valueOf(response.getHttpCode()));
            } else {
                logger.debug("Successfully logged in");
                updateStatus(ThingStatus.ONLINE);
            }
        } catch (TimeoutException e) {
            logger.debug("Timeout during getting login cookie", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to Jablonet cloud");
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Cannot get Jablotron login cookie", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to Jablonet cloud");
        }
    }

    protected synchronized void logout() {
        String url = JABLOTRON_API_URL + "logout.json";
        String urlParameters = "system=" + SYSTEM;

        try {
            ContentResponse resp = createRequest(url)
                    .content(new StringContentProvider(urlParameters), "application/x-www-form-urlencoded; charset=UTF-8")
                    .send();
            String line = resp.getContentAsString();

            logger.trace("logout response: {}", line);
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            //Silence
        } finally {
            //controlDisabled = true;
        }
    }

    public synchronized @Nullable List<JablotronDiscoveredService> discoverServices() {
        try {
            String url = JABLOTRON_API_URL + "serviceListGet.json";
            String urlParameters = "{\"list-type\": \"EXTENDED\",\"visibility\": \"VISIBLE\"}";

            ContentResponse resp = createRequest(url)
                    .header(HttpHeader.ACCEPT, APPLICATION_JSON)
                    .content(new StringContentProvider(urlParameters), APPLICATION_JSON)
                    .send();

            String line = resp.getContentAsString();

            logger.trace("Response: {}", line);
            JablotronGetServiceResponse response = gson.fromJson(line, JablotronGetServiceResponse.class);

            if (response.getHttpCode() != 200) {
                logger.debug("Error during service discovery, got http code: {}", response.getHttpCode());
            }

            return response.getData().getServices();
        } catch (TimeoutException ex) {
            logger.error("Timeout during discovering services", ex);
        } catch (ExecutionException | InterruptedException ex) {
            logger.error("Interruption during discovering services", ex);
        }
        return null;
    }

    protected synchronized @Nullable JablotronControlResponse sendUserCode(Thing th, String section, String key, String status, String code) {
        String url;

        try {
            url = JABLOTRON_API_URL + "controlSegment.json";
            String urlParameters = "service=" + th.getThingTypeUID().getId() + "&serviceId=" + th.getUID().getId() + "&segmentId=" + section + "&segmentKey=" + key + "&expected_status=" + status + "&control_time=0&control_code=" + code + "&system=" + SYSTEM;
            logger.debug("Sending POST to url address: {} to control section: {}", url, section);
            logger.trace("Url parameters: {}", urlParameters);

            ContentResponse resp = createRequest(url)
                    .content(new StringContentProvider(urlParameters), "application/x-www-form-urlencoded; charset=UTF-8")
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

    protected synchronized @Nullable List<JablotronHistoryDataEvent> sendGetEventHistory(Thing th, String alarm) {
        String url = JABLOTRON_API_URL + alarm + "/eventHistoryGet.json";
        String urlParameters = "{\"limit\":1, \"service-id\":" + th.getUID().getId() + "}";

        try {
            ContentResponse resp = createRequest(url)
                    .header(HttpHeader.ACCEPT, APPLICATION_JSON)
                    .content(new StringContentProvider(urlParameters), APPLICATION_JSON)
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

    protected synchronized @Nullable JablotronDataUpdateResponse sendGetStatusRequest(Thing th) {
        String url = JABLOTRON_API_URL + "dataUpdate.json";
        String urlParameters = "data=[{ \"filter_data\":[{\"data_type\":\"section\"},{\"data_type\":\"pgm\"},{\"data_type\":\"teplomery\"},{\"data_type\":\"elektromery\"}],\"service_type\":\"" + th.getThingTypeUID().getId() + "\",\"service_id\":" + th.getUID().getId() + ",\"data_group\":\"serviceData\"}]&system=" + SYSTEM;

        try {
            ContentResponse resp = createRequest(url)
                    .content(new StringContentProvider(urlParameters), "application/x-www-form-urlencoded; charset=UTF-8")
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

    private Request createRequest(String url) {
        return httpClient.newRequest(url)
                .method(HttpMethod.POST)
                .header(HttpHeader.ACCEPT_LANGUAGE, "cs")
                .header(HttpHeader.ACCEPT_ENCODING, "*")
                .header("x-vendor-id", VENDOR)
                .agent(AGENT)
                .timeout(TIMEOUT, TimeUnit.SECONDS);
    }
}
