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
package org.openhab.binding.efergyengage.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link EfergyEngageHubConfig} is responsible for representing the
 * Efergy Engage Hub configuration.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class EfergyEngageHubConfig {

    private String token = "";
    private int utcOffset = 0;
    private int refresh = 300;

    public String getToken() {
        return token;
    }

    public int getUtcOffset() {
        return utcOffset;
    }

    public int getRefresh() {
        return refresh;
    }
}
