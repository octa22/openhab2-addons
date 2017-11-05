package org.openhab.binding.csas.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openhab.binding.csas.CSASBindingConstants.CHANNEL_POINTS;

public class CSASLoyaltyContractHandler extends CSASBaseThingHandler {
    public CSASLoyaltyContractHandler(Thing thing) {
        super(thing);
    }

    private final Logger logger = LoggerFactory.getLogger(CSASLoyaltyContractHandler.class);

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command.equals(RefreshType.REFRESH) && channelUID.getId().equals(CHANNEL_POINTS)) {
            CSASBridgeHandler handler = getBridgeHandler();
            handler.updateLoyaltyPoints(channelUID);
        }
    }
}
