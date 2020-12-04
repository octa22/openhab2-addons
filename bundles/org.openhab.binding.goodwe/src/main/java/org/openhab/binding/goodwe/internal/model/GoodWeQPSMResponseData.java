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
package org.openhab.binding.goodwe.internal.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link GoodWeQPSMResponseData} class holds detail of the successful QPSM
 * response.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class GoodWeQPSMResponseData {
    int record = -1;
    List<GoodWePowerstationInfo> list = new ArrayList<GoodWePowerstationInfo>();

    public int getRecord() {
        return record;
    }

    public List<GoodWePowerstationInfo> getList() {
        return list;
    }
}
