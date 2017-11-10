/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.csas.internal.model;

/**
 * The {@link CSASCard} is represents the model of the
 * CSAS card.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class CSASCard {
    private CSASAccount mainAccount;
    private String type;
    private String state;

    public CSASAccount getMainAccount() {
        return mainAccount;
    }

    public String getType() {
        return type;
    }

    public String getState() {
        return state;
    }
}
