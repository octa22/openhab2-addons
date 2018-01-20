/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.miinternetspeaker;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link MiInternetSpeakerBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class MiInternetSpeakerBindingConstants {

    public static final String BINDING_ID = "miinternetspeaker";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_SPEAKER = new ThingTypeUID(BINDING_ID, "speaker");

    // List of all Channel ids
    public static final String CHANNEL_CONTROL = "control";
    public static final String CHANNEL_STATUS = "status";
    public static final String CHANNEL_BLUETOOTH = "bluetooth";
    public static final String CHANNEL_VOLUME = "volume";
    public static final String CHANNEL_COMMAND = "command";
    public static final String CHANNEL_SOUND = "sound";
    public static final String CHANNEL_SLEEP = "sleep";
    public static final String CHANNEL_PLAYMODE = "playmode";
    public static final String CHANNEL_ARTIST = "artist";
    public static final String CHANNEL_TITLE = "title";

    // Other constants
    public static final String USER_AGENT = "Posix/200112.0 UPnP/1.1 umi/1.0";
    public static final String MCAST_ADDR = "239.255.255.250";
    public static final String XML_HEADER = "<?xml version=\"1.0\"?>";
    public static final String SOAP_ENVELOPE = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">";
    public static final String SOAP_ENVELOPE_END = "</s:Envelope>";
    public static final String SOAP_BODY = "<s:Body>";
    public static final String SOAP_BODY_END = "</s:Body>";
    public static final int MCAST_PORT = 1900;
    public static final int BUFFER_LENGTH = 1024;
    public static final int DISCOVERY_TIMEOUT_SEC = 10;
}
