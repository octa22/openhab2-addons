/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.miinternetspeaker.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.smarthome.core.library.types.*;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.miinternetspeaker.internal.PlayingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.miinternetspeaker.MiInternetSpeakerBindingConstants.*;
import static org.openhab.binding.miinternetspeaker.internal.Utils.readResponse;

/**
 * The {@link MiInternetSpeakerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class MiInternetSpeakerHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(MiInternetSpeakerHandler.class);

    //deviceURL from configuration
    private String deviceUrl = "";

    //thread for async playing info update
    private Thread thread;

    //Gson parser
    private JsonParser parser = new JsonParser();


    /**
     * Future to poll for updated
     */
    private ScheduledFuture<?> pollFuture;

    //xml
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    public MiInternetSpeakerHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("received command {} for channel {}", command, channelUID.getId());

        if (!thing.getStatus().equals(ThingStatus.ONLINE) || command.equals(RefreshType.REFRESH)) {
            return;
        }

        switch (channelUID.getId()) {
            case CHANNEL_CONTROL:
                if( command instanceof PlayPauseType) {
                    sendCommandToSpeaker(command.equals(PlayPauseType.PAUSE) ? "pause" : "play");
                }
                if( command instanceof NextPreviousType) {
                    sendCommandToSpeaker(command.equals(NextPreviousType.NEXT) ? "next" : "prev");
                }
                break;
            case CHANNEL_VOLUME:
                setVolume(((PercentType) command).intValue());
                break;
            case CHANNEL_BLUETOOTH:
                if (command instanceof OnOffType) {
                    enableBluetooth(command.equals(OnOffType.ON));
                }
                break;
            case CHANNEL_COMMAND:
                sendCommandToSpeaker(command.toString());
                break;
            case CHANNEL_SLEEP:
                sendSleepToSpeaker(command.toString());
                break;
            case CHANNEL_SOUND:
                sendSoundToSpeaker(command.toString());
                break;
            case CHANNEL_PLAYMODE:
                sendPlayModeToSpeaker(command.toString());
                break;
            default:
                logger.error("Unknown channel: {}", channelUID.getId());
        }

    }

    @Override
    public void initialize() {
        deviceUrl = getThing().getConfiguration().get("deviceUrl").toString();

        initPolling(30);
        if (deviceUrl.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Device url is not properly initialized - probably non compatible device");
        }
    }

    @Override
    public void dispose() {
        stopPolling();
    }

    /**
     * Stops this thing's polling future
     */
    private void stopPolling() {
        if (pollFuture != null && !pollFuture.isCancelled()) {
            pollFuture.cancel(true);
            pollFuture = null;
        }
    }

    /**
     * starts this things polling future
     */
    private void initPolling(int refresh) {
        stopPolling();
        pollFuture = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    execute();
                } catch (Exception e) {
                    logger.debug("Exception during poll : {}", e);
                }
            }
        }, 0, refresh, TimeUnit.SECONDS);

    }

    private String enableBluetooth(Boolean enable) {
        return (enable) ? sendSetStringToSpeaker("bt_function", "on") : sendSetStringToSpeaker("bt_function", "off");
    }

    private String sendSetStringToSpeaker(String variable, String value) {
        String url;
        try {
            url = deviceUrl.replace("-MR", "") + "xiaomi.com-SystemProperties-1/control";
            StringBuilder sb = new StringBuilder();
            sb.append(XML_HEADER);
            sb.append(SOAP_ENVELOPE);
            sb.append(SOAP_BODY);
            sb.append("<u:SetString xmlns:u=\"urn:xiaomi-com:service:SystemProperties:1\">");
            sb.append("<VariableName>");
            sb.append(variable);
            sb.append("</VariableName>");
            sb.append("<StringValue>");
            sb.append(value);
            sb.append("</StringValue>");
            sb.append("</u:SetString>");
            sb.append(SOAP_BODY_END);
            sb.append(SOAP_ENVELOPE_END);
            return sendMessageToSpeaker(url, sb.toString(), "urn:xiaomi-com:service:SystemProperties:1#SetString");
        } catch (NoRouteToHostException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Thing is probably offline");
        } catch (Exception ex) {
            logger.error("SendSetStringToSpeaker error", ex);
        }
        return "";
    }

    private String sendMessageToSpeaker(String url, String urlParameters, String action) throws Exception {
        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

        URL cookieUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) cookieUrl.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
        connection.setRequestProperty("soapaction", action);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
            wr.write(postData);
        }
        String response = readResponse(connection);
        logger.debug("Response: {}", response);
        return response;
    }

    private void sendCommandToSpeaker(String command) {
        try {
            switch (command.toLowerCase()) {
                case "play":
                    sendPlayToSpeaker();
                    asyncDelayedUpdatePlayingInfo(2000);
                    break;
                case "pause":
                    sendPauseToSpeaker();
                    break;
                case "next":
                    sendNextToSpeaker();
                    asyncDelayedUpdatePlayingInfo(2000);
                    break;
                case "prev":
                    sendPrevToSpeaker();
                    asyncDelayedUpdatePlayingInfo(2000);
                    break;
                case "off":
                    sendOffToSpeaker();
                    break;
                default:
                    logger.error("Unknown command: {}", command.toLowerCase());
            }
        } catch (Exception ex) {
            logger.error("SendCommandToSpeaker error", ex);
        }
    }

    private void asyncDelayedUpdatePlayingInfo(int delay) {
        thread = new Thread(new Runnable() {
            public void run() {

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                updatePlayingInfo();
            }
        });
        thread.start();
    }

    private void getDeviceStatus() {
        Object[] statuses = getStatusItems();

        try {
            String status = getStatus();
            if (status == null) {
                return;
            } else {
                if (!thing.getStatus().equals(ThingStatus.ONLINE)) {
                    updateStatus(ThingStatus.ONLINE);
                }
            }
            for (Object channel : statuses) {
                State newState = new StringType(status);
                updateState(((Channel) channel).getUID(), newState);
            }
        } catch (Exception ex) {
            logger.error("GetDeviceStatus error", ex);
        }
    }

    private void updatePlayingInfo() {
        Object[] artists = getArtistItems();
        Object[] titles = getTitleItems();

        try {

            if (artists.length > 0 || titles.length > 0) {
                PlayingInfo pi = getPlayingInfo();
                if (pi == null) {
                    return;
                }
                String artist = "";
                String title = "";

                artist = pi.getArtist();
                title = pi.getTitle();

                for (Object channel : artists) {
                    State newState = new StringType(artist);
                    updateState(((Channel) channel).getUID(), newState);
                }
                for (Object channel : titles) {
                    State newState = new StringType(title);
                    updateState(((Channel) channel).getUID(), newState);
                }
            }
        } catch (Exception ex) {
            logger.error("UpdatePlayingInfo error", ex);
        }
    }

    private Object[] getSleepItems() {
        return getItems(CHANNEL_SLEEP);
    }

    private Object[] getBluetoothItems() {
        return getItems(CHANNEL_BLUETOOTH);
    }

    private Object[] getVolumeItems() {
        return getItems(CHANNEL_VOLUME);
    }

    private Object[] getSoundItems() {
        return getItems(CHANNEL_SOUND);
    }

    private Object[] getTitleItems() {
        return getItems(CHANNEL_TITLE);
    }

    private Object[] getArtistItems() {
        return getItems(CHANNEL_ARTIST);
    }

    private Object[] getPlayModeItems() {
        return getItems(CHANNEL_PLAYMODE);
    }

    private Object[] getStatusItems() {
        return getItems(CHANNEL_STATUS);
    }

    private Object[] getItems(String type) {
        ArrayList<Channel> items = new ArrayList<>();
        for (Channel channel : getThing().getChannels()) {
            if (channel.getUID().getId().equals(type)) {
                items.add(channel);
            }
        }

        return items.toArray();
    }

    private PlayingInfo getPlayingInfo() {
        String response = sendAVTransportToSpeaker("GetPositionInfo");
        if (response == null) {
            return null;
        }
        logger.debug(response);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document dom = builder.parse(new ByteArrayInputStream(response.getBytes()));
            Node node = dom.getElementsByTagName("s:Envelope").item(0);
            Node getPositionInfo = node.getChildNodes().item(0).getChildNodes().item(0);
            for (int i = 0; i < getPositionInfo.getChildNodes().getLength(); i++) {
                Node meta = getPositionInfo.getChildNodes().item(i);
                if (meta.getNodeName().equals("TrackMetaData")) {
                    return createPlayingInfo(meta.getTextContent());
                }
            }
        } catch (Exception ex) {
            logger.error("GetPlayingInfo error", ex);
        }
        return null;
    }

    private PlayingInfo createPlayingInfo(String metaData) {
        //logger.info(metaData);
        String[] lines = metaData.split(System.getProperty("line.separator"));
        String artist = "";
        String title = "";
        for (String line : lines) {
            if (line.contains("<dc:creator>")) {
                artist = line.replace("<dc:creator>", "").replace("</dc:creator>", "").trim();
                continue;
            }
            if (line.contains("<dc:title>")) {
                title = line.replace("<dc:title>", "").replace("</dc:title>", "").trim();
                continue;
            }
        }
        return new PlayingInfo(title, artist);
    }

    private String sendPlayToSpeaker() {
        return sendAVTransportToSpeaker("Play", "<Speed>1</Speed>");
    }

    private String sendPrevToSpeaker() {
        return sendAVTransportToSpeaker("Previous");
    }

    private String sendNextToSpeaker() {
        return sendAVTransportToSpeaker("Next");
    }

    private String sendPauseToSpeaker() {
        return sendAVTransportToSpeaker("Pause");
    }

    private String sendAVTransportToSpeaker(String command) {
        return sendAVTransportToSpeaker(command, "");
    }

    private String sendAVTransportToSpeaker(String command, String config) {
        String url;
        try {
            url = deviceUrl + "upnp.org-AVTransport-1/control";
            StringBuilder sb = new StringBuilder();
            sb.append(XML_HEADER);
            sb.append(SOAP_ENVELOPE);
            sb.append(SOAP_BODY);
            sb.append("<u:");
            sb.append(command);
            sb.append(" xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\">");
            sb.append("<InstanceID>0</InstanceID>");
            sb.append(config);
            sb.append("</u:");
            sb.append(command);
            sb.append(">");
            sb.append(SOAP_BODY_END);
            sb.append(SOAP_ENVELOPE_END);

            return sendMessageToSpeaker(url, sb.toString(), "urn:schemas-upnp-org:service:AVTransport:1#" + command);
        } catch (NoRouteToHostException | ConnectException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Thing is probably offline");
        } catch (Exception ex) {
            logger.error("SendAVTransportToSpeaker error", ex);
        }
        return null;
    }

    private String sendOffToSpeaker() {
        return sendSleepToSpeaker("1");
    }

    private String sendSleepToSpeaker(String delay) {
        return sendSetStringToSpeaker("delay_stop", delay);
    }

    private String sendSoundToSpeaker(String sound) {
        switch (sound) {
            case "NORMAL":
                return sendSetStringToSpeaker("eq_param", "{&quot;value&quot;:&quot;0,0,0,0,0&quot;,&quot;type&quot;:&quot;Normal&quot;,&quot;version&quot;:1}");
            case "VOICE":
                return sendSetStringToSpeaker("eq_param", "{&quot;value&quot;:&quot;-4,-1,2,2,-3&quot;,&quot;type&quot;:&quot;Voice&quot;,&quot;version&quot;:1}");
            case "BASS":
                return sendSetStringToSpeaker("eq_param", "{&quot;value&quot;:&quot;3,2,0,0,0&quot;,&quot;type&quot;:&quot;Bass&quot;,&quot;version&quot;:1}");
            case "TREBLE":
                return sendSetStringToSpeaker("eq_param", "{&quot;value&quot;:&quot;0,0,0,2,3&quot;,&quot;type&quot;:&quot;Treble&quot;,&quot;version&quot;:1}");
            default:
                logger.error("Cannot set sound mode: {}", sound);
        }
        return "";
    }

    private String sendPlayModeToSpeaker(String mode) {
        switch (mode) {
            case "REPEAT_SHUFFLE":
                return sendAVTransportToSpeaker("SetPlayMode", "<NewPlayMode>REPEAT_SHUFFLE</NewPlayMode>");
            case "REPEAT_ONE":
                return sendAVTransportToSpeaker("SetPlayMode", "<NewPlayMode>REPEAT_ONE</NewPlayMode>");
            case "REPEAT_ALL":
                return sendAVTransportToSpeaker("SetPlayMode", "<NewPlayMode>REPEAT_ALL</NewPlayMode>");
            default:
                logger.error("Cannot set play mode: {}", mode);
        }
        return "";
    }

    private String getBluetoothState() {
        String response = sendGetStringToSpeaker("bt_function");
        try {
            return getDataFromXMLValue(response, 0);
        } catch (Exception ex) {
            logger.error("GetBluetoothState error", ex);
        }
        return "off";
    }

    protected void execute() {
        logger.debug("execute() method is called!");

        Object[] items;
        if (deviceUrl.equals("")) {
            return;
        }

        getDeviceStatus();

        if (thing.getStatus().equals(ThingStatus.ONLINE)) {
            //update playing info
            updatePlayingInfo();

            //update bluetooth value
            items = getBluetoothItems();
            if (items.length > 0) {
                String btState = getBluetoothState();
                for (Object item : items) {
                    State newState = btState.equals("on") ? OnOffType.ON : OnOffType.OFF;
                    updateState(((Channel) item).getUID(), newState);
                }
            }

            //update volume
            items = getVolumeItems();
            if (items.length > 0) {
                int volume = getVolume();
                for (Object item : items) {
                    State newState = new PercentType(volume);
                    updateState(((Channel) item).getUID(), newState);
                }
            }

            //update sound
            items = getSoundItems();
            if (items.length > 0) {
                String soundMode = getSoundMode();
                for (Object item : items) {
                    State newState = new StringType(soundMode);
                    updateState(((Channel) item).getUID(), newState);
                }
            }

            //update sleep
            items = getSleepItems();
            if (items.length > 0) {
                String sleep = getSleepDelay();
                for (Object item : items) {
                    State newState = new StringType(sleep);
                    updateState(((Channel) item).getUID(), newState);
                }
            }

            //update play mode
            items = getPlayModeItems();
            if (items.length > 0) {
                String mode = getPlayMode();
                for (Object item : items) {
                    State newState = new StringType(mode);
                    updateState(((Channel) item).getUID(), newState);
                }
            }
        }
    }

    private String sendGetStringToSpeaker(String variable) {
        String url;

        try {
            url = deviceUrl.replace("-MR", "") + "xiaomi.com-SystemProperties-1/control";
            String urlParameters = XML_HEADER + SOAP_ENVELOPE + "<s:Body s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><u:GetString xmlns:u=\"urn:xiaomi-com:service:SystemProperties:1\"><VariableName>" + variable + "</VariableName></u:GetString>" + SOAP_BODY_END + SOAP_ENVELOPE_END;
            return sendMessageToSpeaker(url, urlParameters, "urn:xiaomi-com:service:SystemProperties:1#GetString");
        } catch (NoRouteToHostException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Thing is probably offline");
        } catch (Exception ex) {
            logger.error("SendGetStringToSpeaker error", ex);
        }
        return "";
    }

    private String setVolume(int volume) {
        String url;

        try {
            url = deviceUrl + "upnp.org-RenderingControl-1/control";
            String urlParameters = XML_HEADER + SOAP_ENVELOPE + "<s:Body s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><u:SetVolume xmlns:u=\"urn:schemas-upnp-org:service:RenderingControl:1\"><InstanceID>0</InstanceID><Channel>Master</Channel><DesiredVolume>" + volume + "</DesiredVolume></u:SetVolume>" + SOAP_BODY_END + SOAP_ENVELOPE_END;
            return sendMessageToSpeaker(url, urlParameters, "urn:schemas-upnp-org:service:RenderingControl:1#SetVolume");
        } catch (NoRouteToHostException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Thing is probably offline");
        } catch (Exception ex) {
            logger.error("SetVolume error", ex);
        }
        return "";
    }

    private int getVolume() {
        String url;

        try {
            url = deviceUrl + "upnp.org-RenderingControl-1/control";
            String urlParameters = XML_HEADER + SOAP_ENVELOPE + "<s:Body s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><u:GetVolume xmlns:u=\"urn:schemas-upnp-org:service:RenderingControl:1\"><InstanceID>0</InstanceID><Channel>Master</Channel></u:GetVolume>" + SOAP_BODY_END + SOAP_ENVELOPE_END;
            String response = sendMessageToSpeaker(url, urlParameters, "urn:schemas-upnp-org:service:RenderingControl:1#GetVolume");
            String volume = getDataFromXMLValue(response, 0);
            return Integer.parseInt(volume);
        } catch (NoRouteToHostException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Thing is probably offline");
        } catch (Exception ex) {
            logger.error("GetVolume error", ex);
        }
        return 0;
    }

    private String getSoundMode() {
        String response = sendGetStringToSpeaker("eq_param");
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document dom = builder.parse(new ByteArrayInputStream(response.getBytes()));
            Node node = dom.getElementsByTagName("s:Envelope").item(0);
            String json = node.getChildNodes().item(0).getChildNodes().item(0).getChildNodes().item(0).getTextContent();
            JsonObject jobject = parser.parse(json.replace("&quot;", "\"")).getAsJsonObject();

            return jobject.get("type").getAsString().toUpperCase();
        } catch (Exception ex) {
            logger.error("GetSoundMode error", ex);
        }
        return "NORMAL";
    }

    private String getSleepDelay() {
        String response = sendGetStringToSpeaker("delay_stop");
        try {
            String value = getDataFromXMLValue(response, 0);
            return value.split(",")[0];
        } catch (Exception ex) {
            logger.error("GetSleepDelay error", ex);
        }
        return "0";
    }

    private String getPlayMode() {
        String response = sendAVTransportToSpeaker("GetTransportSettings", "");
        try {
            String value = getDataFromXMLValue(response, 0);
            return value;
        } catch (Exception ex) {
            logger.error("GetPlayMode error", ex);
        }
        return "";
    }

    private String getStatus() {

        String response = sendAVTransportToSpeaker("GetTransportInfo", "");
        if (response == null) {
            return null;
        }
        try {
            String value = getDataFromXMLValue(response, 0);
            return value;
        } catch (Exception ex) {
            logger.error("GetStatus error", ex);
        }
        return "";
    }

    private String getDataFromXMLValue(String xml, int item) throws Exception {
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document dom = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        Node node = dom.getElementsByTagName("s:Envelope").item(0);
        return node.getChildNodes().item(0).getChildNodes().item(0).getChildNodes().item(item).getTextContent();
    }

    public void setModel(String manufacturer, String model) {
        Map<String, String> properties = editProperties();
        properties.put("Manufacturer", manufacturer);
        properties.put("Model", model);
        updateProperties(properties);
    }
}
