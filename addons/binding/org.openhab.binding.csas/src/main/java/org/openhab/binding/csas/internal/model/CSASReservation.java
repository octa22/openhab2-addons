/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.csas.internal.model;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link CSASReservation} is represents the model of the
 * CSAS account reservations.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class CSASReservation {
    private String creationDate;
    private CSASAmount amount;
    private String description;
    private String merchantName;

    @SerializedName("cz-merchantAddress")
    private String merchantAddress;

    public String getCreationDate() {
        return creationDate;
    }

    public CSASAmount getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getMerchantAddress() {
        return merchantAddress;
    }

    public String getMerchantName() {
        return merchantName;
    }
}
