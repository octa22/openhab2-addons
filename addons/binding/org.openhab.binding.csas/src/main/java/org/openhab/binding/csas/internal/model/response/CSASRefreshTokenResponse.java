package org.openhab.binding.csas.internal.model.response;

import com.google.gson.annotations.SerializedName;

public class CSASRefreshTokenResponse {
    @SerializedName("access_token")
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }
}
