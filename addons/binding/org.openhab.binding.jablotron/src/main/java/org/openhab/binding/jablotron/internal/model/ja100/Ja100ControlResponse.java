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
package org.openhab.binding.jablotron.internal.model.ja100;

/**
 * The {@link Ja100ControlResponse} class defines the control command
 * response for ja100.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class Ja100ControlResponse {
    private Integer result;
    private int responseCode;
    private int authorization;

    public Integer getResult() {
        return result;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public int getAuthorization() {
        return authorization;
    }
}
