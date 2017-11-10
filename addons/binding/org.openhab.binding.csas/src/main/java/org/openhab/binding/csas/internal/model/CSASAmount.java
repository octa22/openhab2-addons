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
 * The {@link CSASAmount} is represents the model of the
 * CSAS payment amount.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class CSASAmount {
    private String value;
    private String currency;
    private int precision;

    public String getValue() {
        return value;
    }

    public String getCurrency() {
        return currency;
    }

    public int getPrecision() {
        return precision;
    }
}
