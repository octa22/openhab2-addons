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

/**
 * The {@link EfergyEngageGetEnergyResponse} represents the model of
 * the response of total power consumptions.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class EfergyEngageGetEnergyResponse {
    float sum = 0;
    String units = "";
    @Nullable
    EfergyEngageError error;

    public float getSum() {
        return sum;
    }

    public String getUnits() {
        return units;
    }

    public @Nullable EfergyEngageError getError() {
        return error;
    }
}
