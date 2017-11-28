/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.csas.handler;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;

/**
 * The {@link CSASBaseThingHandler} is the super class for handlers.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public abstract class CSASBaseThingHandler extends BaseThingHandler {
    public CSASBaseThingHandler(Thing thing) {
        super(thing);
    }

    protected CSASBridgeHandler getBridgeHandler() {
        return (CSASBridgeHandler) this.getBridge().getHandler();
    }

    protected String getThingId() { return thing.getUID().getId(); }
}
