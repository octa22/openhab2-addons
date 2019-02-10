/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.jablotron.internal.model;

import com.google.gson.annotations.SerializedName;
import org.openhab.binding.jablotron.internal.model.oasis.OasisLastEntryCID;

/**
 * The {@link OasisLastEntryCID} class defines the OASIS last trouble
 * object.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class JablotronTrouble {
    private String zekdy;
    private String cas;
    private String message;
    private String name;

    public String getZekdy() {
        return zekdy;
    }

    public String getCas() {
        return cas;
    }

    public String getMessage() {
        return message;
    }

    public String getName() {
        return name;
    }
}
