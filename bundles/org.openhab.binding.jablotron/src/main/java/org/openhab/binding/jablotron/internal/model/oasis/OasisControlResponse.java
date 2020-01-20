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
package org.openhab.binding.jablotron.internal.model.oasis;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link OasisControlResponse} class defines the control command
 * response for OASIS.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class OasisControlResponse {
    private int vysledek = -1;
    private int status = -1;

    public int getVysledek() {
        return vysledek;
    }

    public int getStatus() {
        return status;
    }
}
