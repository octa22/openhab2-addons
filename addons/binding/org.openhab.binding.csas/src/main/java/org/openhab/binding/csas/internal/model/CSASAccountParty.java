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
 * The {@link CSASAccountParty} is represents the model of the
 * CSAS account party.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class CSASAccountParty {
    private String accountPartyDescription;
    private String accountPartyInfo;

    public String getAccountPartyDescription() {
        return accountPartyDescription;
    }

    public String getAccountPartyInfo() {
        return accountPartyInfo;
    }
}
