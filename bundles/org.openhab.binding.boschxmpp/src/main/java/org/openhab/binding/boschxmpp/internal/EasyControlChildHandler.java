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

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.boschxmpp.internal.protocol.BoschXmppException;
import org.openhab.binding.boschxmpp.internal.protocol.BoschXmppException.Reason;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Common lifecycle and request handling for Things below an EasyControl bridge. */
@NonNullByDefault
public abstract class EasyControlChildHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private volatile boolean disposed;
    private volatile boolean verified;

    protected EasyControlChildHandler(Thing thing) {
        super(thing);
    }

    protected @Nullable EasyControlBridgeHandler getBridgeHandler() {
        @Nullable
        Bridge bridge = getBridge();
        return bridge != null && bridge.getHandler() instanceof EasyControlBridgeHandler handler ? handler : null;
    }

    protected void initializeChild(boolean validConfiguration, String error) {
        disposed = false;
        verified = false;
        if (!validConfiguration) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, error);
        } else if (getBridgeHandler() == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED,
                    "EasyControl bridge is not available");
        } else {
            updateStatus(ThingStatus.UNKNOWN);
        }
    }

    protected boolean verifyEndpoint(EasyControlBridgeHandler bridge, String endpoint) throws BoschXmppException {
        if (verified) {
            return true;
        }
        try {
            bridge.get(endpoint);
            verified = true;
            return true;
        } catch (BoschXmppException e) {
            if (e.getReason() != Reason.INVALID_RESPONSE) {
                throw e;
            }
            if (!disposed) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "EasyControl endpoint does not exist: " + endpoint);
            }
            return false;
        }
    }

    protected void readOptional(EasyControlBridgeHandler bridge, String endpoint, String channel,
            Function<JsonElement, State> converter) throws BoschXmppException {
        if (!isChannelLinked(channel)) {
            return;
        }
        try {
            JsonObject response = bridge.get(endpoint);
            updateValue(response, channel, converter);
        } catch (BoschXmppException e) {
            if (e.getReason() == Reason.INVALID_RESPONSE) {
                if (!disposed) {
                    updateState(channel, UnDefType.UNDEF);
                }
            } else {
                throw e;
            }
        } catch (RuntimeException e) {
            // One optional field must not make the bridge appear disconnected.
            logger.warn("Invalid EasyControl value from {} for channel {}", endpoint, channel, e);
            if (!disposed) {
                updateState(channel, UnDefType.UNDEF);
            }
        }
    }

    protected void updateValue(JsonObject response, String channel, Function<JsonElement, State> converter) {
        @Nullable
        JsonElement value = response.get("value");
        if (!disposed) {
            try {
                updateState(channel, value == null || value.isJsonNull() ? UnDefType.UNDEF
                        : Objects.requireNonNull(converter.apply(value)));
            } catch (RuntimeException e) {
                logger.warn("Invalid EasyControl value for channel {}", channel, e);
                updateState(channel, UnDefType.UNDEF);
            }
        }
    }

    protected void updateFailureStatus(BoschXmppException e) {
        if (disposed) {
            return;
        }
        if (e.getReason() == Reason.INVALID_RESPONSE) {
            logger.warn("EasyControl rejected a request for {}: {}", getThing().getUID(), e.getMessage());
            return;
        }
        ThingStatusDetail detail = e.getReason() == Reason.AUTHENTICATION || e.getReason() == Reason.DECRYPTION
                ? ThingStatusDetail.CONFIGURATION_ERROR
                : ThingStatusDetail.COMMUNICATION_ERROR;
        updateStatus(ThingStatus.OFFLINE, detail, e.getMessage());
    }

    void bridgeOffline(String description) {
        if (!disposed) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, description);
        }
    }

    @Override
    public void dispose() {
        disposed = true;
        super.dispose();
    }

    protected boolean isDisposed() {
        return disposed;
    }

    protected boolean isChannelLinked(String channelId) {
        return isLinked(channelId);
    }

    abstract void refresh();
}
