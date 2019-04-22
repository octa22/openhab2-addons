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

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

/**
 * The {@link OrbitBHyveDevice} holds information about a B-Hyve
 * device.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class OrbitBHyveDevice {
    String name;
    String type;
    String id;
    List<OrbitBHyveZone> zones;
    OrbitBHyveDeviceStatus status;

    @SerializedName("is_connected")
    boolean isConnected;

    @SerializedName("hardware_version")
    String hwVersion;

    @SerializedName("firmware_version")
    String fwVersion;

    @SerializedName("mac_address")
    String macAddress;

    @SerializedName("num_stations")
    int numStations;

    @SerializedName("last_connected_at")
    String lastConnectedAt;

    JsonObject location;

    @SerializedName("restricted_frequency")
    JsonObject restrictedFrequency;

    @SerializedName("suggested_start_time")
    String suggestedStartTime;

    JsonObject timezone;

    @SerializedName("water_sense_mode")
    String waterSenseMode;

    @SerializedName("wifi_version")
    int wifiVersion;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public String getHwVersion() {
        return hwVersion;
    }

    public String getFwVersion() {
        return fwVersion;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public int getNumStations() {
        return numStations;
    }

    public List<OrbitBHyveZone> getZones() {
        return zones;
    }

    public String getId() {
        return id;
    }

    public OrbitBHyveDeviceStatus getStatus() {
        return status;
    }

    public String getWaterSenseMode() {
        return waterSenseMode;
    }

    public void setWaterSenseMode(String waterSenseMode) {
        this.waterSenseMode = waterSenseMode;
    }
}
