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
package org.openhab.binding.jablotron.internal;

import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.io.net.http.HttpClientFactory;
import org.openhab.binding.jablotron.internal.handler.JablotronBridgeHandler;
import org.openhab.binding.jablotron.internal.handler.JablotronJa100Handler;
import org.openhab.binding.jablotron.internal.handler.JablotronOasisHandler;
import org.openhab.binding.jablotron.internal.discovery.JablotronDiscoveryService;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.*;

import static org.openhab.binding.jablotron.JablotronBindingConstants.THING_TYPE_BRIDGE;
import static org.openhab.binding.jablotron.JablotronBindingConstants.THING_TYPE_JA100;
import static org.openhab.binding.jablotron.JablotronBindingConstants.THING_TYPE_OASIS;

/**
 * The {@link JablotronHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, immediate = true)
public class JablotronHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>(Arrays.asList(THING_TYPE_OASIS, THING_TYPE_JA100, THING_TYPE_BRIDGE));
    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();
    private final HttpClientFactory httpClientFactory;

    @Activate
    public JablotronHandlerFactory(@Reference HttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_BRIDGE)) {
            JablotronBridgeHandler handler = new JablotronBridgeHandler(thing, httpClientFactory.getCommonHttpClient());
            registerItemDiscoveryService(handler);
            return handler;
        }
        if (thingTypeUID.equals(THING_TYPE_OASIS)) {
            return new JablotronOasisHandler(thing, httpClientFactory.getCommonHttpClient());
        }
        if (thingTypeUID.equals(THING_TYPE_JA100)) {
            return new JablotronJa100Handler(thing, httpClientFactory.getCommonHttpClient());
        }

        return null;
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof JablotronBridgeHandler) {
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.get(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                // remove discovery service, if bridge handler is removed
                JablotronDiscoveryService service = (JablotronDiscoveryService) bundleContext
                        .getService(serviceReg.getReference());
                serviceReg.unregister();
                discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            }
        }
    }

    private synchronized void registerItemDiscoveryService(JablotronBridgeHandler bridgeHandler) {
        JablotronDiscoveryService discoveryService = new JablotronDiscoveryService(bridgeHandler);
        discoveryService.activate(null);
        this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));

    }
}
