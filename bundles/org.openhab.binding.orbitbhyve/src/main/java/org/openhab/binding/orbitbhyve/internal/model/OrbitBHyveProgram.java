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

import com.google.gson.annotations.SerializedName;

/**
 * The {@link OrbitBHyveProgram} holds information about a B-Hyve
 * device programs.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class OrbitBHyveProgram {
    @SerializedName("device_id")
    String deviceId;

    String program;
    String name;
    String id;
    boolean enabled;

    public String getDeviceId() {
        return deviceId;
    }

    public String getProgram() {
        return program;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
