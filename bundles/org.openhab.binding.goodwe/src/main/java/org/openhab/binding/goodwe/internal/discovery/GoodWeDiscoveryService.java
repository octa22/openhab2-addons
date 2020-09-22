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
package org.openhab.binding.goodwe.internal.discovery;

import static org.openhab.binding.goodwe.internal.GoodWeBindingConstants.SUPPORTED_THING_TYPES_UIDS;
import static org.openhab.binding.goodwe.internal.GoodWeBindingConstants.THING_TYPE_POWERSTATION;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.openhab.binding.goodwe.internal.GoodWeBridgeHandler;
import org.openhab.binding.goodwe.internal.model.GoodWePowerstationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link GoodWeDiscoveryService discovers power plants associated with your
 * SEMS Portal account.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class GoodWeDiscoveryService extends AbstractDiscoveryService implements DiscoveryService, ThingHandlerService {

    private final Logger logger = LoggerFactory.getLogger(GoodWeDiscoveryService.class);

    @Nullable
    private GoodWeBridgeHandler bridgeHandler;

    @Nullable
    private ScheduledFuture<?> discoveryJob;

    private static final int DISCOVERY_TIMEOUT_SEC = 10;
    private static final int DISCOVERY_REFRESH_SEC = 3600;

    public GoodWeDiscoveryService() {
        super(SUPPORTED_THING_TYPES_UIDS, DISCOVERY_TIMEOUT_SEC);
        logger.info("Creating discovery service");
    }

    @Override
    public void activate() {
        super.activate(null);
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler thingHandler) {
        if (thingHandler instanceof GoodWeBridgeHandler) {
            bridgeHandler = (GoodWeBridgeHandler) thingHandler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Starting GoodWe background discovery");

        ScheduledFuture<?> localDiscoveryJob = discoveryJob;
        if (localDiscoveryJob == null || localDiscoveryJob.isCancelled()) {
            discoveryJob = scheduler.scheduleWithFixedDelay(this::runDiscovery, 10, DISCOVERY_REFRESH_SEC,
                    TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stopping GoodWe background discovery");

        ScheduledFuture<?> localDiscoveryJob = discoveryJob;
        if (localDiscoveryJob != null && !localDiscoveryJob.isCancelled()) {
            localDiscoveryJob.cancel(true);
        }
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES_UIDS;
    }

    @Override
    protected void startScan() {
        runDiscovery();
    }

    private synchronized void runDiscovery() {
        logger.info("Starting scanning for plants...");

        GoodWeBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler != null && ThingStatus.ONLINE == localBridgeHandler.getThing().getStatus()) {
            List<GoodWePowerstationInfo> plants = localBridgeHandler.getPowerPlants();

            for (GoodWePowerstationInfo device : plants) {
                discoverDevice(device);
            }
        } else {
            logger.debug("Cannot start discovery since the bridge is not online!");
        }
    }

    private void discoverDevice(GoodWePowerstationInfo plant) {
        deviceDiscovered(plant.getStationName(), plant.getId(), THING_TYPE_POWERSTATION);
    }

    private void deviceDiscovered(String name, String id, ThingTypeUID thingTypeUID) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("id", id);

        GoodWeBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler != null) {
            ThingUID thingUID = new ThingUID(thingTypeUID, localBridgeHandler.getThing().getUID(), id);

            logger.info("Discovered power station: {} with id: {}", name, id);
            thingDiscovered(DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID)
                    .withProperties(properties).withRepresentationProperty("id").withLabel(name)
                    .withBridge(localBridgeHandler.getThing().getUID()).build());
        }
    }
}
