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

public class CSASAccountNumber {
    private String number;
    private String bankCode;

    @SerializedName("cz-iban")
    private String iban;

    public String getNumber() {
        return number;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getIban() {
        return iban;
    }

    public String getFullAccount() {
        return number + "/" + bankCode;
    }
}
