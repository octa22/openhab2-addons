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
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.jablotron.internal.config.JablotronConfig;
import org.openhab.binding.jablotron.internal.model.JablotronDiscoveredService;
import org.openhab.binding.jablotron.internal.model.JablotronGetServiceResponse;
import org.openhab.binding.jablotron.internal.model.JablotronLoginResponseAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.openhab.binding.jablotron.JablotronBindingConstants.*;

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

    public @Nullable JablotronConfig getBridgeConfig() {
        return bridgeConfig;
    }

    protected synchronized void login() {
        try {
            String url = JABLOTRON_API_URL + "login.json";
            String urlParameters = "login=" + URLEncoder.encode(bridgeConfig.getLogin(), "UTF-8") + "&password=" + URLEncoder.encode(bridgeConfig.getPassword(), "UTF-8") + "&version=3.2.3&selected_lang=cs&selected_country=cz&client_id=" + CLIENT;

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
            logger.trace("login response: {}", line);
            JablotronLoginResponseAPI response = gson.fromJson(line, JablotronLoginResponseAPI.class);
            if (!response.isStatus()) {
                logger.debug("Error during login: {}", response.getErrorMessage());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, response.getErrorMessage());
            } else {
                logger.debug("Successfully logged in");
                updateStatus(ThingStatus.ONLINE);
            }
        } catch (TimeoutException e) {
            logger.debug("Timeout during getting login cookie", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to Jablonet cloud");
        } catch (UnsupportedEncodingException | ExecutionException | InterruptedException e) {
            logger.error("Cannot get Jablotron login cookie", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to Jablonet cloud");
        }
    }

    protected synchronized void logout() {
        String url = JABLOTRON_API_URL + "logout.json";
        String urlParameters = "client_id=" + CLIENT;

        try {
            ContentResponse resp = httpClient.newRequest(url)
                    .method(HttpMethod.POST)
                    .header(HttpHeader.ACCEPT_LANGUAGE, "cs-CZ")
                    .header(HttpHeader.ACCEPT_ENCODING, "gzip, deflate")
                    .agent(AGENT)
                    .content(new StringContentProvider(urlParameters), "application/x-www-form-urlencoded; charset=UTF-8")
                    .timeout(5, TimeUnit.SECONDS)
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
            String url = JABLOTRON_API_URL + "getServiceList.json";
            String urlParameters = "visibility=default&list_type=extended&client_id=" + CLIENT;

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

            logger.trace("Response: {}", line);
            JablotronGetServiceResponse response = gson.fromJson(line, JablotronGetServiceResponse.class);

            if (!response.isStatus()) {
                logger.debug("Error during service discovery: {}", response.getErrorMessage());
            }

            return response.getServices();
        } catch (TimeoutException ex) {
            logger.error("Timeout during discovering services", ex);
        } catch (ExecutionException | InterruptedException ex) {
            logger.error("Interruption during discovering services", ex);
        }
        return null;
    }
}
