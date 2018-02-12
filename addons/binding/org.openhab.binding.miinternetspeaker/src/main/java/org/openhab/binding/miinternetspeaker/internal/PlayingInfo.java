/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.miinternetspeaker.internal;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * The {@link PlayingInfo} is responsible for holding information about playing
 * title and artist.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class PlayingInfo {
    private String title;
    private String artist;

    public PlayingInfo(String title, String artist) {
        this.title = StringEscapeUtils.unescapeHtml(title);
        this.artist = StringEscapeUtils.unescapeHtml(artist);
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    @Override
    public String toString() {
        return "PlayingInfo{" +
                "title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                '}';
    }
}
