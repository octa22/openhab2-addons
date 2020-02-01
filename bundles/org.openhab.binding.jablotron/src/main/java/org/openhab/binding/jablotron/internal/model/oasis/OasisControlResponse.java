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
package org.openhab.binding.jablotron.internal.model.oasis;

import com.google.gson.annotations.SerializedName;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link OasisControlResponse} class defines the control command
 * response for OASIS.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class OasisControlResponse {
    private boolean status = false;

    @SerializedName("error_message")
    String errorMessage = "";

    public boolean isStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
