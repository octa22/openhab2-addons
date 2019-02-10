/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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

import java.util.ArrayList;

/**
 * The {@link JablotronWidgetsResponse} class defines the get widgets
 * response.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class JablotronWidgetsResponse {
    private int status;

    @SerializedName("cnt-widgets")
    private int cntWidgets;

    @SerializedName("widget")
    private ArrayList<JablotronWidget> widgets;

    public int getStatus() {
        return status;
    }

    public int getCntWidgets() {
        return cntWidgets;
    }

    public ArrayList<JablotronWidget> getWidgets() {
        return widgets;
    }

    public boolean isOKStatus() {
        return status == 200;
    }
}
