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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.goodwe.internal.config.GoodWePowerstationConfiguration;
import org.openhab.binding.goodwe.internal.model.GoodWePSMDetailResponse;
import org.openhab.binding.goodwe.internal.model.GoodWePowerstationInfo;
import org.openhab.binding.goodwe.internal.model.GoodWePowerstationKpi;
import org.openhab.binding.goodwe.internal.model.GoodWePowerstationStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link GoodWePowerstationHandler} is responsible for handling commands, which are
 * sent to one of the channels of the plant thing.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class GoodWePowerstationHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(GoodWePowerstationHandler.class);

    private @Nullable GoodWePowerstationConfiguration config;

    /**
     * Future to poll for updates
     */
    private @Nullable ScheduledFuture<?> pollFuture;

    public GoodWePowerstationHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            // TODO
        }
    }

    @Override
    public void initialize() {
        logger.debug("Start initializing!");
        config = getConfigAs(GoodWePowerstationConfiguration.class);

        updateStatus(ThingStatus.UNKNOWN);
        pollFuture = scheduler.scheduleWithFixedDelay(() -> {
            update();
        }, 0, config.refresh, TimeUnit.SECONDS);
        logger.debug("Finished initializing!");
    }

    private void update() {
        Bridge localBridge = getBridge();
        if (localBridge != null && ThingStatus.ONLINE == localBridge.getStatus()) {
            GoodWeBridgeHandler handler = (GoodWeBridgeHandler) localBridge.getHandler();
            if (handler != null) {
                GoodWePSMDetailResponse response = handler.getPowerStationDetail(config.id);
                if (response != null) {
                    updateData(response);
                }
            }
        } else {
            logger.info("The bridge is not initialized yet");
        }
    }

    private void updateData(GoodWePSMDetailResponse response) {
        GoodWePowerstationInfo info = response.getData().getInfo();

        if (info.getStatus() != -1) {
            // info
            updateState(CHANNEL_CAPACITY, new DecimalType(info.getCapacity()));
            updateState(CHANNEL_BATTERY_CAPACITY, new DecimalType(info.getBatteryCapacity()));

            // kpi
            GoodWePowerstationKpi kpi = response.getData().getKpi();
            updateState(CHANNEL_PAC, new DecimalType(kpi.getPac()));
            updateState(CHANNEL_POWER, new DecimalType(kpi.getPower()));
            updateState(CHANNEL_TOTAL_POWER, new DecimalType(kpi.getTotalPower()));
            updateState(CHANNEL_DAY_INCOME, new DecimalType(kpi.getDayIncome()));
            updateState(CHANNEL_TOTAL_INCOME, new DecimalType(kpi.getTotalIncome()));

            // stats
            GoodWePowerstationStats stats = response.getData().getStats();
            updateState(CHANNEL_SUM, new DecimalType(stats.getSum()));
            updateState(CHANNEL_BUY, new DecimalType(stats.getBuy()));
            updateState(CHANNEL_BUY_PERCENT, new DecimalType(stats.getBuyPercent()));
            updateState(CHANNEL_SELL, new DecimalType(stats.getSell()));
            updateState(CHANNEL_SELL_PERCENT, new DecimalType(stats.getSellPercent()));
            updateState(CHANNEL_SELF_USE_OF_PV, new DecimalType(stats.getSelfUseOfPv()));
            updateState(CHANNEL_CONSUMPTION_OF_LOAD, new DecimalType(stats.getConsumptionOfLoad()));

            updateStatus(ThingStatus.ONLINE);
        } else {
            logger.debug("Powerstation {} is probably offline!", config.id);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "Status: " + info.getStatus());
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            update();
        }
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> localFuture = pollFuture;
        if (localFuture != null && !localFuture.isCancelled()) {
            localFuture.cancel(true);
        }
    }
}
