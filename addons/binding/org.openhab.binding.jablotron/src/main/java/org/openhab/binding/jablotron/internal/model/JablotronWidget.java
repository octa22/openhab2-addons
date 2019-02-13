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

/**
 * The {@link JablotronWidget} class defines the widgets
 * object.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class JablotronWidget {
    private String name;
    private int id;
    private String url;
    private String templateService;

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
}
