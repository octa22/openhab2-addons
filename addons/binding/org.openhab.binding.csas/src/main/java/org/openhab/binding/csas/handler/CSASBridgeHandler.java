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
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
import org.eclipse.smarthome.core.cache.ExpiringCacheMap;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.ConfigStatusBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.csas.config.CSASConfig;
import org.openhab.binding.csas.internal.CSASItemType;
import org.openhab.binding.csas.internal.CSASSimpleTransaction;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang.time.DateUtils.addDays;
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

    private String accessToken = "";

    /**
     * Our configuration
     */
    private CSASConfig thingConfig;

    //Gson parser
    private Gson gson = new Gson();

    //Account list
    private HashMap<String, String> accountList = new HashMap<>();

    /**
     * Future to poll for updated
     */
    private ScheduledFuture<?> pollFuture;

    //Cache
    private ExpiringCacheMap<String, CSASAccountBalanceResponse> accountBalance;
    private ExpiringCacheMap<String, ArrayList<CSASSimpleTransaction>> accountTransactions;

    public CSASBridgeHandler(@NonNull Bridge thing) {
        super(thing);
    }

    @Override
    public Collection<ConfigStatusMessage> getConfigStatus() {
        return Collections.emptyList();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        thingConfig = getConfigAs(CSASConfig.class);
        accountBalance = new ExpiringCacheMap<>(CACHE_EXPIRY);
        accountTransactions = new ExpiringCacheMap<String, ArrayList<CSASSimpleTransaction>>(CACHE_EXPIRY);
        refreshToken();

        initPolling(thingConfig.getRefresh());

        if (thing.getStatus().equals(ThingStatus.ONLINE)) {
            scheduler.schedule(this::startDiscovery, 1, TimeUnit.SECONDS);
        }
        logger.info("CSAS binding initialized...");
    }

    @Override
    public void dispose() {
        super.dispose();
        stopPolling();
        logger.info("CSAS binding disposed...");
    }

    /**
     * starts this things polling future
     */
    private void initPolling(int refresh) {
        stopPolling();
        pollFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                updateCSASStates();
            } catch (Exception e) {
                logger.error("Exception during poll!", e);
            }
        }, refresh, refresh, TimeUnit.SECONDS);
    }

    private void updateCSASStates() {
        logger.debug("Updating CSAS states...");
        refreshToken();
        if (!thing.getStatus().equals(ThingStatus.ONLINE)) {
            return;
        }

        for (Thing t : getThing().getThings()) {
            CSASBaseThingHandler handler = (CSASBaseThingHandler) t.getHandler();
            if (handler == null) {
                continue;
            }
            for (Channel channel : t.getChannels()) {
                ChannelUID uid = channel.getUID();
                if (uid != null) {
                    handler.handleCommand(uid, RefreshType.REFRESH);
                }
            }
        }
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

    public void startDiscovery() {
        if (discoveryService != null) {
            //discover products
            getAccounts();
            getCards();
            getBuildingSavings();
            getPensions();
            getInsurances();
            getSecurities();
            getLoyalty();
            listAccounts();
        }
    }

    private void getLoyalty() {
        String url = null;

        try {
            url = NETBANKING_V3 + "cz/my/contracts/loyalty";

            String line = DoNetbankingRequest(url);
            logger.debug("CSAS getLoyalty: {}", line);

            CSASLoyaltyResponse resp = gson.fromJson(line, CSASLoyaltyResponse.class);
            if (resp.getState().equals(REGISTERED)) {
                discoveryService.loyaltyContractDiscovered();
            }
        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed: ", url, e);
        } catch (Exception e) {
            logger.error("Cannot get CSAS loyalty points", e);
        }
    }

    private synchronized CSASAmount getCachedAccountBalance(String accountId, CSASItemType balanceType) {
        if (accountBalance.get(accountId) == null) {
            logger.debug("Putting method into cached map...");
            accountBalance.put(accountId, () -> invokeGetAccountBalance(accountId));
        }

        logger.debug("Getting cached balance for account id: {}", accountId);
        CSASAccountBalanceResponse resp = accountBalance.get(accountId);
        return balanceType.equals(CSASItemType.BALANCE) ? resp.getBalance() : resp.getDisposable();
    }

    private CSASAccountBalanceResponse invokeGetAccountBalance(String accountId) {
        String url = null;

        try {
            url = NETBANKING_V3 + "my/accounts/" + accountId + "/balance";

            String line = DoNetbankingRequest(url);
            logger.debug("CSAS getBalance of account: {} returned: {}", accountId, line);

            return gson.fromJson(line, CSASAccountBalanceResponse.class);
        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed: ", url, e);
        } catch (Exception e) {
            logger.error("Cannot get CSAS balance", e);
        }
        return null;
    }

    private String getAccountBalanceFull(String accountId, CSASItemType balanceType) {
        CSASAmount bal = getCachedAccountBalance(accountId, balanceType);
        String balance = readBalanceWithCurrency(bal);
        return formatMoney(balance);
    }

    private Double getAccountBalance(String accountId, CSASItemType balanceType) {
        CSASAmount bal = getCachedAccountBalance(accountId, balanceType);
        return readBalanceAsDouble(bal);
    }

    private String getAccountCurrency(String accountId) {
        CSASAmount bal = getCachedAccountBalance(accountId, CSASItemType.BALANCE);
        return bal.getCurrency();
    }

    private String formatMoney(String balance) {
        StringBuilder newBalance = new StringBuilder();
        int len = balance.length();
        int dec = balance.indexOf('.');
        if (dec >= 0) {
            len = dec;
            newBalance.append(balance.substring(dec));
        }

        int j = 0;
        for (int i = len - 1; i >= 0; i--) {
            char c = balance.charAt(i);
            newBalance.insert(0, c);
            if (++j % 3 == 0 && i > 0 && balance.charAt(i - 1) != '-')
                newBalance.insert(0, " ");
        }
        return newBalance.toString();
    }

    private String readBalanceWithCurrency(CSASAmount balance) {
        String value = balance.getValue();
        String currency = balance.getCurrency();

        int precision = balance.getPrecision();
        int places = value.length();

        return (precision == 0) ? value + ".00 " + currency : value.substring(0, places - precision) + "." + value.substring(places - precision) + " " + currency;
    }

    private Double readBalanceAsDouble(CSASAmount balance) {
        String value = balance.getValue();

        int precision = balance.getPrecision();
        int places = value.length();

        return (precision == 0) ? Double.parseDouble(value) : Double.parseDouble(value.substring(0, places - precision) + "." + value.substring(places - precision));
    }

    private void getSecurities() {
        String url = null;

        try {
            url = NETBANKING_V3 + "my/securities";

            String line = DoNetbankingRequest(url);
            logger.debug("CSAS getSecurities: {}", line);

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
            logger.debug("CSAS getPensions: {}", line);

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
            logger.debug("CSAS getInsurances: {}", line);

            CSASInsurancesResponse resp = gson.fromJson(line, CSASInsurancesResponse.class);
            if (resp.getInsurances() != null) {
                for (CSASInsurance insurance : resp.getInsurances()) {
                    String id = insurance.getId();
                    String policyNumber = insurance.getPolicyNumber();
                    String productI18N = insurance.getProductI18N();
                    if (!accountList.containsKey(id) && insurance.getStatus().equals(ACTIVE)) {
                        accountList.put(id, "Insurance: " + policyNumber + " (" + productI18N + ")");
                        discoveryService.insuranceContractDiscovered(id, policyNumber + " (" + productI18N + ")");
                    }
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
            logger.debug("CSAS getBuildingSavings: {}", line);

            CSASBuildingsResponse resp = gson.fromJson(line, CSASBuildingsResponse.class);
            if (resp.getBuildings() != null) {
                for (CSASAccount account : resp.getBuildings()) {
                    readAccount(account.getId(), account.getAccountno());
                    discoveryService.buildingSavingsAccountDiscovered(account);
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
            logger.debug("CSAS getCards: {}", line);

            CSASCardsResponse resp = gson.fromJson(line, CSASCardsResponse.class);
            if (resp.getCards() != null) {
                for (CSASCard card : resp.getCards()) {
                    CSASAccount cardAccount = card.getMainAccount();
                    if (cardAccount != null && card.getType().equals(CREDIT) && card.getState().equals(ACTIVE)) {
                        readAccount(cardAccount.getId(), cardAccount.getAccountno());
                        discoveryService.cardAccountDiscovered(cardAccount);
                    }
                }
            }

        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed", url, e);
        } catch (Exception e) {
            logger.error("Cannot get CSAS cards", e);
        }
    }

    private void listAccounts() {
        logger.debug("Found CSAS product(s): {}\n", getAccountList());
    }

    private String getAccountList() {
        StringBuilder sb = new StringBuilder();
        for (Object o : accountList.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            String id = (String) pair.getKey();
            String acc = (String) pair.getValue();
            sb.append("\t").append(acc).append(" Id: ").append(id).append("\n");
        }
        return sb.toString();
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
                    discoveryService.accountDiscovered(account);
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
        if (!accountList.containsKey(id)) {
            accountList.put(id, "Account: " + number + "/" + bankCode);
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
            logger.debug("CSAS response: {}", line);

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

    public void updateBalanceFull(ChannelUID channelUID, String id) {
        String balance = getAccountBalanceFull(id, CSASItemType.BALANCE);
        updateState(channelUID, new StringType(balance));
    }

    public void updateBalance(ChannelUID channelUID, String id) {
        Double balance = getAccountBalance(id, CSASItemType.BALANCE);
        updateState(channelUID, new DecimalType(balance));
    }

    public void updateDisposableFull(ChannelUID channelUID, String id) {
        String balance = getAccountBalanceFull(id, CSASItemType.DISPOSABLE_BALANCE);
        updateState(channelUID, new StringType(balance));
    }

    public void updateDisposable(ChannelUID channelUID, String id) {
        Double balance = getAccountBalance(id, CSASItemType.DISPOSABLE_BALANCE);
        updateState(channelUID, new DecimalType(balance));
    }

    public void updateCurrency(ChannelUID channelUID, String id) {
        String currency = getAccountCurrency(id);
        updateState(channelUID, new StringType(currency));
    }

    public void updateLoyaltyPoints(ChannelUID channelUID) {
        String url = null;

        try {
            url = NETBANKING_V3 + "cz/my/contracts/loyalty";

            String line = DoNetbankingRequest(url);
            logger.debug("CSAS getLoyalty: {}", line);

            CSASLoyaltyResponse resp = gson.fromJson(line, CSASLoyaltyResponse.class);
            if (resp.getState().equals(REGISTERED)) {
                State state = new DecimalType(Integer.parseInt(resp.getPointsCount()));
                updateState(channelUID, state);
            }
        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed: ", url, e);
        } catch (Exception e) {
            logger.error("Cannot get CSAS loyalty points", e);
        }
    }

    /*
    private String getIbanFromAccountId(String accountId) {
        if (ibanList.containsKey(accountId)) {
            return ibanList.get(accountId);
        }

        logger.debug("Cannot get IBAN for account: {}", accountId);
        return "";
    }*/

    private CSASSimpleTransaction createTransaction(CSASTransaction csasTran) {

        SimpleDateFormat myUTCFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        SimpleDateFormat requiredFormat = new SimpleDateFormat("dd.MM.yyyy");

        CSASSimpleTransaction tran = new CSASSimpleTransaction();

        try {
            CSASAmount amount = csasTran.getAmount();

            Date date = myUTCFormat.parse(csasTran.getBookingDate());
            String shortDate = requiredFormat.format(date);

            String balance = formatMoney(readBalanceWithCurrency(amount)) + " " + shortDate;
            String description = csasTran.getDescription();
            tran.setBalance(balance);

            if (description != null) {
                tran.setDescription(description);
            }

            String variableSymbol = csasTran.getVariableSymbol();
            if (variableSymbol != null) {
                tran.setVariableSymbol(variableSymbol);
            }

            CSASAccountParty party = csasTran.getAccountParty();
            if (party == null) {
                return tran;
            }
            if (party.getAccountPartyInfo() != null) {
                String accountPartyInfo = party.getAccountPartyInfo();
                tran.setAccountPartyInfo(accountPartyInfo);
            }
            if (party.getAccountPartyDescription() != null) {
                String accountPartyDescription = party.getAccountPartyDescription();
                tran.setAccountPartyDescription(accountPartyDescription);
            }
        } catch (Exception ex) {
            logger.error("Error during parsing transaction!", ex);
        }
        return tran;
    }

    private ArrayList<CSASSimpleTransaction> invokeGetTransactions(String iban) {

        String url = null;
        ArrayList<CSASSimpleTransaction> transactionsList = new ArrayList<>();

        SimpleDateFormat requestFormat = new SimpleDateFormat("yyyy-MM-dd");

        try {
            //url = NETBANKING_V3 + "cz/my/accounts/" + getIbanFromAccountId(accountId) + "/transactions?dateStart=" + requestFormat.format(addDays(new Date(), -HISTORY_INTERVAL)) + "T00:00:00+01:00&dateEnd=" + requestFormat.format(new Date()) + "T00:00:00+01:00";
            url = NETBANKING_V3 + "cz/my/accounts/" + iban + "/transactions?dateStart=" + requestFormat.format(addDays(new Date(), -HISTORY_INTERVAL)) + "T00:00:00+01:00&dateEnd=" + requestFormat.format(new Date()) + "T00:00:00+01:00";

            String line = DoNetbankingRequest(url);
            logger.debug("CSAS getTransactions: {}", line);

            CSASTransactionsResponse resp = gson.fromJson(line, CSASTransactionsResponse.class);
            if (resp.getTransactions() != null) {
                for (CSASTransaction tran : resp.getTransactions()) {
                    transactionsList.add(createTransaction(tran));
                }
            }

            logger.trace("Transactions: {}", transactionsList.toString());
            return transactionsList;

        } catch (MalformedURLException e) {
            logger.error("The URL '{}' is malformed: ", url, e);
        } catch (Exception e) {
            logger.error("Cannot get CSAS transactions: ", e);
        }
        return transactionsList;
    }

    private synchronized ArrayList<CSASSimpleTransaction> getCachedTransactions(String accountId, String iban) {
        if (accountTransactions.get(accountId) == null) {
            logger.info("Putting getTransactions method into cached map...");
            accountTransactions.put(accountId, () -> invokeGetTransactions(iban));
        }
        return accountTransactions.get(accountId);
    }

    private void updateTransaction(ChannelUID channelUID, String id, String iban, int position) {
        ArrayList<CSASSimpleTransaction> transactions = getCachedTransactions(id, iban);
        if(transactions.size() > position) {
            updateState(channelUID, new StringType(transactions.get(position).getBalance()));
        } else {
            updateState(channelUID, new StringType("-"));
        }
    }

    public void updateTransaction(ChannelUID channelUID, String id, String iban) {
        /*
        if (getIbanFromAccountId(id).isEmpty()) {
            logger.info("IBAN list not ready yet");
            return;
        }*/

        String tran = channelUID.getId().replace(TRAN, "");
        if (tryParseInt(tran)) {
            int position = Integer.parseInt(tran);
            updateTransaction(channelUID, id, iban, position);
        }
    }

    private boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
