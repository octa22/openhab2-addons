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
package org.openhab.binding.jablotron.internal.model;

import com.google.gson.annotations.SerializedName;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link JablotronServiceDetailSegment} class defines the service segment detail object
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class JablotronServiceDetailSegment {

    @SerializedName("segment_id")
    String segmentId = "";

    @SerializedName("segment_state")
    String segmentState = "";

    public String getSegmentId() {
        return segmentId;
    }

    public String getSegmentState() {
        return segmentState;
    }
}
