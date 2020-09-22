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

import com.google.gson.annotations.SerializedName;

/**
 * The {@link GoodWePowerstationKpi} class class holds performance information
 * about a powerstation bound to the SEMS Portal account.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class GoodWePowerstationKpi {
    double pac = -1;
    double power = -1;

    @SerializedName(value = "total_power")
    double totalPower = -1;

    @SerializedName(value = "day_income")
    double dayIncome = -1;

    @SerializedName(value = "total_income")
    double totalIncome = -1;

    public double getPac() {
        return pac;
    }

    public double getPower() {
        return power;
    }

    public double getTotalPower() {
        return totalPower;
    }

    public double getDayIncome() {
        return dayIncome;
    }

    public double getTotalIncome() {
        return totalIncome;
    }
}
