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
 * The {@link GoodWePSMDetailResponse} class holds detailed information about the power
 * station monitor.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class GoodWePSMDetailResponse extends GoodWeGenericResponse {
    private GoodWePSMDetailResponseData data = new GoodWePSMDetailResponseData();

    public GoodWePSMDetailResponseData getData() {
        return data;
    }
}
