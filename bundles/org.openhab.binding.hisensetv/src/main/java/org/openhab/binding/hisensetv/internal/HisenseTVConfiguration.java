/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.binding.hisensetv.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Configuration of the TV's built-in MQTT broker and remote identity.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class HisenseTVConfiguration {
    public String host = "";
    public int port = 36669;
    public boolean tls = true;
    public String certificateFingerprint = "";
    public String username = "hisenseservice";
    public String password = "multimqttservice";
    public String remoteId = "XX:XX:XX:XX:XX:XY";
    public int refreshInterval = 30;
}
