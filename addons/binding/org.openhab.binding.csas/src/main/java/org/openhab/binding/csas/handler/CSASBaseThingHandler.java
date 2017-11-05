package org.openhab.binding.csas.handler;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;

public abstract class CSASBaseThingHandler extends BaseThingHandler {
    public CSASBaseThingHandler(Thing thing) {
        super(thing);
    }

    protected CSASBridgeHandler getBridgeHandler() {
        return (CSASBridgeHandler) this.getBridge().getHandler();
    }

    protected String getId() {
        return getThing().getConfiguration().get("id").toString();
    }
}
