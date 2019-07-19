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
package org.openhab.binding.mintos.internal;

import java.util.HashMap;

/**
 * The {@link MintosCurrencies} class holds the list of the iso codes and
 * currency abbreviations.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public final class MintosCurrencies {

    private static HashMap<Integer, String> currencies = new HashMap<>();

    static {
        addCurrency(203, "CZK");
        addCurrency(208, "DKK");
        addCurrency(398, "KZT");
        addCurrency(484, "MXN");
        addCurrency(643, "RUB");
        addCurrency(752, "SEK");
        addCurrency(826, "GBP");
        addCurrency(840, "USD");
        addCurrency(946, "RON");
        addCurrency(978, "EUR");
        addCurrency(981, "GEL");
        addCurrency(985, "PLN");
    }

    private static void addCurrency(int iso, String abbreviation) {
        currencies.put(iso, abbreviation);
    }

    public static String getAbbreviation(int iso) {
        return currencies.getOrDefault(iso, "N/A");
    }

    public static String getAbbreviation(String iso) {
        return getAbbreviation(Integer.parseInt(iso));
    }
}
