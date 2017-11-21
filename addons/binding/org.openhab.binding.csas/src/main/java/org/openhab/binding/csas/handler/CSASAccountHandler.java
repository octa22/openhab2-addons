/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.csas.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openhab.binding.csas.CSASBindingConstants.*;

/**
 * The {@link CSASAccountHandler} is responsible for handling commands, which are
 * sent to one of the channels of the account compatible thing.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class CSASAccountHandler extends CSASBaseThingHandler {
    public CSASAccountHandler(Thing thing) {
        super(thing);
    }

    private final Logger logger = LoggerFactory.getLogger(CSASLoyaltyContractHandler.class);

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (!command.equals(RefreshType.REFRESH) || !isLinked(channelUID.getId())) {
            return;
        }

        CSASBridgeHandler handler = getBridgeHandler();
        switch (channelUID.getId()) {
            case CHANNEL_BALANCE_FULL:
                handler.updateBalanceFull(channelUID, getId());
                break;
            case CHANNEL_BALANCE:
                handler.updateBalance(channelUID, getId());
                break;
            case CHANNEL_CURRENCY:
                handler.updateCurrency(channelUID, getId());
                break;
            default:
                if (channelUID.getId().startsWith(TRAN)) {
                    handler.updateTransaction(channelUID, getId(), getIBAN());
                } else {
                    logger.error("Unknown channel: {}", channelUID.getId());
                }
        }
    }

    public String getIBAN() {
        return getThing().getConfiguration().get("iban").toString();
    }
}
