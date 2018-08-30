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
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
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
        try {
            switch (channelUID.getId()) {
                case CHANNEL_BALANCE_FULL:
                    handler.updateBalanceFull(channelUID, getThingId());
                    break;
                case CHANNEL_BALANCE:
                    handler.updateBalance(channelUID, getThingId());
                    break;
                case CHANNEL_CURRENCY:
                    handler.updateCurrency(channelUID, getThingId());
                    break;
                default:
                    if (channelUID.getId().startsWith(TRAN)) {
                        handler.updateTransaction(channelUID, getThingId(), getIBAN());
                    } else {
                        logger.error("Unknown channel: {}", channelUID.getId());
                    }
            }
        } catch (NumberFormatException ex) {
            logger.error("Account {} doesn't exist!", getIBAN(), ex);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Account doesn't exist");
        }
    }

    public String getIBAN() {
        return getThing().getConfiguration().get("iban").toString();
    }
}
