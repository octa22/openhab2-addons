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
package org.openhab.binding.efergyengage.internal.handler;

import static org.openhab.binding.efergyengage.internal.EfergyEngageBindingConstants.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.smarthome.core.cache.ExpiringCacheMap;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.efergyengage.internal.config.EfergyEngageHubConfig;
import org.openhab.binding.efergyengage.internal.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.*;

/**
 * The {@link EfergyEngageHubHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class EfergyEngageHubHandler extends BaseBridgeHandler {

    private Logger logger = LoggerFactory.getLogger(EfergyEngageHubHandler.class);

    public EfergyEngageHubHandler(Bridge thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
    }

    private final HttpClient httpClient;

    private @Nullable String token = null;

    private Gson gson = new Gson();

    // caches
    private @Nullable ExpiringCacheMap<String, List<EfergyEngageData>> cache;
    private @Nullable ExpiringCacheMap<String, EfergyEngageEstimate> cacheEstimate;

    /**
     * Hub configuration
     */
    private @Nullable EfergyEngageHubConfig thingConfig;

    /**
     * Future to poll for updates
     */
    private @Nullable ScheduledFuture<?> pollFuture;

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (RefreshType.REFRESH.equals(command) && token != null) {
            updateHubChannel(channelUID);
        }
    }

    @Override
    public void initialize() {
        logger.debug("Efergy Engage initialization");
        thingConfig = getConfigAs(EfergyEngageHubConfig.class);
        cache = new ExpiringCacheMap<>(CACHE_EXPIRY);
        cacheEstimate = new ExpiringCacheMap<>(CACHE_EXPIRY);

        httpClient.setFollowRedirects(false);

        login();
        initPolling(thingConfig.getRefresh());
    }

    @Override
    public void dispose() {
        stopPolling();
        super.dispose();
    }

    public synchronized void login() {
        if (StringUtils.isNotBlank(thingConfig.getToken())) {
            token = thingConfig.getToken();
            EfergyEngageMac device = readStatus();
            if (device != null) {
                updateProperty("MAC", device.getMac());
                updateProperty("Type", device.getType());
                updateProperty("Version", device.getVersion());
                if ("on".equals(device.getStatus())) {
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Status: " + device.getStatus());
                }
            }
            return;
        }
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                "Please configure an application token!");
    }

    public List<EfergyEngageData> readInstant() {
        try {
            String url = EFERGY_URL + "getCurrentValuesSummary?token=" + token;

            ContentResponse response = sendRequest(url);
            String line = readResponse(response);

            if (line.startsWith("{")) {
                // error
                EfergyEngageGetCurrentValuesResponse res = gson.fromJson(line,
                        EfergyEngageGetCurrentValuesResponse.class);
                logger.debug("{} - {}", res.getError().getDesc(), res.getError().getMore());
            } else {
                // read value
                EfergyEngageData[] data = gson.fromJson(line, EfergyEngageData[].class);

                if (data.length == 0) {
                    logger.debug("Null data received: {}", line);
                    return new ArrayList<>();
                }
                return Arrays.asList(data);
            }
        } catch (JsonSyntaxException | InterruptedException | ExecutionException | TimeoutException e) {
            logger.debug("Cannot get Efergy Engage summary", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Got exception: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        return new ArrayList<>();
    }

    private ContentResponse sendRequest(String url) throws InterruptedException, ExecutionException, TimeoutException {
        return httpClient.newRequest(url).method(HttpMethod.GET).timeout(READ_TIMEOUT, TimeUnit.MILLISECONDS).send();
    }

    private EfergyEngageMeasurement readPowerMeasurement(List<EfergyEngageData> data, String sid) {
        EfergyEngageMeasurement measurement = new EfergyEngageMeasurement();
        for (EfergyEngageData pwer : data) {
            if (!PWER.equals(pwer.getCid()) || !sid.equals(pwer.getSid())) {
                logger.trace("skipping entry with cid: {} and sid: {}", pwer.getCid(), pwer.getSid());
                continue;
            }
            JsonArray dataArray = pwer.getData();
            if (dataArray.size() <= 0) {
                return measurement;
            }
            JsonObject obj = dataArray.get(0).getAsJsonObject();
            if (obj.entrySet().size() > 0) {
                Map.Entry<String, JsonElement> entry = obj.entrySet().iterator().next();
                measurement.setValue(entry.getValue().getAsFloat());
                measurement.setMilis(System.currentTimeMillis() - 1000 * pwer.getAge());
            }
        }
        return measurement;
    }

    /**
     * starts this things polling future
     */
    private void initPolling(int refresh) {
        stopPolling();
        pollFuture = scheduler.scheduleWithFixedDelay(this::execute, 10, refresh, TimeUnit.SECONDS);
    }

    /**
     * Stops this thing's polling future
     */
    private void stopPolling() {
        if (pollFuture != null && !pollFuture.isCancelled()) {
            pollFuture.cancel(true);
            pollFuture = null;
        }
    }

    /**
     * The polling future executes this every iteration
     */
    private void execute() {
        if (this.getThing().getStatus() != ThingStatus.ONLINE) {
            login();
        }

        if (this.getThing().getStatus() != ThingStatus.ONLINE) {
            logger.debug("The thing is still not online!");
            return;
        }

        for (Channel channel : getThing().getChannels()) {
            updateHubChannel(channel.getUID());
        }
    }

    public void updateHubChannel(ChannelUID uid) {
        EfergyEngageMeasurement value;
        EfergyEngageEstimate est;
        State state;
        switch (uid.getId()) {
            case CHANNEL_ESTIMATE:
                est = readForecastCached();
                if (est == null) {
                    logger.debug("A null forecast received!");
                    return;
                }
                state = new DecimalType(est.getEstimate());
                updateState(uid, state);
                break;
            case CHANNEL_COST:
                est = readForecastCached();
                if (est == null) {
                    logger.debug("A null forecast received!");
                    return;
                }
                state = new DecimalType(est.getPreviousSum());
                updateState(uid, state);
                break;
            case CHANNEL_DAYTOTAL:
                value = readEnergy(DAY);
                state = new QuantityType(value.getValue(), SmartHomeUnits.KILOWATT_HOUR);
                updateState(uid, state);
                break;
            case CHANNEL_WEEKTOTAL:
                value = readEnergy(WEEK);
                state = new QuantityType(value.getValue(), SmartHomeUnits.KILOWATT_HOUR);
                updateState(uid, state);
                break;
            case CHANNEL_MONTHTOTAL:
                value = readEnergy(MONTH);
                state = new QuantityType(value.getValue(), SmartHomeUnits.KILOWATT_HOUR);
                updateState(uid, state);
                break;
            case CHANNEL_YEARTOTAL:
                value = readEnergy(YEAR);
                state = new QuantityType(value.getValue(), SmartHomeUnits.KILOWATT_HOUR);
                updateState(uid, state);
                break;
            default:
                logger.debug("Unknown channel to update: {}", uid.getId());
        }
    }

    public void updateSensorChannel(ChannelUID channel, String sid) {
        EfergyEngageMeasurement value;
        State state;
        switch (channel.getId()) {
            case CHANNEL_INSTANT:
                value = readInstantCached(sid);
                state = new QuantityType(value.getValue(), SmartHomeUnits.WATT);
                updateState(channel, state);
                break;
            case CHANNEL_LAST_MEASUREMENT:
                value = readInstantCached(sid);
                if (value.getMilis() > 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(new java.util.Date(value.getMilis()));
                    updateState(channel, new DateTimeType(cal));
                }
                break;
            default:
                logger.debug("Unknown sensor channel to update: {}", channel.getId());
        }
    }

    private @Nullable EfergyEngageEstimate readForecastCached() {
        if (cacheEstimate.get(CHANNEL_ESTIMATE) == null) {
            cacheEstimate.put(CHANNEL_ESTIMATE, this::readForecast);
        }
        return cacheEstimate.get(CHANNEL_ESTIMATE);
    }

    private @Nullable EfergyEngageMeasurement readInstantCached(String sid) {
        if (cache.get(CHANNEL_INSTANT) == null) {
            cache.put(CHANNEL_INSTANT, this::readInstant);
        }
        List<EfergyEngageData> data = cache.get(CHANNEL_INSTANT);
        return readPowerMeasurement(data, sid);
    }

    private EfergyEngageMeasurement readEnergy(String period) {
        String url;
        EfergyEngageMeasurement measurement = new EfergyEngageMeasurement();

        try {
            url = EFERGY_URL + "getEnergy?token=" + token + "&period=" + period + "&offset="
                    + thingConfig.getUtcOffset();
            ;

            ContentResponse content = sendRequest(url);
            String line = readResponse(content);
            // read value
            EfergyEngageGetEnergyResponse response = gson.fromJson(line, EfergyEngageGetEnergyResponse.class);

            float energy = -1;
            String units = "";
            if (response.getError() == null) {
                energy = response.getSum();
                units = response.getUnits();
                logger.debug("Efergy reading for {} period: {} {}", period, energy, units);
            } else {
                logger.debug("{} - {}", response.getError().getDesc(), response.getError().getMore());
            }
            measurement.setValue(energy);
            measurement.setUnit(units);
        } catch (JsonSyntaxException | InterruptedException | TimeoutException | ExecutionException e) {
            logger.debug("Cannot get Efergy Engage data", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Got exception: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return measurement;
    }

    private String readResponse(ContentResponse content) {
        String response = content.getContentAsString();
        if (logger.isTraceEnabled()) {
            logger.trace("Received message: {}", response);
        }
        return response;
    }

    private @Nullable EfergyEngageEstimate readForecast() {
        String url;

        try {
            url = EFERGY_URL + "getForecast?token=" + token + "&dataType=cost&period=month&offset="
                    + thingConfig.getUtcOffset();

            ContentResponse content = sendRequest(url);
            String line = readResponse(content);

            // read value
            EfergyEngageGetForecastResponse response = gson.fromJson(line, EfergyEngageGetForecastResponse.class);
            if (response.getError() == null) {
                return response.getMonthTariff();
            } else {
                logger.debug("{} - {}", response.getError().getDesc(), response.getError().getMore());
                return null;
            }
        } catch (JsonSyntaxException | InterruptedException | TimeoutException | ExecutionException e) {
            logger.debug("Cannot get Efergy Engage forecast", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Got exception: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

    private @Nullable EfergyEngageMac readStatus() {
        String url;

        try {
            url = EFERGY_URL + "getStatus?token=" + token;

            ContentResponse content = sendRequest(url);
            String line = readResponse(content);

            // read value
            EfergyEngageGetStatusResponse response = gson.fromJson(line, EfergyEngageGetStatusResponse.class);
            if (response.getListOfMacs().size() > 0) {
                return response.getListOfMacs().get(0);
            }
            if ("error".equals(response.getStatus())) {
                String error = response.getStatus() + " : " + response.getDescription();
                logger.debug("Cannot get Efergy Engage status - {}", error);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, error);
            }
        } catch (JsonSyntaxException | InterruptedException | TimeoutException | ExecutionException e) {
            logger.debug("Cannot get Efergy Engage status", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Got exception: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }
}
