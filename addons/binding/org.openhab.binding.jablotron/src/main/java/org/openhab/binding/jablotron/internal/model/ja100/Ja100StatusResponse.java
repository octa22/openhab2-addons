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
package org.openhab.binding.jablotron.internal.model.ja100;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import org.openhab.binding.jablotron.handler.JablotronBridgeHandler;
import org.openhab.binding.jablotron.internal.model.JablotronTrouble;
import org.openhab.binding.jablotron.internal.model.oasis.OasisEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * The {@link Ja100StatusResponse} class defines the JA100 get status
 * response.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class Ja100StatusResponse {

    private Gson gson = new Gson();
    private JsonParser parser = new JsonParser();

    private final Logger logger = LoggerFactory.getLogger(JablotronBridgeHandler.class);

    int status;
    JsonElement sekce;
    JsonElement pgm;
    Integer isAlarm;

    //@SerializedName("trouble")
    //ArrayList<JablotronTrouble> troubles;
    //JsonElement alarm;
    //boolean controlDisabled;
    //int service;
    JsonElement teplomery;

    public int getStatus() {
        return status;
    }

    /*
    public JsonElement getVypis() {
        return vypis;
    }*/

    /*
    public int getService() {
        return service;
    }

    public int getIsAlarm() {
        return isAlarm;
    }
    */

    public boolean isOKStatus() {
        return status == 200;
    }

    public boolean isBusyStatus() {
        return status == 201;
    }

    public boolean isNoSessionStatus() {
        return status == 800;
    }


    public boolean inService() {
        //return service == 1;
        return false;
    }

    public boolean isAlarm() {
        return isAlarm != null && isAlarm.intValue() == 1;
    }

    public boolean hasEvents() {
        //return vypis != null && !vypis.equals(JsonNull.INSTANCE);
        return false;
    }

    public boolean hasTemperature() {
        return teplomery != null && !teplomery.isJsonNull() && !teplomery.isJsonArray();
    }

    public boolean hasSectionStatus() {
        return sekce != null && !sekce.isJsonNull();
    }

    public boolean hasPGMStatus() {
        return pgm != null && !pgm.isJsonNull();
    }

    /*
    public Date getLastEventTime() {
        if (lastEntry != null) {
            return getZonedDateTime(lastEntry.cid.time);
        } else
            return null;
    }*/

    private Date getZonedDateTime(long lastEventTime) {
        Instant dt = Instant.ofEpochSecond(lastEventTime);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(dt, ZoneId.of("Europe/Prague"));
        return Date.from(zdt.toInstant());
    }

    public ArrayList<OasisEvent> getEvents() {
        if (!hasEvents()) {
            return null;
        }

        ArrayList<OasisEvent> result = new ArrayList<>();

        return result;
    }

    public ArrayList<Ja100Temperature> getTemperatures() {
        if (!hasTemperature()) {
            return null;
        }

        ArrayList<Ja100Temperature> result = new ArrayList<>();

        JsonObject jobject = teplomery.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : jobject.entrySet()) {
            String key = entry.getKey();
            if (jobject.get(key) instanceof JsonObject) {
                //each device
                JsonObject temp = jobject.get(key).getAsJsonObject();
                Ja100Temperature ev = gson.fromJson(temp, Ja100Temperature.class);
                result.add(ev);
            }
        }
        return result;
    }

    public ArrayList<Ja100Section> getPGMs() {
        return getSectionsCommon(pgm);
    }

    public ArrayList<Ja100Section> getSections() {
        return getSectionsCommon(sekce);
    }

    private ArrayList<Ja100Section> getSectionsCommon(JsonElement section) {
        if (section == null || section.equals(JsonNull.INSTANCE)) {
            return null;
        }

        ArrayList<Ja100Section> result = new ArrayList<>();

        if (section.isJsonArray()) {
            //sections sent as an array
            JsonArray jArray = section.getAsJsonArray();
            for (JsonElement el : jArray) {
                JsonObject status = el.getAsJsonObject();
                Ja100Section sec = gson.fromJson(status, Ja100Section.class);
                result.add(sec);
            }
        } else { JsonObject jobject = section.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jobject.entrySet()) {
                String key = entry.getKey();
                if (jobject.get(key) instanceof JsonObject) {
                    //each device
                    JsonObject status = jobject.get(key).getAsJsonObject();
                    Ja100Section sec = gson.fromJson(status, Ja100Section.class);
                    result.add(sec);
                }
            }
        }
        return result;
    }
}
