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
package org.openhab.binding.jablotron.internal.model.ja100;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link Ja100ControlResponse} class defines the control command
 * response for ja100.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class Ja100ControlResponse {
    private int result;
    private int responseCode = -1;
    private int authorization = -1;

    public int getResult() {
        return result;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public int getAuthorization() {
        return authorization;
    }
}
