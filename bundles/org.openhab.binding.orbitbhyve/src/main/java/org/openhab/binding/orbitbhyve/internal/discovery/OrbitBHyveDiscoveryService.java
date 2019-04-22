/**
 * Copyright (c) 2014,2019 by the respective copyright holders.
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
package org.openhab.binding.orbitbhyve.internal.discovery;

import static org.openhab.binding.orbitbhyve.internal.OrbitBHyveBindingConstants.THING_TYPE_SPRINKLER;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.*;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.openhab.binding.orbitbhyve.internal.handler.OrbitBHyveBridgeHandler;
import org.openhab.binding.orbitbhyve.internal.model.OrbitBHyveDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OrbitBHyveDiscoveryService} discovers sprinklers
 * associated with your Orbit B-Hyve cloud account.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class OrbitBHyveDiscoveryService extends AbstractDiscoveryService
        implements DiscoveryService, ThingHandlerService {

    private final Logger logger = LoggerFactory.getLogger(OrbitBHyveDiscoveryService.class);

    private @Nullable OrbitBHyveBridgeHandler bridgeHandler;

    private @Nullable ScheduledFuture<?> discoveryJob;

    private static final int DISCOVERY_TIMEOUT_SEC = 10;
    private static final int DISCOVERY_REFRESH_SEC = 1800;

    public OrbitBHyveDiscoveryService() {
        super(DISCOVERY_TIMEOUT_SEC);
        logger.debug("Creating discovery service");
    }

    @Override
    protected void startScan() {
        runDiscovery();
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
        if (thingHandler instanceof OrbitBHyveBridgeHandler) {
            bridgeHandler = (OrbitBHyveBridgeHandler) thingHandler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Starting Orbit B-Hyve background discovery");

        ScheduledFuture<?> localDiscoveryJob = discoveryJob;
        if (localDiscoveryJob == null || localDiscoveryJob.isCancelled()) {
            discoveryJob = scheduler.scheduleWithFixedDelay(this::runDiscovery, 10, DISCOVERY_REFRESH_SEC,
                    TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stopping Orbit B-Hyve background discovery");
        ScheduledFuture<?> localDiscoveryJob = discoveryJob;
        if (localDiscoveryJob != null && !localDiscoveryJob.isCancelled()) {
            localDiscoveryJob.cancel(true);
        }
    }

    private synchronized void runDiscovery() {
        OrbitBHyveBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler != null && ThingStatus.ONLINE == localBridgeHandler.getThing().getStatus()) {
            List<OrbitBHyveDevice> devices = localBridgeHandler.getDevices();
            logger.debug("Discovered total of {} devices", devices.size());
            for (OrbitBHyveDevice device : devices) {
                sprinklerDiscovered(device);
            }
        }
    }

    private void sprinklerDiscovered(OrbitBHyveDevice device) {
        OrbitBHyveBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler != null) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(Thing.PROPERTY_FIRMWARE_VERSION, device.getFwVersion());
            properties.put(Thing.PROPERTY_HARDWARE_VERSION, device.getHwVersion());
            properties.put(Thing.PROPERTY_MAC_ADDRESS, device.getMacAddress());
            properties.put(Thing.PROPERTY_MODEL_ID, device.getType());
            properties.put("Zones", device.getNumStations());
            properties.put("Active zones", device.getZones().size());

            ThingUID thingUID = new ThingUID(THING_TYPE_SPRINKLER, localBridgeHandler.getThing().getUID(),
                    device.getId());

            logger.debug("Detected a/an {} - label: {} id: {}", THING_TYPE_SPRINKLER.getId(), device.getName(),
                    device.getId());
            thingDiscovered(DiscoveryResultBuilder.create(thingUID).withThingType(THING_TYPE_SPRINKLER)
                    .withProperties(properties).withLabel(device.getName())
                    .withBridge(localBridgeHandler.getThing().getUID()).build());
        }
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return Collections.singleton(THING_TYPE_SPRINKLER);
    }
}
