/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.binding.boschxmpp.internal.discovery;

import static org.openhab.binding.boschxmpp.internal.BoschXmppBindingConstants.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.boschxmpp.internal.EasyControlBridgeHandler;
import org.openhab.binding.boschxmpp.internal.discovery.EasyControlDiscoveryParser.Child;
import org.openhab.binding.boschxmpp.internal.protocol.BoschXmppException;
import org.openhab.core.config.discovery.AbstractThingHandlerDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/** Discovers zones, devices and DHW circuits below one EasyControl bridge. */
@Component(scope = ServiceScope.PROTOTYPE, service = EasyControlDiscoveryService.class)
@NonNullByDefault
public class EasyControlDiscoveryService extends AbstractThingHandlerDiscoveryService<EasyControlBridgeHandler> {

    @FunctionalInterface
    private interface DiscoveryParser {
        Iterable<Child> parse(JsonObject response);
    }

    private static final int DISCOVERY_TIMEOUT_SECONDS = 60;

    private final Logger logger = LoggerFactory.getLogger(EasyControlDiscoveryService.class);

    public EasyControlDiscoveryService() {
        super(EasyControlBridgeHandler.class, Set.of(THING_TYPE_ZONE, THING_TYPE_DEVICE, THING_TYPE_DHW),
                DISCOVERY_TIMEOUT_SECONDS, true);
    }

    @Override
    protected void startScan() {
        discoverEndpoint("/zones/list",
                response -> EasyControlDiscoveryParser.parseZones(response, THING_TYPE_ZONE, CONFIG_ZONE_ID));
        discoverEndpoint("/devices/list",
                response -> EasyControlDiscoveryParser.parseDevices(response, THING_TYPE_DEVICE, CONFIG_DEVICE_ID));
        discoverEndpoint("/dhwCircuits",
                response -> EasyControlDiscoveryParser.parseDhwCircuits(response, THING_TYPE_DHW, CONFIG_CIRCUIT_ID));
    }

    private void discoverEndpoint(String endpoint, DiscoveryParser parser) {
        try {
            discover(parser.parse(thingHandler.get(endpoint)));
        } catch (BoschXmppException | RuntimeException e) {
            logger.debug("EasyControl child discovery failed for {}: {}", endpoint, e.getMessage(), e);
        }
    }

    private void discover(Iterable<Child> children) {
        ThingUID bridgeUID = thingHandler.getThing().getUID();
        for (Child child : children) {
            DiscoveryResultBuilder builder = DiscoveryResultBuilder
                    .create(new ThingUID(child.type(), bridgeUID, child.id())).withLabel(child.label())
                    .withProperty(child.configurationProperty(), child.id()).withBridge(bridgeUID)
                    .withRepresentationProperty(child.configurationProperty());
            child.properties().forEach(builder::withProperty);
            thingDiscovered(builder.build());
        }
    }
}
