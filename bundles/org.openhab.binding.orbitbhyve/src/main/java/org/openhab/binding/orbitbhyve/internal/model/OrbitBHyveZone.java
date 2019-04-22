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

import com.google.gson.JsonArray;
import com.google.gson.annotations.SerializedName;

/**
 * The {@link OrbitBHyveZone} holds information about a B-Hyve
 * zone.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class OrbitBHyveZone {
    String name;
    int station;

    @SerializedName("catch_cup_run_time")
    int catchCupRunTime;

    @SerializedName("catch_cup_volumes")
    JsonArray catchCupVolumes;

    @SerializedName("num_sprinklers")
    int numSprinklers;

    @SerializedName("landscape_type")
    String landscapeType;

    @SerializedName("soil_type")
    String soilType;

    @SerializedName("sprinkler_type")
    String sprinklerType;

    @SerializedName("sun_shade")
    String sunShade;

    @SerializedName("slope_grade")
    int slopeGrade;

    @SerializedName("image_url")
    String imageUrl;

    @SerializedName("smart_watering_enabled")
    boolean smartWateringEnabled;

    public String getName() {
        return name;
    }

    public int getStation() {
        return station;
    }

    public boolean isSmartWateringEnabled() {
        return smartWateringEnabled;
    }

    public void setSmartWateringEnabled(boolean smartWateringEnabled) {
        this.smartWateringEnabled = smartWateringEnabled;
    }
}
