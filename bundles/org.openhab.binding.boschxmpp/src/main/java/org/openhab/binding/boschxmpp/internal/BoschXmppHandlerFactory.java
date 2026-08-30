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
package org.openhab.binding.boschxmpp.internal;

import static org.openhab.binding.boschxmpp.internal.BoschXmppBindingConstants.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link BoschXmppHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.boschxmpp", service = ThingHandlerFactory.class)
public class BoschXmppHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_EASYCONTROL, THING_TYPE_ZONE,
            THING_TYPE_DEVICE, THING_TYPE_DHW);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_EASYCONTROL.equals(thingTypeUID)) {
            return new EasyControlBridgeHandler(thing);
        } else if (THING_TYPE_ZONE.equals(thingTypeUID)) {
            return new EasyControlZoneHandler(thing);
        } else if (THING_TYPE_DEVICE.equals(thingTypeUID)) {
            return new EasyControlDeviceHandler(thing);
        } else if (THING_TYPE_DHW.equals(thingTypeUID)) {
            return new EasyControlDhwHandler(thing);
        }

        return null;
    }
}
