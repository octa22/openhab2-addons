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
package org.openhab.binding.mintos.internal;

/**
 * The {@link MintosConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class MintosConfiguration {

    public String login;
    public String password;
    public String currency;
    public long refreshInterval;
}
