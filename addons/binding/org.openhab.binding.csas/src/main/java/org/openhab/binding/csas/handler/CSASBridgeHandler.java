/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.csas.handler;

import com.google.gson.Gson;
import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.ConfigStatusBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.csas.config.CSASConfig;
import org.openhab.binding.csas.internal.discovery.CSASDiscoveryService;
import org.openhab.binding.csas.internal.model.*;
import org.openhab.binding.csas.internal.model.response.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.csas.CSASBindingConstants.*;

/**
 * The {@link CSASBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
public class CSASBridgeHandler extends ConfigStatusBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(CSASBridgeHandler.class);

    private CSASDiscoveryService discoveryService = null;

    private long refreshInterval = 1800000;
    private String accessToken = "";

    /**
     * Our configuration
     */
    protected CSASConfig thingConfig;

    //Gson parser
    private Gson gson = new Gson();

    //Account list
    HashMap<String, String> accountList = new HashMap<>();

    //IbanList
    HashMap<String, String> ibanList = new HashMap<>();

    public CSASBridgeHandler(Bridge thing) {
        super(thing);
    }

    @Override
    public Collection<ConfigStatusMessage> getConfigStatus() {
        return Collections.emptyList();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        /*
        if (channelUID.getId().equals(CHANNEL_1)) {

        }
        */
    }

    @Override
    public void initialize() {
        thingConfig = getConfigAs(CSASConfig.class);
        refreshToken();
        scheduler.schedule(() -> startDiscovery(), 1, TimeUnit.SECONDS);
    }

    public void startDiscovery() {
        if (discoveryService != null) {
            //discover accounts
            getAccounts();
            getCards();
            getBuildingSavings();
            getPensions();
            getInsurances();
            getSecurities();
            getLoyalty();
            listUnboundAccounts();
        }
    }

    private void getLoyalty() {
        String url = null;

        try {
            url = NETBANKING_V3 + "cz/my/contracts/loyalty";

            String line = DoNetbankingRequest(url);
            logger.info("CSAS getLoyalty: " + line);

            CSASLoyaltyResponse resp = gson.fromJson(line, CSASLoyaltyResponse.class);
            if (resp.getState().equals(REGISTERED)) {
                discoveryService.loyaltyContractDiscovered();
            }
        } catch (MalformedURLException e) {
            logger.error("The URL '" + url + "' is malformed: " + e.toString());
        } catch (Exception e) {
            logger.error("Cannot get CSAS loyalty points: " + e.toString());
        }
    }

    private void getSecurities() {
        String url = null;

        try {
            url = NETBANKING_V3 + "my/securities";

            String line = DoNetbankingRequest(url);
            logger.debug("CSAS getSecurities: " + line);

            CSASSecuritiesResponse resp = gson.fromJson(line, CSASSecuritiesResponse.class);
            if (resp.getSecuritiesAccounts() != null) {
                for (CSASSecuritiesAccount mainAccount : resp.getSecuritiesAccounts()) {
                    String id = mainAccount.getId();
                    String accountno = mainAccount.getAccountno();
                    if (!accountList.containsKey(id)) {
                        accountList.put(id, "Securities account: " + accountno);
                        discoveryService.securitiesAccountDiscovered(id, accountno);
                    }
                }
            }
        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed", url, e);
        } catch (Exception e) {
            logger.error("Cannot get CSAS securities", e);
        }
    }

    private void getPensions() {

        String url = null;

        try {
            url = NETBANKING_V3 + "cz/my/contracts/pensions";

            String line = DoNetbankingRequest(url);
            logger.debug("CSAS getPensions: " + line);

            CSASPensions resp = gson.fromJson(line, CSASPensions.class);
            if (resp.getPensions() != null) {
                for (CSASAgreement agreement : resp.getPensions()) {
                    String id = agreement.getId();
                    String number = agreement.getAgreementNumber();
                    if (!accountList.containsKey(id)) {
                        accountList.put(id, "Pension agreement: " + number);
                        discoveryService.pensionContractDiscovered(id, number);
                    }
                }
            }
        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed", url, e);
        } catch (Exception e) {
            logger.error("Cannot get CSAS pensions", e);
        }
    }

    private void getInsurances() {

        String url = null;

        try {
            url = NETBANKING_V3 + "my/contracts/insurances";

            String line = DoNetbankingRequest(url);
            logger.debug("CSAS getInsurances: " + line);

            CSASInsurancesResponse resp = gson.fromJson(line, CSASInsurancesResponse.class);
            if (resp.getInsurances() != null) {
                for (CSASInsurance insurance : resp.getInsurances()) {
                    String id = insurance.getId();
                    String policyNumber = insurance.getPolicyNumber();
                    String productI18N = insurance.getProductI18N();
                    if (!accountList.containsKey(id))
                        accountList.put(id, "Insurance: " + policyNumber + " (" + productI18N + ")");
                    discoveryService.insuranceContractDiscovered(id, policyNumber + " (" + productI18N + ")");
                }
            }
        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed", url, e);
        } catch (Exception e) {
            logger.error("Cannot get CSAS insurances", e);
        }
    }

    private void getBuildingSavings() {

        String url = null;

        try {
            url = NETBANKING_V3 + "my/contracts/buildings";

            String line = DoNetbankingRequest(url);
            logger.debug("CSAS getBuildingSavings: " + line);

            CSASBuildingsResponse resp = gson.fromJson(line, CSASBuildingsResponse.class);
            if (resp.getBuildings() != null) {
                for (CSASAccount account : resp.getBuildings()) {
                    readAccount(account.getId(), account.getAccountno());
                    discoveryService.buildingSavingsAccountDiscovered(account.getId(), account.getAccountno());
                }
            }
        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed", url, e);
        } catch (Exception e) {
            logger.error("Cannot get CSAS building savings", e);
        }
    }

    private void getCards() {

        String url = null;

        try {
            url = NETBANKING_V3 + "my/cards";

            String line = DoNetbankingRequest(url);
            logger.info("CSAS getCards: {}", line);

            CSASCardsResponse resp = gson.fromJson(line, CSASCardsResponse.class);
            if (resp.getCards() != null) {
                for (CSASCard card : resp.getCards()) {
                    CSASAccount cardAccount = card.getMainAccount();
                    if (cardAccount != null && card.getType().equals(CREDIT)) {
                        readAccount(cardAccount.getId(), cardAccount.getAccountno());
                        discoveryService.cardAccountDiscovered(cardAccount.getId(), cardAccount.getAccountno());
                    }
                }
            }

        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed", url, e);
        } catch (Exception e) {
            logger.error("Cannot get CSAS cards", e);
        }
    }

    private void listUnboundAccounts() {
        StringBuilder sb = new StringBuilder();
        Iterator it = accountList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            String id = (String) pair.getKey();
            String acc = (String) pair.getValue();
            //if (!isBound(id))
            sb.append("\t").append(acc).append(" Id: ").append(id).append("\n");
        }
        if (sb.length() > 0) {
            logger.info("Found unbound CSAS account(s): {}\n", sb.toString());
        }
    }

    private void getAccounts() {
        String url = null;

        try {
            url = NETBANKING_V3 + "my/accounts";

            String line = DoNetbankingRequest(url);
            logger.debug("CSAS getAccounts: {}", line);

            CSASAccountsResponse resp = gson.fromJson(line, CSASAccountsResponse.class);
            if (resp.getAccounts() != null) {
                for (CSASAccount account : resp.getAccounts()) {
                    readAccount(account.getId(), account.getAccountno());
                    discoveryService.accountDiscovered(account.getId(), account.getAccountno());
                }
            }
        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed", url, e);
        } catch (Exception e) {
            logger.error("Cannot get CSAS accounts", e);
        }
    }

    private String DoNetbankingRequest(String url) throws Exception {
        URL cookieUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) cookieUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("WEB-API-key", thingConfig.getWebAPIKey());
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        InputStream response = connection.getInputStream();
        return readResponse(response);
    }

    private void readAccount(String id, CSASAccountNumber account) {
        if (account == null)
            return;

        String number = account.getNumber();
        String bankCode = account.getBankCode();
        String iban = account.getIban();
        if (!accountList.containsKey(id)) {
            accountList.put(id, "Account: " + number + "/" + bankCode);
            ibanList.put(id, iban);
        }
    }

    private void refreshToken() {
        String url = null;

        try {
            String urlParameters = "client_id=" + thingConfig.getClientId() + "&client_secret=" + thingConfig.getClientSecret() + "&redirect_uri=https://localhost/code&grant_type=refresh_token&refresh_token=" + thingConfig.getRefreshToken();
            url = "https://www.csas.cz/widp/oauth2/token";

            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

            URL cookieUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) cookieUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.write(postData);
            }

            InputStream response = connection.getInputStream();
            String line = readResponse(response);
            logger.debug("CSAS response: " + line);

            CSASRefreshTokenResponse resp = gson.fromJson(line, CSASRefreshTokenResponse.class);
            accessToken = resp.getAccessToken();
            updateStatus(ThingStatus.ONLINE);
        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed", url, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        } catch (Exception e) {
            logger.error("Cannot get CSAS token", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
        }
    }

    private String readResponse(InputStream response) throws Exception {
        String line;
        StringBuilder body = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(response));

        while ((line = reader.readLine()) != null) {
            body.append(line).append("\n");
        }
        line = body.toString();
        logger.debug(line);
        return line;
    }

    public void setDiscoveryService(CSASDiscoveryService csasDiscoveryService) {
        this.discoveryService = csasDiscoveryService;
    }
}
