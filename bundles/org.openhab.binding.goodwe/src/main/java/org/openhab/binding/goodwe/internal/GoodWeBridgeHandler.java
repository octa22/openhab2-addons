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
package org.openhab.binding.goodwe.internal;

import static org.openhab.binding.goodwe.internal.GoodWeBindingConstants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.net.http.HttpClientFactory;
import org.openhab.binding.goodwe.internal.config.GoodWeBridgeConfiguration;
import org.openhab.binding.goodwe.internal.discovery.GoodWeDiscoveryService;
import org.openhab.binding.goodwe.internal.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link GoodWeBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels of the bridge thing.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class GoodWeBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(GoodWeBridgeHandler.class);

    private @Nullable GoodWeBridgeConfiguration config;

    /**
     * The shared HttpClient
     */
    private final HttpClient httpClient;

    /**
     * The Gson & parser
     */
    private final Gson gson = new Gson();

    /**
     * Future for regular ping
     */
    private @Nullable ScheduledFuture<?> pollFuture;

    /**
     * The login details
     */
    private String token = "";
    private String uid = "";
    private long timestamp = -1;

    public GoodWeBridgeHandler(Bridge thing, HttpClientFactory httpClientFactory) {
        super(thing);
        this.httpClient = httpClientFactory.createHttpClient("goodwe_" + thing.getUID().getId());
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(GoodWeDiscoveryService.class);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        logger.debug("Start initializing!");
        config = getConfigAs(GoodWeBridgeConfiguration.class);

        try {
            httpClient.start();
        } catch (Exception e) {
            logger.debug("Cannot start http client", e);
            return;
        }

        updateStatus(ThingStatus.UNKNOWN);
        scheduler.execute(() -> {
            login();
            initPolling();
            logger.debug("Finished initializing!");
        });
    }

    @Override
    public void dispose() {
        cleanup();
    }

    private void initPolling() {
        stopPolling();
        pollFuture = scheduler.scheduleWithFixedDelay(() -> {
            getStats();
        }, 10, 30, TimeUnit.SECONDS);
    }

    private void cleanup() {
        logger.debug("Doing cleanup");
        stopPolling();
        try {
            httpClient.stop();
        } catch (Exception e) {
            logger.debug("Error during http client stopping", e);
        }
    }

    /**
     * Stops this thing's polling future
     */
    private void stopPolling() {
        ScheduledFuture<?> localPollFuture = pollFuture;
        if (localPollFuture != null && !localPollFuture.isCancelled()) {
            localPollFuture.cancel(true);
        }
    }

    private void login() {
        Request request = sendRequestBuilder(SEMS_PORTAL_API_URL + "v1/Common/CrossLogin", HttpMethod.POST);
        request.content(
                new StringContentProvider(
                        "{\"account\":\"" + config.account + "\", \"pwd\":\"" + config.password + "\"}"),
                "application/json");
        request.header("token", "{\"version\":\"v2.0.4\",\"client\":\"ios\",\"language\":\"en\"}");

        try {
            ContentResponse content = request.send();
            if (logger.isTraceEnabled()) {
                logger.trace("Login response: {}", content.getContentAsString());
            }
            GoodWeLoginResponse response = gson.fromJson(content.getContentAsString(), GoodWeLoginResponse.class);
            if (0 == response.getCode()) {
                this.token = response.getData().getToken();
                this.uid = response.getData().getUid();
                this.timestamp = response.getData().getTimestamp();
                updateStatus(ThingStatus.ONLINE);
                getPowerPlants();
            } else {
                logError(response);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, response.getMsg());
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            logger.debug("Cannot login to SEMS Portal!", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Cannot login to the SEMS Portal!");
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public List<GoodWePowerstationInfo> getPowerPlants() {
        GoodWeQPSMResponse response = invokePostRequest(
                SEMS_PORTAL_API_URL + "PowerStationMonitor/QueryPowerStationMonitor", "{}", GoodWeQPSMResponse.class);
        if (response != null && 0 == response.getCode()) {
            logger.debug("Found total of {} power stations", response.getData().getRecord());
            if (response.getData().getRecord() > 0) {
                return response.getData().getList();
            }
        }
        return new ArrayList<>();
    }

    private @Nullable <T> T invokePostRequest(String url, String contentString, Class<T> classOfT) {
        Request request = sendRequestBuilderWithToken(url, HttpMethod.POST);
        request.content(new StringContentProvider(contentString), APPLICATION_JSON);
        try {
            ContentResponse content = request.send();
            if (logger.isTraceEnabled()) {
                logger.trace("Response: {}", content.getContentAsString());
            }
            return gson.fromJson(content.getContentAsString(), classOfT);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            logger.debug("Error calling url: {}!", url, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Error calling url: " + url + "!");
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

    public void getStats() {
        GoodWeGenericResponse response = invokePostRequest(
                SEMS_PORTAL_API_URL + "PowerStationMonitor/StatPowerStationKPI", "{}", GoodWeGenericResponse.class);
        if (response != null) {
            if (0 == response.getCode()) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                logError(response);
                if (AUTHORIZATION_EXPIRED.equals(response.getMsg())) {
                    login();
                }
            }
        }
    }

    private void logError(GoodWeGenericResponse response) {
        logger.debug("hasError: {}", response.getHasError());
        logger.debug("msg: {}", response.getMsg());
        logger.debug("code: {}", response.getCode());
    }

    public @Nullable GoodWePSMDetailResponse getPowerStationDetail(String id) {
        return invokePostRequest(SEMS_PORTAL_API_URL + "v1/PowerStation/GetMonitorDetailByPowerstationId",
                "{\"PowerStationId\":\"" + id + "\"}", GoodWePSMDetailResponse.class);
    }

    private Request sendRequestBuilder(String url, HttpMethod method) {
        return httpClient.newRequest(url).method(method).timeout(GOODWE_TIMEOUT, TimeUnit.SECONDS).agent(GOODWE_AGENT);
    }

    private Request sendRequestBuilderWithToken(String url, HttpMethod method) {
        return sendRequestBuilder(url, method).header("token",
                "{\"version\":\"v2.0.4\",\"client\":\"ios\",\"language\":\"en\",\"timestamp\":" + timestamp
                        + ",\"uid\":\"" + uid + "\",\"token\":\"" + token + "\"}");
    }
}
