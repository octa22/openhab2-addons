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
package org.openhab.binding.efergyengage.internal;

import static org.openhab.binding.efergyengage.internal.EfergyEngageBindingConstants.EFERGY_ENGAGE_HUB;
import static org.openhab.binding.efergyengage.internal.EfergyEngageBindingConstants.EFERGY_ENGAGE_SENSOR;

import java.util.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.io.net.http.HttpClientFactory;
import org.openhab.binding.efergyengage.internal.handler.EfergyEngageHubHandler;
import org.openhab.binding.efergyengage.internal.handler.EfergyEngageSensorHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link EfergyEngageHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.efergyengage")
public class EfergyEngageHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>(
            Arrays.asList(EFERGY_ENGAGE_HUB, EFERGY_ENGAGE_SENSOR));

    /**
     * the shared http client
     */
    private @NonNullByDefault({}) HttpClient httpClient;

    @Activate
    public EfergyEngageHandlerFactory(@Reference HttpClientFactory httpClientFactory) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (EFERGY_ENGAGE_HUB.equals(thingTypeUID)) {
            return new EfergyEngageHubHandler((Bridge) thing, httpClient);
        } else if (EFERGY_ENGAGE_SENSOR.equals(thingTypeUID)) {
            return new EfergyEngageSensorHandler(thing);
        }

        return null;
    }
}
