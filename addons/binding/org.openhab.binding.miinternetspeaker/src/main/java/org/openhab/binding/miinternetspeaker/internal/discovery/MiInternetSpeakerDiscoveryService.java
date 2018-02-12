/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.miinternetspeaker.internal.discovery;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryServiceCallback;
import org.eclipse.smarthome.config.discovery.ExtendedDiscoveryService;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.miinternetspeaker.handler.MiInternetSpeakerHandler;
import org.openhab.binding.miinternetspeaker.internal.MiInternetSpeakerDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.openhab.binding.miinternetspeaker.MiInternetSpeakerBindingConstants.*;
import static org.openhab.binding.miinternetspeaker.internal.Utils.isOKPacket;
import static org.openhab.binding.miinternetspeaker.internal.Utils.readResponse;

/**
 * The {@link MiInternetSpeakerDiscoveryService} is responsible for discovering compatible
 * things.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class MiInternetSpeakerDiscoveryService extends AbstractDiscoveryService implements ExtendedDiscoveryService {

    private final Logger logger =
            LoggerFactory.getLogger(MiInternetSpeakerDiscoveryService.class);

    private DiscoveryServiceCallback discoveryServiceCallback;

    //speaker devices
    private HashMap<String, MiInternetSpeakerDevice> devices = new HashMap<>();
    private String deviceUrl = "";

    //xml
    private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    //location
    private String location = "";

    //thread
    private Thread thread;

    //Socket
    private MulticastSocket socket = null;

    private byte[] buffer = new byte[BUFFER_LENGTH];
    private DatagramPacket dgram = new DatagramPacket(buffer, buffer.length);


    public MiInternetSpeakerDiscoveryService() {
        super(DISCOVERY_TIMEOUT_SEC);
        setupSocket();
        startScan();
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return Collections.singleton(THING_TYPE_SPEAKER);
    }

    @Override
    protected void startScan() {
        discoverXiaomiSpeakerDevices();
    }

    @Override
    public void setDiscoveryServiceCallback(DiscoveryServiceCallback discoveryServiceCallback) {
        this.discoveryServiceCallback = discoveryServiceCallback;
    }

    @Override
    protected synchronized void deactivate() {
        super.deactivate();
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    private void setupSocket() {
        try {
            socket = new MulticastSocket(); // must bind receive side
            socket.joinGroup(InetAddress.getByName(MCAST_ADDR));
        } catch (IOException e) {
            logger.error("Setup socket error: {}", e.toString());
        }

        thread = new Thread(new Runnable() {
            public void run() {
                receiveData(socket, dgram);
            }
        });
        thread.start();
    }

    private void receiveData(MulticastSocket socket, DatagramPacket dgram) {

        try {
            while (socket != null && !socket.isClosed()) {
                socket.receive(dgram);
                String sentence = new String(dgram.getData(), 0,
                        dgram.getLength());

                if (isOKPacket(sentence)) {
                    logger.debug("Received packet: {}", sentence);
                    String[] lines = sentence.split("\n");

                    boolean compatible = false;

                    for (String line : lines) {
                        line = line.replace("\r", "");
                        line = line.replace("\n", "");

                        if (line.startsWith("LOCATION: "))
                            location = line.substring(10);
                        else if (line.contains("ST: urn:schemas-upnp-org:device:UmiSystem:1"))
                            compatible = true;
                    }

                    if (compatible) {
                        parseLocation(location);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Receive data error: {}", e.toString());
        }
    }

    private void parseLocation(String location) {
        try {
            URL cookieUrl = new URL(location);
            HttpURLConnection connection = (HttpURLConnection) cookieUrl.openConnection();

            connection.setRequestMethod("GET");
            String response = readResponse(connection);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document dom = builder.parse(new ByteArrayInputStream(response.getBytes()));
            Node node = dom.getElementsByTagName("root").item(0);
            String friendlyName = node.getChildNodes().item(1).getChildNodes().item(1).getTextContent();
            String manufacturer = node.getChildNodes().item(1).getChildNodes().item(2).getTextContent();
            String modelDescription = node.getChildNodes().item(1).getChildNodes().item(3).getTextContent();
            String modelName = node.getChildNodes().item(1).getChildNodes().item(4).getTextContent();
            String uuid = node.getChildNodes().item(1).getChildNodes().item(9).getTextContent().replace("uuid:", "");
            deviceUrl = location.replace("/Upnp/device.xml", "-MR/");

            if (!devices.containsKey(deviceUrl)) {
                MiInternetSpeakerDevice device = new MiInternetSpeakerDevice(friendlyName, manufacturer, modelDescription, modelName);
                devices.put(deviceUrl, device);
                logger.info("Detected a new Xiaomi Internet Speaker device: ");
                logger.info("Friendly name: {}", friendlyName);
                logger.info("Manufacturer: {}", manufacturer);
                logger.info("Description: {}", modelDescription);
                logger.info("Model name: {}", modelName);
                logger.info("Uuid: {}", uuid);
            }
            MiInternetSpeakerDevice device = devices.get(deviceUrl);
            speakerDiscovered(friendlyName + " " + modelName, deviceUrl, uuid, device);

        } catch (Exception ex) {
            logger.error("Device discovery error: {}", ex.toString());
        }
    }

    public void speakerDiscovered(String label, String deviceURL, String uuid, MiInternetSpeakerDevice device) {
        Map<String, Object> properties = new HashMap<>(1);
        properties.put("deviceUrl", deviceURL);

        ThingUID thingUID = new ThingUID(BINDING_ID, THING_TYPE_SPEAKER.getId(), uuid);

        Thing thing = discoveryServiceCallback.getExistingThing(thingUID);
        if (thing == null) {
            logger.info("Detected a speaker - label: {} uuid: {}", label, uuid);
            thingDiscovered(
                    DiscoveryResultBuilder.create(thingUID).withThingType(THING_TYPE_SPEAKER).withProperties(properties)
                            .withRepresentationProperty("url").withLabel(label)
                            .build());
        } else {
            MiInternetSpeakerHandler handler = (MiInternetSpeakerHandler) thing.getHandler();
            if( handler != null ) {
                handler.setModel(device.getManufacturer(), device.getModelName());
            }
        }
    }

    private void discoverXiaomiSpeakerDevices() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("M-SEARCH * HTTP/1.1\r\n");
            sb.append("HOST: 239.255.255.250:1900\r\n");
            sb.append("MAN: \"ssdp:discover\"\r\n");
            sb.append("MX: 5\r\n");
            sb.append("User-Agent: ");
            sb.append(USER_AGENT);
            sb.append("\r\n");
            sb.append("ST: urn:schemas-upnp-org:device:UmiSystem:1\r\n");
            sb.append("\r\n");
            byte[] sendData = sb.toString().getBytes("UTF-8");
            InetAddress addr = InetAddress.getByName(MCAST_ADDR);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, addr, MCAST_PORT);
            logger.debug("Sending discover packet");
            socket.send(sendPacket);
        } catch (Exception e) {
            logger.error("Cannot discover Xiaomi internet speaker devices", e);
        }
    }
}
