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

/**
 * The {@link EfergyEngageEstimate} represents the model of
 * the montly money spending estimation based on consumption this month.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class EfergyEngageEstimate {
    float estimate = 0;
    float previousSum = 0;

    public float getEstimate() {
        return estimate;
    }

    public float getPreviousSum() {
        return previousSum;
    }
}
