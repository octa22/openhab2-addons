/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.csas.config;

/**
 * The {@link CSASConfig} is responsible for holding bridge configuration.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class CSASConfig {
    private String clientId;
    private String clientSecret;
    private String refreshToken;
    private String webAPIKey;
    private int refresh;

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getWebAPIKey() {
        return webAPIKey;
    }

    public int getRefresh() {
        return refresh;
    }
}
