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
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.jablotron.internal.config.JablotronConfig;
import org.openhab.binding.jablotron.internal.Utils;
import org.openhab.binding.jablotron.internal.model.JablotronLoginResponse;
import org.openhab.binding.jablotron.internal.model.JablotronWidgetsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
public class JablotronBridgeHandler extends BaseThingHandler implements BridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(JablotronBridgeHandler.class);

    private Gson gson = new Gson();

    final HttpClient httpClient;

    /**
     * Our configuration
     */
    public JablotronConfig bridgeConfig;

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

    public JablotronConfig getBridgeConfig() {
        return bridgeConfig;
    }

    private void login() {
        String url = JABLOTRON_URL + "ajax/login.php";

        try {
            String urlParameters = "login=" + URLEncoder.encode(bridgeConfig.getLogin(), "UTF-8") + "&heslo=" + URLEncoder.encode(bridgeConfig.getPassword(), "UTF-8") + "&aStatus=200&loginType=Login";

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
            if (!response.isOKStatus()) {
                logger.error("Invalid response: {}", line);
                return;
            }
            logger.debug("Successfully logged to Jablotron cloud!");
            updateStatus(ThingStatus.ONLINE);
        } catch (TimeoutException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Timeout during login to Jablonet cloud");
            scheduler.schedule(this::login, 30, TimeUnit.SECONDS);
        } catch (UnsupportedEncodingException | ExecutionException | InterruptedException ex) {
            logger.error("Exception during login to Jablotron cloud: {}", ex);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to Jablonet cloud");
        }
    }

    private void logout() {

        String url = JABLOTRON_URL + "logout";
        try {
            ContentResponse resp = httpClient.newRequest(url)
                    .method(HttpMethod.GET)
                    .header(HttpHeader.ACCEPT_LANGUAGE, "cs-CZ")
                    .header(HttpHeader.ACCEPT_ENCODING, "gzip, deflate")
                    .header(HttpHeader.REFERER, JABLOTRON_URL)
                    .agent(AGENT)
                    .timeout(5, TimeUnit.SECONDS)
                    .send();

            String line = resp.getContentAsString();

            logger.debug("logout... {}", line);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            //Silence
            //logger.error(e.toString());
        }
    }

    public synchronized JablotronWidgetsResponse discoverServices() {
        try {
            String url = JABLOTRON_URL + "ajax/widget-new.php?" + Utils.getBrowserTimestamp();

            ContentResponse resp = httpClient.newRequest(url)
                    .method(HttpMethod.GET)
                    .header(HttpHeader.ACCEPT_LANGUAGE, "cs-CZ")
                    .header(HttpHeader.ACCEPT_ENCODING, "gzip, deflate")
                    .header(HttpHeader.REFERER, JABLOTRON_URL + "cloud")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .agent(AGENT)
                    .timeout(TIMEOUT, TimeUnit.SECONDS)
                    .send();

            String line = resp.getContentAsString();

            logger.debug("Response: {}", line);
            JablotronWidgetsResponse response = gson.fromJson(line, JablotronWidgetsResponse.class);

            return response;
        } catch (TimeoutException ex) {
            logger.error("Timeout during discovering services", ex);
        } catch (ExecutionException | InterruptedException ex) {
            logger.error("Interruption during discovering services", ex);
        }
        return null;
    }
}
