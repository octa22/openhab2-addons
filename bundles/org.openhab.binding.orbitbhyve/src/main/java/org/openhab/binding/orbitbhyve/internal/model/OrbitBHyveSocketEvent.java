/**
 * Copyright (c) 2014,2019 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.orbitbhyve.internal.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

/**
 * The {@link OrbitBHyveSocketEvent} holds information about a B-Hyve
 * event received on web socket.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class OrbitBHyveSocketEvent {

    String event;
    String mode;
    JsonElement program;
    int delay;

    @SerializedName("device_id")
    String deviceId;

    @SerializedName("current_station")
    int station;

    public String getEvent() {
        return event;
    }

    public String getMode() {
        return mode;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public int getStation() {
        return station;
    }

    public JsonElement getProgram() {
        return program;
    }

    public int getDelay() {
        return delay;
    }
}
