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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.efergyengage.internal.config.EfergyEngageSensorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EfergyEngageSensorHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class EfergyEngageSensorHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(EfergyEngageSensorHandler.class);

    public EfergyEngageSensorHandler(Thing thing) {
        super(thing);
    }

    /**
     * Thing configuration
     */
    private @Nullable EfergyEngageSensorConfig thingConfig;

    /**
     * Future to poll for updates
     */
    private @Nullable ScheduledFuture<?> pollFuture;

    @Override
    public void initialize() {
        updateStatus(ThingStatus.ONLINE);
        thingConfig = getConfigAs(EfergyEngageSensorConfig.class);
        initPolling(thingConfig.getRefresh());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType && getBridge() != null) {
            EfergyEngageHubHandler handler = (EfergyEngageHubHandler) getBridge().getHandler();
            if (handler != null) {
                handler.updateSensorChannel(channelUID, thing.getUID().getId());
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        stopPolling();
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
        logger.debug("updating sensor channels...");
        if (getBridge() == null) {
            return;
        }

        EfergyEngageHubHandler handler = (EfergyEngageHubHandler) getBridge().getHandler();
        if (ThingStatus.OFFLINE == getBridge().getStatus()) {
            // try to get the hub online
            handler.login();
        }

        for (Channel channel : getThing().getChannels()) {
            handler.updateSensorChannel(channel.getUID(), thing.getUID().getId());
        }

        if (ThingStatus.ONLINE != getThing().getStatus()) {
            updateStatus(ThingStatus.ONLINE);
        }
    }
}
