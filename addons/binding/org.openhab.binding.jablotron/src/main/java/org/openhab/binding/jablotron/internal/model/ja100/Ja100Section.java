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
 * The {@link Ja100StatusResponse} class defines the JA100 section status
 * object.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class Ja100Section {
    private int stav;
    private String stateName;
    private String nazev;
    private String time;
    private long active;

    public long getStav() {
        return stav;
    }

    public String getStateName() {
        return stateName;
    }

    public String getNazev() {
        return nazev;
    }

    public String getTime() {
        return time;
    }

    public long getActive() {
        return active;
    }
}
