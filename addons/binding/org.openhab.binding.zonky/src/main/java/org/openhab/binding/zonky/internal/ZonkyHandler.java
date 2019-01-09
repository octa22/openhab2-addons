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
package org.openhab.binding.zonky.internal;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.zonky.internal.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.openhab.binding.zonky.internal.ZonkyBindingConstants.*;

/**
 * The {@link ZonkyHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class ZonkyHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(ZonkyHandler.class);

    @Nullable
    private ZonkyConfiguration config;
    private String token;
    private String refreshToken;

    // Future
    @Nullable
    private ScheduledFuture<?> future = null;

    // Gson & parser
    private final Gson gson = new Gson();

    // Instantiate and configure the SslContextFactory
    private SslContextFactory sslContextFactory = new SslContextFactory();

    // Instantiate HttpClient with the SslContextFactory
    private HttpClient httpClient = new HttpClient(sslContextFactory);


    public ZonkyHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            // TODO: handle data refresh
        }

    }

    @Override
    public void initialize() {
        // logger.debug("Start initializing!");
        config = getConfigAs(ZonkyConfiguration.class);

        updateStatus(ThingStatus.UNKNOWN);

        httpClient.setFollowRedirects(false);

        try {
            if (httpClient.isStarted()) {
                httpClient.stop();
            }
            httpClient.start();
        } catch (Exception e) {
            logger.debug("Cannot start http client", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR, "Cannot start http client");
            return;
        }

        if (login()) {
            future = scheduler.scheduleWithFixedDelay(this::refresh, 0, config.refresh, TimeUnit.MILLISECONDS);
        }
    }

    private boolean login() {
        String url;

        if (config == null) {
            return false;
        }

        try {
            //login
            url = ZONKY_URL + "oauth/token";
            String urlParameters = "username=" + config.username + "&password=" + config.password + "&grant_type=password&scope=SCOPE_APP_WEB";
            ContentResponse content = sendRequestBuilder(url, HttpMethod.POST)
                    .header("Authorization", "Basic d2ViOndlYg==")
                    .content(new StringContentProvider(urlParameters), "application/x-www-form-urlencoded; charset=UTF-8")
                    .send();

            logger.trace("response: {}", content.getContentAsString());
            ZonkyTokenResponse response = gson.fromJson(content.getContentAsString(), ZonkyTokenResponse.class);

            token = response.getAccessToken();
            refreshToken = response.getRefreshToken();
            if (!token.isEmpty()) {
                logger.info("Successfully logged in to Zonky!");
                updateStatus(ThingStatus.ONLINE);
                return true;
            }
        } catch (JsonSyntaxException e) {
            logger.debug("Received invalid data", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Received invalid data");
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.debug("Cannot get login cookie!", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot get login cookie");
        }
        return false;
    }

    private Boolean refreshToken() {
        String url;

        try {
            //login
            url = ZONKY_URL + "oauth/token";
            String urlParameters = "refresh_token=" + refreshToken + "&grant_type=refresh_token&scope=SCOPE_APP_WEB";
            ContentResponse content = sendRequestBuilder(url, HttpMethod.POST)
                    .header("Authorization", "Basic d2ViOndlYg==")
                    .content(new StringContentProvider(urlParameters), "application/x-www-form-urlencoded; charset=UTF-8")
                    .send();

            ZonkyTokenResponse response = gson.fromJson(content.getContentAsString(), ZonkyTokenResponse.class);
            token = response.getAccessToken();
            refreshToken = response.getRefreshToken();
            return true;

        } catch (JsonSyntaxException e) {
            logger.debug("Received invalid data", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Received invalid data");
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.debug("Cannot refresh token!", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot refresh token");
        }
        return false;
    }


    private Request sendRequestBuilder(String url, HttpMethod method) {
        return httpClient.newRequest(url)
                .method(method)
                .header(HttpHeader.ACCEPT_LANGUAGE, "en-US,en")
                .header(HttpHeader.ACCEPT_ENCODING, "gzip, deflate")
                .header("X-Requested-With", "XMLHttpRequest")
                .agent(USER_AGENT);
    }

    @Override
    public void dispose() {
        if (!token.isEmpty()) {
            logout();
        }
        if (future != null && !future.isCancelled()) {
            future.cancel(true);
        }
        super.dispose();
    }

    private void logout() {
        String url;

        try {
            //logout
            url = ZONKY_URL + "users/me/logout";

            sendRequestBuilder(url, HttpMethod.GET)
                    .header("Authorization", "Bearer " + token)
                    .send();

        } catch (Exception e) {
            // silence
        }
    }

    private void refresh() {
        //first call
        if (token.isEmpty()) {
            login();
        } else {
            if (!refreshToken()) {
                token = "";
                login();
            }
        }

        if (token.isEmpty())
            return;

        try {
            @Nullable
            String wallet = getWallet();

            @Nullable
            String statistics = getStatistics();

            @Nullable
            String weekly = getWeeklyStatistics();
            if (wallet == null || statistics == null || weekly == null) {
                return;
            }

            logger.trace("Wallet: {}", wallet);
            logger.trace("Statistis: {}", statistics);
            logger.trace("Weekly: {}", weekly);

            ZonkyWalletResponse walletResponse = gson.fromJson(wallet, ZonkyWalletResponse.class);
            ZonkyStatResponse statResponse = gson.fromJson(statistics, ZonkyStatResponse.class);
            ZonkyWeeklyStatResponse weeklyResponse = gson.fromJson(weekly, ZonkyWeeklyStatResponse.class);

            updateChannelStates(walletResponse, statResponse, weeklyResponse);
        } catch (Exception ex) {
            logger.error("Got exception furing refresh", ex);
        }
    }

    private void updateChannelStates(ZonkyWalletResponse walletResponse, ZonkyStatResponse statResponse, ZonkyWeeklyStatResponse weeklyResponse) {
        for (Channel c : thing.getChannels()) {
            State newValue = null;
            String name = c.getUID().getId();
            logger.debug("Channel name: {}", name);
            if (isWalletType(name)) {
                Number value = getWalletValue(walletResponse, name);
                newValue = new DecimalType(value.doubleValue());
            } else if (isWeeklyStatiscticsType(name)) {
                Number value = getWeeklyStatValue(weeklyResponse, name);
                newValue = new DecimalType(value.doubleValue());
            } else if (isStatisticsType(name)) {
                Number value = name.replace(STATISTICS, "").equals(CURRENT_PROFITABILITY) ? statResponse.getCurrentProfitability() : statResponse.getExpectedProfitability();
                newValue = new DecimalType(value.doubleValue() * 100);
            } else if (isCurrentOverviewType(name)) {
                ZonkyCurrentOverview currentOverview = statResponse.getCurrentOverview();
                Number value = getStatCurrentOverviewValue(currentOverview, name);
                newValue = new DecimalType(value.doubleValue());
            } else if (isOverallOverviewType(name)) {
                ZonkyOverallOverview overallOverview = statResponse.getOverallOverview();
                Number value = getStatOverallOverviewValue(overallOverview, name);
                newValue = new DecimalType(value.doubleValue());
            } else {
                logger.debug("Unknown channel found: {}", name);
            }

            if (newValue != null) {
                updateState(c.getUID(), newValue);
            }
        }
    }

    @Nullable
    private String getWallet() {
        return sendJsonRequest("users/me/wallet");
    }

    @Nullable
    private String getStatistics() {
        return sendJsonRequest("statistics/overview");
    }

    @Nullable
    private String getWeeklyStatistics() {
        return sendJsonRequest("statistics/weekly-statistics");
    }

    @Nullable
    private String sendJsonRequest(String uri) {
        String url;

        logger.debug("sending uri request: {}", uri);
        try {
            //login
            url = ZONKY_URL + uri;

            ContentResponse content = sendRequestBuilder(url, HttpMethod.GET)
                    .header("Authorization", "Bearer " + token)
                    .send();

            return content.getContentAsString();
        } catch (JsonSyntaxException e) {
            logger.debug("Received invalid data", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Received invalid data");
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.debug("Cannot get login cookie!", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot get login cookie");
        }
        return null;
    }

    private Number getWalletValue(ZonkyWalletResponse wallet, String type) {
        switch (type.replace(WALLET, "")) {
            case BALANCE:
                return wallet.getBalance();
            case AVAILABLE_BALANCE:
                return wallet.getAvailableBalance();
            case BLOCKED_BALANCE:
                return wallet.getBlockedBalance();
            case CREDIT_SUM:
                return wallet.getCreditSum();
            case DEBIT_SUM:
                return wallet.getDebitSum();
        }
        return 0d;
    }

    private boolean isStatisticsType(String type) {
        return type.startsWith(STATISTICS);
    }

    private boolean isCurrentOverviewType(String type) {
        return type.startsWith(CURRENT_OVERVIEW);
    }

    private boolean isOverallOverviewType(String type) {
        return type.startsWith(OVERALL_OVERVIEW);
    }

    private boolean isWalletType(String type) {
        return type.startsWith(WALLET);
    }

    private boolean isWeeklyStatiscticsType(String type) {
        return type.startsWith(WEEKLY_STATS);
    }

    private Number getStatOverallOverviewValue(ZonkyOverallOverview overallOverview, String type) {
        switch (type.replace(OVERALL_OVERVIEW, "")) {
            case FEES_AMOUNT:
                return overallOverview.getFeesAmount();
            case FEES_DISCOUNT:
                return overallOverview.getFeesDiscount();
            case INTEREST_PAID:
                return overallOverview.getInterestPaid();
            case INVESTMENT_COUNT:
                return overallOverview.getInvestmentCount();
            case NET_INCOME:
                return overallOverview.getNetIncome();
            case PRINCIPAL_LOST:
                return overallOverview.getPrincipalLost();
            case PRINCIPAL_PAID:
                return overallOverview.getPrincipalPaid();
            case PENALTY_PAID:
                return overallOverview.getPenaltyPaid();
            case TOTAL_INVESTMENT:
                return overallOverview.getTotalInvestment();
        }
        return 0d;
    }

    private Number getStatCurrentOverviewValue(ZonkyCurrentOverview currentOverview, String type) {
        switch (type.replace(CURRENT_OVERVIEW, "")) {
            case INTEREST_LEFT:
                return currentOverview.getInterestLeft();
            case INTEREST_LEFT_DUE:
                return currentOverview.getInterestLeftDue();
            case INTEREST_LEFT_TO_PAY:
                return currentOverview.getInterestLeftToPay();
            case INTEREST_PAID:
                return currentOverview.getInterestPaid();
            case PENALTY_PAID:
                return currentOverview.getPenaltyPaid();
            case INTEREST_PLANNED:
                return currentOverview.getInterestPlanned();
            case INVESTMENT_COUNT:
                return currentOverview.getInvestmentCount();
            case PRINCIPAL_LEFT:
                return currentOverview.getPrincipalLeft();
            case PRINCIPAL_LEFT_DUE:
                return currentOverview.getPrincipalLeftDue();
            case PRINCIPAL_LEFT_TO_PAY:
                return currentOverview.getPrincipalLeftToPay();
            case PRINCIPAL_PAID:
                return currentOverview.getPrincipalPaid();
            case TOTAL_INVESTMENT:
                return currentOverview.getTotalInvestment();
        }
        return 0d;
    }

    private Number getWeeklyStatValue(ZonkyWeeklyStatResponse weekly, String type) {
        switch (type.replace(WEEKLY_STATS, "")) {
            case NEW_INVESTMENTS:
                return weekly.getNewInvestments();
            case NEW_INVESTMENTS_AMOUNT:
                return weekly.getNewInvestmentsAmount();
            case PAID_INSTALMENTS:
                return weekly.getPaidInstalments();
            case PAID_INSTALMENTS_AMOUNT:
                return weekly.getPaidInstalmentsAmount();
            case SOLD_INVESTMENTS:
                return weekly.getSoldInvestments();
            case SOLD_INVESTMENTS_AMOUNT:
                return weekly.getSoldInvestmentsAmount();
            case BOUGHT_INVESTMENTS:
                return weekly.getBoughtInvestments();
            case BOUGHT_INVESTMENTS_AMOUNT:
                return weekly.getBoughtInvestmentsAmount();
        }
        return 0d;
    }
}
