/**
 * Copyright (c) 2014,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.zonky.internal.model;

public class ZonkyStatResponse {
    private Number currentProfitability;
    private Number expectedProfitability;
    private ZonkyCurrentOverview currentOverview;
    private ZonkyOverallOverview overallOverview;

    public Number getCurrentProfitability() {
        return currentProfitability;
    }

    public Number getExpectedProfitability() {
        return expectedProfitability;
    }

    public ZonkyCurrentOverview getCurrentOverview() {
        return currentOverview;
    }

    public ZonkyOverallOverview getOverallOverview() {
        return overallOverview;
    }
}
