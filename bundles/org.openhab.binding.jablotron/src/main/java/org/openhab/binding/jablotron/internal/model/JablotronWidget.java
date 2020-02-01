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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link JablotronWidget} class defines the widgets
 * object.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class JablotronWidget {
    private String name = "";
    private int id = -1;
    private String url = "";
    private String templateService = "";
    private List<JablotronWidgetSekce> sekce = new ArrayList<>();
    private int noticeCount = -1;

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getTemplateService() {
        return templateService;
    }

    public List<JablotronWidgetSekce> getSekce() {
        return sekce;
    }

    public int getNoticeCount() {
        return noticeCount;
    }
}
