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
package org.openhab.binding.jablotron.internal.discovery;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryServiceCallback;
import org.eclipse.smarthome.config.discovery.ExtendedDiscoveryService;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.jablotron.internal.handler.JablotronBridgeHandler;
import org.openhab.binding.jablotron.internal.model.JablotronWidgetsResponse;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.jablotron.JablotronBindingConstants.*;

/**
 * The {@link JablotronDiscoveryService} is responsible for the thing discovery
 * process.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class JablotronDiscoveryService extends AbstractDiscoveryService implements ExtendedDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(JablotronDiscoveryService.class);
    private JablotronBridgeHandler bridge;
    private @Nullable DiscoveryServiceCallback discoveryServiceCallback;

    private @Nullable ScheduledFuture<?> discoveryJob = null;

    private static final int DISCOVERY_TIMEOUT_SEC = 10;

    public JablotronDiscoveryService(JablotronBridgeHandler bridgeHandler) {
        super(DISCOVERY_TIMEOUT_SEC);
        logger.debug("Creating discovery service");
        this.bridge = bridgeHandler;
    }

    private void startDiscovery() {
        if (this.bridge.getThing().getStatus() == ThingStatus.ONLINE) {
                discoverServices();
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        super.stopBackgroundDiscovery();
        if (discoveryJob != null) {
            discoveryJob.cancel(true);
        }
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Starting SomfyTahoma background discovery");

        if (discoveryJob == null || discoveryJob.isCancelled()) {
            discoveryJob = scheduler.scheduleWithFixedDelay(this::startDiscovery, 10, 3600,
                    TimeUnit.SECONDS);
        }

    }

    /**
     * Called on component activation.
     */
    @Override
    @Activate
    public void activate(@Nullable Map<String, @Nullable Object> configProperties) {
        super.activate(configProperties);
    }

    @Deactivate
    @Override
    protected void deactivate() {
        super.deactivate();
        if (discoveryJob != null) {
            discoveryJob.cancel(true);
        }
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return new HashSet<>(Arrays.asList(THING_TYPE_OASIS, THING_TYPE_JA100));
    }

    @Override
    public void setDiscoveryServiceCallback(DiscoveryServiceCallback discoveryServiceCallback) {
        this.discoveryServiceCallback = discoveryServiceCallback;
    }

    @Override
    protected void startScan() {
        logger.debug("Starting scanning for items...");
        //bridge.setDiscoveryService(this);
        startDiscovery();
    }

    public void oasisDiscovered(String label, String serviceId) {
        Map<String, Object> properties = new HashMap<>(1);

        ThingUID thingUID = new ThingUID(THING_TYPE_OASIS, bridge.getThing().getUID(), serviceId);

        if (discoveryServiceCallback.getExistingThing(thingUID) == null) {
            logger.info("Detected an OASIS alarm with service id: {}", serviceId);
            thingDiscovered(
                    DiscoveryResultBuilder.create(thingUID).withThingType(THING_TYPE_OASIS).withProperties(properties)
                            .withLabel(label)
                            .withBridge(bridge.getThing().getUID()).build());
        }
    }

    public void ja100Discovered(String label, String serviceId) {
        Map<String, Object> properties = new HashMap<>(1);

        ThingUID thingUID = new ThingUID(THING_TYPE_JA100, bridge.getThing().getUID(), serviceId);

        if (discoveryServiceCallback.getExistingThing(thingUID) == null) {
            logger.info("Detected a JA100 alarm with service id: {}", serviceId);
            thingDiscovered(
                    DiscoveryResultBuilder.create(thingUID).withThingType(THING_TYPE_JA100).withProperties(properties)
                            .withLabel(label)
                            .withBridge(bridge.getThing().getUID()).build());
        }
    }

    private synchronized void discoverServices() {
        try {
            JablotronWidgetsResponse response = bridge.discoverServices();

            if (response == null || !response.isOKStatus()) {
                logger.error("Invalid widgets response");
                return;
            }

            if (response.getCntWidgets() == 0) {
                logger.error("Cannot found any Jablotron device");
                return;
            }

            for (int i = 0; i < response.getCntWidgets(); i++) {
                String serviceId = String.valueOf(response.getWidgets().get(i).getId());
                String url = response.getWidgets().get(i).getUrl();
                logger.debug("Found Jablotron service: {} id: {}", response.getWidgets().get(i).getName(), serviceId);

                String device = response.getWidgets().get(i).getTemplateService();
                if (device.equals(THING_TYPE_OASIS.getId())) {
                    oasisDiscovered("Jablotron OASIS Alarm", serviceId);
                } else if (device.equals(THING_TYPE_JA100.getId())) {
                    ja100Discovered("Jablotron JA100 Alarm", serviceId);
                } else {
                    logger.error("Unsupported device type discovered: {} with serviceId: {} and url: {}", response.getWidgets().get(i).getTemplateService(), serviceId, url);
                }
            }
        } catch (Exception ex) {
            logger.error("Cannot discover Jablotron services!", ex);
        }
    }
}
