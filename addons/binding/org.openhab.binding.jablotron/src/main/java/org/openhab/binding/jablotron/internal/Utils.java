/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
