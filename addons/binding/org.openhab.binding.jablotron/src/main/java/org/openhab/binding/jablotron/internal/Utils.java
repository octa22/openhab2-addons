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
package org.openhab.binding.jablotron.internal;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;

/**
 * The {@link Utils} class defines the common
 * utils.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class Utils {
    public static String getSessionCookie(HttpsURLConnection connection) {

        String headerName;
        for (int i = 1; (headerName = connection.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equals("Set-Cookie")) {
                if (connection.getHeaderField(i).startsWith("PHPSESSID")) {
                    int semicolon = connection.getHeaderField(i).indexOf(";");
                    return connection.getHeaderField(i).substring(0, semicolon);
                }
            }
        }
        return "";
    }

    public static String readResponse(HttpsURLConnection connection) throws Exception {
        synchronized(Utils.class) {
            InputStream stream = connection.getInputStream();
            String line;
            StringBuilder body = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            while ((line = reader.readLine()) != null) {
                body.append(line).append("\n");
            }
            line = body.toString();
            //logger.debug(line);
            return line;
        }
    }

    public static String getBrowserTimestamp() {
        return "_=" + System.currentTimeMillis();
    }

    public static int getHoursOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        return cal.get(Calendar.HOUR_OF_DAY);
    }
}
