/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.miinternetspeaker.internal;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

/**
 * The {@link Utils} contains useful utility methods which are used across the whole
 * binding.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class Utils {
    public static String readResponse(HttpURLConnection connection) throws Exception {
        InputStream stream = connection.getInputStream();
        String line;
        StringBuilder body = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        while ((line = reader.readLine()) != null) {
            body.append(line).append("\n");
        }
        line = body.toString();
        return line;
    }

    public static boolean isOKPacket(String sentence) {
        return sentence.startsWith("HTTP/1.1 200 OK");
    }
}
