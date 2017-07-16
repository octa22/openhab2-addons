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
 * The {@link EfergyEngageGetCurrentValuesResponse} represents the model of
 * the error response of getting current values summary.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class EfergyEngageGetCurrentValuesResponse {
    @Nullable
    EfergyEngageError error;

    public @Nullable EfergyEngageError getError() {
        return error;
    }
}
