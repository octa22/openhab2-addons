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
package org.openhab.binding.boschxmpp.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link BoschXmppConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class BoschXmppConfiguration {

    public String serialNumber = "";
    public String accessKey = "";
    public String devicePassword = "";
    public String hostname = "xmpp.rrcng.ticx.boschtt.net";
    public int port = 5222;
    public int refreshInterval = 30;
    public int requestTimeout = 5;
}
