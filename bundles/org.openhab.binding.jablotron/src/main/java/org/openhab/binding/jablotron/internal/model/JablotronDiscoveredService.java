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
package org.openhab.binding.jablotron.internal.model;

import com.google.gson.annotations.SerializedName;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link JablotronDiscoveredService} class defines the discovered service
 * object
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class JablotronDiscoveredService {

    @SerializedName("service-id")
    int id = 0;
    String name = "";

    @SerializedName("service-type")
    String serviceType = "";

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getServiceType() {
        return serviceType;
    }
}
