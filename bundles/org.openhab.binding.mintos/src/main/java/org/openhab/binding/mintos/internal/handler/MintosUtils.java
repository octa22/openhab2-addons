/**
 * Copyright (c) 2014,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.mintos.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;

import static org.openhab.binding.mintos.internal.MintosBindingConstants.LOGOUT_URL;

/**
 * The {@link MintosUtils} provides common methods, which are
 * reused within handlers.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class MintosUtils {

    public static String getCsrfToken(String content) {
        final String field = "token=\"";
        int pos = content.indexOf(field);
        return content.substring(pos + field.length(), pos + field.length() + 43);
    }

    public static String getLogoutUrl(String text) {
        int pos = text.indexOf(LOGOUT_URL);
        int posEnd = text.indexOf("\" class=\"logout main-nav-logout");
        return text.substring(pos + 9, posEnd);
    }
}
