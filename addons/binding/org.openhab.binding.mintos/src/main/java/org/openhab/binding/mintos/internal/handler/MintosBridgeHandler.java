/**
 * Copyright (c) 2014,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.mintos.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
import org.eclipse.smarthome.core.cache.ExpiringCache;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.ConfigStatusBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.mintos.internal.MintosAccountOverview;
import org.openhab.binding.mintos.internal.config.MintosBridgeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.openhab.binding.mintos.internal.MintosBindingConstants.*;

/**
 * The {@link MintosBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class MintosBridgeHandler extends ConfigStatusBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(MintosBridgeHandler.class);

    @Nullable
    private MintosBridgeConfiguration config;

    private ExpiringCache<MintosAccountOverview> accountOverview;

    private SslContextFactory sslContext = new SslContextFactory();

    private HttpClient httpClient;

    private String csrf = "";
    private String logoutURL = "";

    public MintosBridgeHandler(Bridge thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public Collection<ConfigStatusMessage> getConfigStatus() {
        return Collections.emptyList();
    }

    @Override
    public void initialize() {
        config = getConfigAs(MintosBridgeConfiguration.class);

        httpClient = new HttpClient(sslContext);
        try {
            httpClient.start();
        } catch (Exception e) {
            logger.error("Cannot start http client!", e);
            return;
        }

        if (config != null && !config.getLogin().isEmpty() && !config.getPassword().isEmpty() ) {
            getOverview();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
        }
        logout();
    }

    private void logout() {
        try {
            if (!logoutURL.isEmpty()) {
                httpClient.newRequest(logoutURL).method(HttpMethod.GET)
                        .header("referer", "https://www.mintos.com/en/overview")
                        .header(":authority", "www.mintos.com")
                        .header(":method", "POST")
                        .header(":path", logoutURL.replace("https://www.mintos.com", ""))
                        .header(":scheme", "https")
                        .agent(AGENT)
                        .send();
            }
            if (httpClient.isRunning()) {
                httpClient.stop();
            }
            logoutURL = "";
        } catch (Exception e) {
            logger.error("Error during logout", e);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if(httpClient != null && httpClient.isRunning()) {
            try {
                httpClient.stop();
            } catch (Exception e) {
                //do nothing
            }
        }
    }

    public MintosBridgeConfiguration getBridgeConfiguration(){
        return config;
    }

    private synchronized String getOverview() {
        ContentResponse response;

        try {
            response = httpClient.newRequest("https://www.mintos.com/en")
                    .method(HttpMethod.GET)
                    .header(":authority", "www.mintos.com")
                    .header(":method", "GET")
                    .header(":path", "/en/")
                    .header(":scheme", "https")
                    .agent(AGENT)
                    .send();
            csrf = getCsrfToken(response.getContentAsString());
            logger.debug("Got CSRF token: {}", csrf);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Cannot send GET command to mintos.com!", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot open www.mintos.com");
            return null;
        }

        StringContentProvider content = new StringContentProvider("_csrf_token=" + csrf + "&_username=" + config.getLogin() + "&_password=" + config.getPassword());
        try {
            response = httpClient.newRequest("https://www.mintos.com/en/login/check")
                    .method(HttpMethod.POST)
                    .content(content, "application/x-www-form-urlencoded")
                    .header(":authority", "www.mintos.com")
                    .header(":method", "POST")
                    .header(":path", "/en/login/check")
                    .header(":scheme", "https")
                    .header("origin", "https://www.mintos.com")
                    .header("referer", "https://www.mintos.com/en/login")
                    .agent(AGENT)
                    .send();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Cannot login to mintos.com!", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to www.mintos.com");
            return null;
        }

        if (response.getStatus() == 200) {
            logger.debug("Got login response with length: {}", response.getContentAsString().length());
            updateStatus(ThingStatus.ONLINE);
            String txt = response.getContentAsString();
            logoutURL = getLogoutUrl(txt);
            return txt;
        } else {
            logger.error("Got response code: {} and message: {}", response.getStatus(), response.getContentAsString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Got response code: " + response.getStatus());
        }
        return null;
    }

    public List<String> getAccounts() {
        try {
            httpClient = new HttpClient(sslContext);
            httpClient.start();
        } catch (Exception e) {
            logger.error("Cannot start http client!", e);
            return new ArrayList<>();
        }
        String response = getOverview();
        logout();
        return parseAccounts(response);
    }

    private List<String> parseAccounts(String page) {
        final String SPAN = "<span title=\"";
        List<String> accounts = new ArrayList<>();
        int pos = page.indexOf(SPAN);
        while( pos >= 0 ) {
            page = page.substring(pos + SPAN.length());
            String currency = page.substring(0,3);
            logger.debug("Found currency: {}", currency);
            accounts.add(currency);
            pos = page.indexOf(SPAN);
        }
        return accounts;
    }

    private String getCsrfToken(String content) {
        final String field = "_csrf_token";
        int pos = content.indexOf(field);
        return content.substring(pos + field.length() + 9, pos + field.length() + 9 + 43);
    }

    private String getLogoutUrl(String text) {
        int pos = text.indexOf(LOGOUT_URL);
        int posEnd = text.indexOf("\" class=\"logout main-nav-logout");
        return text.substring(pos + 9, posEnd);
    }
}
