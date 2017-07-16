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
package org.openhab.binding.efergyengage.internal.model;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.JsonArray;

/**
 * The {@link EfergyEngageData} represents the model of
 * the efergy engage data.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class EfergyEngageData {
    String cid = "";
    String sid = "";
    JsonArray data = new JsonArray();
    Integer age = 0;
    String units = "W";

    public String getCid() {
        return cid;
    }

    public String getSid() {
        return sid;
    }

    public JsonArray getData() {
        return data;
    }

    public Integer getAge() {
        return age;
    }

    public String getUnits() {
        return units;
    }
}
