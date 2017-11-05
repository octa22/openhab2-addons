package org.openhab.binding.csas.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openhab.binding.csas.CSASBindingConstants.*;

public class CSASAccountHandler extends CSASBaseThingHandler {
    public CSASAccountHandler(Thing thing) {
        super(thing);
    }

    private final Logger logger = LoggerFactory.getLogger(CSASLoyaltyContractHandler.class);

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (!command.equals(RefreshType.REFRESH)) {
            return;
        }

        CSASBridgeHandler handler = getBridgeHandler();
        if( channelUID.getId().equals(CHANNEL_BALANCE_FULL)) {
            handler.updateBalanceFull(channelUID, getId());
        }
        if( channelUID.getId().equals(CHANNEL_BALANCE)) {
            handler.updateBalance(channelUID, getId());
        }
        if( channelUID.getId().equals(CHANNEL_DISPOSABLE_FULL)) {
            handler.updateDisposableFull(channelUID, getId());
        }
        if( channelUID.getId().equals(CHANNEL_DISPOSABLE)) {
            handler.updateDisposable(channelUID, getId());
        }
        if( channelUID.getId().equals(CHANNEL_CURRENCY)) {
            handler.updateCurrency(channelUID, getId());
        }
    }

}
