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
package org.openhab.binding.goodwe.internal.model;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link GoodWePowerstationInfo} class holds information about a powerstation
 * bound to the SEMS Portal account.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class GoodWePowerstationInfo {
    double capacity = -1;
    int status = -1;

    @SerializedName(value = "battery_capacity")
    double batteryCapacity = -1;

    @SerializedName(value = "stationname")
    private String stationName = "";

    @SerializedName(value = "powerstation_id")
    private String id = "";

    public double getCapacity() {
        return capacity;
    }

    public int getStatus() {
        return status;
    }

    public double getBatteryCapacity() {
        return batteryCapacity;
    }

    public String getStationName() {
        return stationName;
    }

    public String getId() {
        return id;
    }
}
