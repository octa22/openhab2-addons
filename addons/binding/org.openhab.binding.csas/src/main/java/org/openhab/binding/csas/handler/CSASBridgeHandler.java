/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.csas.handler;

import static org.openhab.binding.csas.CSASBindingConstants.*;

import com.google.gson.Gson;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.csas.internal.model.response.CSASRefreshTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * The {@link CSASBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
 @NonNullByDefault
public class CSASBridgeHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(CSASBridgeHandler.class);

    private long refreshInterval = 1800000;
    private String clientId = "";
    private String clientSecret = "";
    private String refreshToken = "";
    private String accessToken = "";
    private String webAPIKey = "";

    //Gson parser
    private Gson gson = new Gson();


    public CSASBridgeHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        /*
        if (channelUID.getId().equals(CHANNEL_1)) {

        }
        */
    }

    @Override
    public void initialize() {
        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        refreshToken();
        updateStatus(ThingStatus.ONLINE);
    }

    private void refreshToken() {
        String url = null;

        try {
            String urlParameters = "client_id=" + clientId + "&client_secret=" + clientSecret + "&redirect_uri=https://localhost/code&grant_type=refresh_token&refresh_token=" + refreshToken;
            url = "https://www.csas.cz/widp/oauth2/token";

            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

            URL cookieUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) cookieUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.write(postData);
            }

            InputStream response = connection.getInputStream();
            String line = readResponse(response);
            logger.debug("CSAS response: " + line);

            CSASRefreshTokenResponse resp = gson.fromJson(line, CSASRefreshTokenResponse.class);
            accessToken = resp.getAccessToken();
            logger.info("Access token: {}", accessToken);
        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed", url, e.toString());
        } catch (Exception e) {
            logger.error("Cannot get CSAS token", e.toString());
        }
    }

    private String readResponse(InputStream response) throws Exception {
        String line;
        StringBuilder body = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(response));

        while ((line = reader.readLine()) != null) {
            body.append(line).append("\n");
        }
        line = body.toString();
        logger.debug(line);
        return line;
    }

}
