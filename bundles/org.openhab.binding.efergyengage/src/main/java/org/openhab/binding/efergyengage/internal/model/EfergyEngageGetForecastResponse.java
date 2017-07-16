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
package org.openhab.binding.efergyengage.internal.model;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link EfergyEngageGetForecastResponse} represents the model of
 * the monthly money spending estimation response.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class EfergyEngageGetForecastResponse {

    @SerializedName("month_tariff")
    EfergyEngageEstimate monthTariff = new EfergyEngageEstimate();

    @Nullable
    EfergyEngageError error;

    public EfergyEngageEstimate getMonthTariff() {
        return monthTariff;
    }

    public @Nullable EfergyEngageError getError() {
        return error;
    }
}
