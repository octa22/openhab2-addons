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
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;

/**
 * The {@link JablotronWidgetsResponse} class defines the get widgets
 * response.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class JablotronWidgetsResponse {
    private int status = -1;

    @SerializedName("cnt-widgets")
    private int cntWidgets = -1;

    @SerializedName("widget")
    private @Nullable ArrayList<JablotronWidget> widgets;

    public int getStatus() {
        return status;
    }

    public int getCntWidgets() {
        return cntWidgets;
    }

    public @Nullable ArrayList<JablotronWidget> getWidgets() {
        return widgets;
    }

    public boolean isOKStatus() {
        return status == 200;
    }
}
