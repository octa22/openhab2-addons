/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
 * The {@link GoodWeQPSMResponse} class holds information about the query power
 * station monitor response of the SEMS Portal.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class GoodWeQPSMResponse extends GoodWeGenericResponse {
    private GoodWeQPSMResponseData data = new GoodWeQPSMResponseData();

    public GoodWeQPSMResponseData getData() {
        return data;
    }
}
