/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.csas.internal;

import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.csas.handler.*;
import org.openhab.binding.csas.internal.discovery.CSASDiscoveryService;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;

import java.util.*;

import static org.openhab.binding.csas.CSASBindingConstants.*;

/**
 * The {@link CSASHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, immediate = true, configurationPid = "binding.csas")
public class CSASHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>(Arrays.asList(
            THING_TYPE_BRIDGE,
            THING_TYPE_ACCOUNT,
            THING_TYPE_BS_ACCOUNT,
            THING_TYPE_CARD_ACCOUNT,
            THING_TYPE_SECURITIES_ACCOUNT,
            THING_TYPE_INSURANCE_CONTRACT,
            THING_TYPE_LOYALTY_CONTRACT,
            THING_TYPE_PENSION_CONTRACT
        ));
    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_BRIDGE)) {
            CSASBridgeHandler handler = new CSASBridgeHandler((Bridge) thing);
            registerItemDiscoveryService(handler);
            return handler;
        }
        if (thingTypeUID.equals(THING_TYPE_ACCOUNT)) {
            return new CSASAccountHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_CARD_ACCOUNT)) {
            return new CSASCardAccountHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_BS_ACCOUNT)) {
            return new CSASBuildingSavingAccountHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_SECURITIES_ACCOUNT)) {
            return new CSASSecuritiesAccountHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_PENSION_CONTRACT)) {
            return new CSASPensionContractHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_INSURANCE_CONTRACT)) {
            return new CSASInsuranceContractHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_LOYALTY_CONTRACT)) {
            return new CSASLoyaltyContractHandler(thing);
        }
        return null;
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof CSASBridgeHandler) {
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.get(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                // remove discovery service, if bridge handler is removed
                CSASDiscoveryService service = (CSASDiscoveryService) bundleContext
                        .getService(serviceReg.getReference());
                serviceReg.unregister();
                discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            }
        }
    }

    private synchronized void registerItemDiscoveryService(CSASBridgeHandler bridgeHandler) {
        CSASDiscoveryService discoveryService = new CSASDiscoveryService(bridgeHandler);
        this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));

    }

}
