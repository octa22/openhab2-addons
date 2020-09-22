/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.goodwe.internal.model;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link GoodWePowerstationStats} class class holds statistics
 * about a powerstation bound to the SEMS Portal account.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class GoodWePowerstationStats {
    double sum = -1;
    double buy = -1;
    double buyPercent = -1;
    double sell = -1;
    double sellPercent = -1;
    double selfUseOfPv = -1;
    double consumptionOfLoad = -1;

    public double getSum() {
        return sum;
    }

    public double getBuy() {
        return buy;
    }

    public double getBuyPercent() {
        return buyPercent;
    }

    public double getSell() {
        return sell;
    }

    public double getSellPercent() {
        return sellPercent;
    }

    public double getSelfUseOfPv() {
        return selfUseOfPv;
    }

    public double getConsumptionOfLoad() {
        return consumptionOfLoad;
    }
}
