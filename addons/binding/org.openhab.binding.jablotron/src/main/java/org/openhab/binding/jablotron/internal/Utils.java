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

import java.util.Calendar;
import java.util.Date;

/**
 * The {@link Utils} class defines the common
 * utils.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class Utils {
    public static String getBrowserTimestamp() {
        return "_=" + System.currentTimeMillis();
    }

    public static int getHoursOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        return cal.get(Calendar.HOUR_OF_DAY);
    }
}
