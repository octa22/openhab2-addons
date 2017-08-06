/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.jablotron.internal.model.oasis;

/**
 * The {@link OasisControlResponse} class defines the control command
 * response for OASIS.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class OasisControlResponse {
    private Integer vysledek;
    private int status;

    public Integer getVysledek() {
        return vysledek;
    }

    public int getStatus() {
        return status;
    }
}
