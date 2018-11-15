/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.mintos.internal.discovery;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryServiceCallback;
import org.eclipse.smarthome.config.discovery.ExtendedDiscoveryService;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.mintos.internal.handler.MintosBridgeHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.mintos.internal.MintosBindingConstants.THING_TYPE_ACCOUNT;

@NonNullByDefault
public class MintosAccountDiscoveryService extends AbstractDiscoveryService implements ExtendedDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(MintosAccountDiscoveryService.class);

    private MintosBridgeHandler bridge;

    @Nullable
    private DiscoveryServiceCallback discoveryServiceCallback;
    @Nullable
    private ScheduledFuture<?> discoveryJob;


    private static final int DISCOVERY_TIMEOUT_SEC = 10;
    private static final int DISCOVERY_REFRESH_SEC = 1800;

    public MintosAccountDiscoveryService(MintosBridgeHandler bridgeHandler) {
        super(DISCOVERY_TIMEOUT_SEC);
        logger.debug("Creating discovery service");
        this.bridge = bridgeHandler;
    }


    @Override
    protected void startScan() {
        runDiscovery();
    }

    /**
     * Called on component activation.
     */
    @Override
    @Activate
    public void activate(@Nullable Map<String, @Nullable Object> configProperties) {
        super.activate(configProperties);
    }

    @Override
    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public void setDiscoveryServiceCallback(DiscoveryServiceCallback discoveryServiceCallback) {
        this.discoveryServiceCallback = discoveryServiceCallback;
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Starting Mintos background discovery");

        if (discoveryJob == null || discoveryJob.isCancelled()) {
            discoveryJob = scheduler.scheduleWithFixedDelay(this::runDiscovery, 10, DISCOVERY_REFRESH_SEC,
                    TimeUnit.SECONDS);
        }

    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stopping Mintos background discovery");
        if (discoveryJob != null && !discoveryJob.isCancelled()) {
            discoveryJob.cancel(true);
            discoveryJob = null;
        }
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return new HashSet<>(Collections.singleton(THING_TYPE_ACCOUNT));
    }

    private void runDiscovery() {
        logger.debug("Starting scanning for things...");

        if (bridge.getThing().getStatus().equals(ThingStatus.ONLINE)) {
            List<String> accounts = bridge.getAccounts();
            for(String currency : accounts) {
                accountDiscovered(currency);
            }
        }
    }

    private void accountDiscovered(String currency) {
        ThingUID thingUID = new ThingUID(THING_TYPE_ACCOUNT, bridge.getThing().getUID(), currency);

        if (discoveryServiceCallback.getExistingThing(thingUID) == null) {
            logger.debug("Detected an account with currency: {}", currency);
            thingDiscovered(DiscoveryResultBuilder.create(thingUID).withThingType(THING_TYPE_ACCOUNT)
                    .withRepresentationProperty("currency").withLabel("Mintos " + currency + " account")
                    .withBridge(bridge.getThing().getUID()).build());
        }
    }

}
