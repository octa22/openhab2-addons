/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.csas.internal.model.response;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link CSASRefreshTokenResponse} is represents the response model of the
 * token refreshing operation.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class CSASRefreshTokenResponse {
    @SerializedName("access_token")
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }
}
