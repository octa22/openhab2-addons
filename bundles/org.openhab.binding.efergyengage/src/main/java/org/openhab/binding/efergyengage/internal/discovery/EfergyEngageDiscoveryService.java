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
package org.openhab.binding.efergyengage.internal.discovery;

import static org.openhab.binding.efergyengage.internal.EfergyEngageBindingConstants.EFERGY_ENGAGE_SENSOR;
import static org.openhab.binding.efergyengage.internal.EfergyEngageBindingConstants.PWER;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.*;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.openhab.binding.efergyengage.internal.handler.EfergyEngageHubHandler;
import org.openhab.binding.efergyengage.internal.model.EfergyEngageData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EfergyEngageDiscoveryService} discovers sensors
 * associated with your Efergy Engage cloud account.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class EfergyEngageDiscoveryService extends AbstractDiscoveryService
        implements DiscoveryService, ThingHandlerService {

    private final Logger logger = LoggerFactory.getLogger(EfergyEngageDiscoveryService.class);

    private EfergyEngageHubHandler bridgeHandler;

    private @Nullable DiscoveryServiceCallback discoveryServiceCallback;

    private @Nullable ScheduledFuture<?> discoveryJob;

    private static final int DISCOVERY_TIMEOUT_SEC = 10;
    private static final int DISCOVERY_REFRESH_SEC = 3600;

    public EfergyEngageDiscoveryService(EfergyEngageHubHandler bridgeHandler) {
        super(DISCOVERY_TIMEOUT_SEC);
        logger.debug("Creating discovery service");
        this.bridgeHandler = bridgeHandler;
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
    public void setThingHandler(@NonNullByDefault({}) ThingHandler handler) {
        if (handler instanceof EfergyEngageHubHandler) {
            bridgeHandler = (EfergyEngageHubHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Starting Efergy Engage background discovery");

        if (discoveryJob == null || discoveryJob.isCancelled()) {
            discoveryJob = scheduler.scheduleWithFixedDelay(this::runDiscovery, 10, DISCOVERY_REFRESH_SEC,
                    TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stopping Efergy Engage background discovery");
        if (discoveryJob != null && !discoveryJob.isCancelled()) {
            discoveryJob.cancel(true);
            discoveryJob = null;
        }
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return Collections.singleton(EFERGY_ENGAGE_SENSOR);
    }

    @Override
    protected void startScan() {
        runDiscovery();
    }

    private synchronized void runDiscovery() {
        logger.debug("Starting scanning for things...");

        if (ThingStatus.ONLINE == bridgeHandler.getThing().getStatus()) {
            List<EfergyEngageData> list = bridgeHandler.readInstant();
            for (EfergyEngageData data : list) {
                String sid = data.getSid();
                String cid = data.getCid();
                String units = data.getUnits();

                if (PWER.equals(cid)) {
                    sensorDiscovered(sid, units);
                }
            }
        } else {
            logger.debug("Cannot start discovery since the bridge is not online! Rescheduling...");
            scheduler.schedule(this::runDiscovery, 60, TimeUnit.SECONDS);
        }
    }

    private void sensorDiscovered(String sid, String units) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("units", units);
        properties.put("sid", sid);

        String label = "Efergy CT Sensor " + sid;

        ThingUID thingUID = new ThingUID(EFERGY_ENGAGE_SENSOR, bridgeHandler.getThing().getUID(), sid);

        logger.debug("Detected a CT sensor - label: {}", label);
        thingDiscovered(DiscoveryResultBuilder.create(thingUID).withThingType(EFERGY_ENGAGE_SENSOR)
                .withProperties(properties).withRepresentationProperty("sid").withLabel(label)
                .withBridge(bridgeHandler.getThing().getUID()).build());
    }
}
