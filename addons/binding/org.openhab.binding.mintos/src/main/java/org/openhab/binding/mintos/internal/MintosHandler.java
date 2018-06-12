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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.smarthome.core.cache.ExpiringCache;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.openhab.binding.mintos.internal.MintosBindingConstants.*;

/**
 * The {@link MintosHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Ondrej Pecta - Initial contribution
 */
@NonNullByDefault
public class MintosHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(MintosHandler.class);

    @Nullable
    private MintosConfiguration config;

    private ExpiringCache<MintosAccountOverview> accountOverview;

    private SslContextFactory sslContext = new SslContextFactory();

    private HttpClient httpClient;

    private String csrf = "";
    private String logoutURL = "";

    //Future
    @Nullable
    ScheduledFuture<?> future;

    public MintosHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command.equals(RefreshType.REFRESH)) {
            //updateChannelState(channelUID.getId());
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(MintosConfiguration.class);

        if (config != null && !config.login.isEmpty() && !config.password.isEmpty() ) {
            accountOverview = new ExpiringCache<>(10000, this::getOverview);
            startPolling();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (future != null && !future.isCancelled()) {
            future.cancel(true);
        }
    }

    private void startPolling() {
        future = scheduler.scheduleWithFixedDelay(this::updateState, 0, config.refreshInterval, TimeUnit.MINUTES);
    }

    private void updateState() {
        httpClient = new HttpClient(sslContext);
        try {
            httpClient.start();
        } catch (Exception e) {
            logger.error("Cannot start http client!", e);
            return;
        }
        if (httpClient.isRunning()) {
            updateChannelStates(accountOverview.getValue());
            logout();
        }
    }

    private void updateChannelState(String channel) {
        MintosAccountOverview overview = accountOverview.getValue();
        if (overview == null) {
            logger.error("Channel {} not updated because od null overview", overview);
            return;
        }
        State state = null;
        switch (channel) {
            case ACCOUNT_BALANCE:
                state = new DecimalType(overview.getAccountBalance());
                break;
            case AVAILABLE_FUNDS:
                state = new DecimalType(overview.getAvailableFunds());
                break;
            case INVESTED_FUNDS:
                state = new DecimalType(overview.getInvestedFunds());
                break;
            case NET_ANNUAL_RETURN:
                state = new DecimalType(overview.getNetAnnualReturn());
                break;
            case INTEREST:
                state = new DecimalType(overview.getInterest());
                break;
            case LATE_PAYMENT_FEES:
                state = new DecimalType(overview.getLatePaymentFees());
                break;
            case BAD_DEBT:
                state = new DecimalType(overview.getBadDebt());
                break;
            case SEC_MARKET_TRANSACTIONS:
                state = new DecimalType(overview.getSecMarketTransactions());
                break;
            case CAMPAIGN_REWARDS:
                state = new DecimalType(overview.getCampaignRewards());
                break;
            case SERVICE_FEES:
                state = new DecimalType(overview.getServiceFees());
                break;
            case TOTAL_PROFIT:
                state = new DecimalType(overview.getTotalProfit());
                break;
            default:
                logger.error("unknown channel: {}", channel);
        }
        if (state != null) {
            updateState(channel, state);
        }
    }

    private void updateChannelStates(MintosAccountOverview overview) {
        updateState(ACCOUNT_BALANCE, new DecimalType(overview.getAccountBalance()));
        updateState(AVAILABLE_FUNDS, new DecimalType(overview.getAvailableFunds()));
        updateState(INVESTED_FUNDS, new DecimalType(overview.getInvestedFunds()));
        updateState(NET_ANNUAL_RETURN, new DecimalType(overview.getNetAnnualReturn()));
        updateState(INTEREST, new DecimalType(overview.getInterest()));
        updateState(LATE_PAYMENT_FEES, new DecimalType(overview.getLatePaymentFees()));
        updateState(BAD_DEBT, new DecimalType(overview.getBadDebt()));
        updateState(SEC_MARKET_TRANSACTIONS, new DecimalType(overview.getSecMarketTransactions()));
        updateState(SERVICE_FEES, new DecimalType(overview.getServiceFees()));
        updateState(CAMPAIGN_REWARDS, new DecimalType(overview.getCampaignRewards()));
        updateState(TOTAL_PROFIT, new DecimalType(overview.getTotalProfit()));
    }

    private void logout() {
        try {
            if (!logoutURL.isEmpty()) {
                httpClient.newRequest(logoutURL).method(HttpMethod.GET)
                        .header("referer", "https://www.mintos.com/en/overview")
                        .header(":authority", "www.mintos.com")
                        .header(":method", "POST")
                        .header(":path", logoutURL.replace("https://www.mintos.com", ""))
                        .header(":scheme", "https")
                        .agent(AGENT)
                        .send();
            }
            if (httpClient.isRunning()) {
                httpClient.stop();
            }
            logoutURL = "";
        } catch (Exception e) {
            logger.error("Erorr during logout", e);
        }
    }


    private synchronized MintosAccountOverview getOverview() {
        ContentResponse response;

        try {
            response = httpClient.newRequest("https://www.mintos.com/en")
                    .method(HttpMethod.GET)
                    .header(":authority", "www.mintos.com")
                    .header(":method", "GET")
                    .header(":path", "/en/")
                    .header(":scheme", "https")
                    .agent(AGENT)
                    .send();
            csrf = getCsrfToken(response.getContentAsString());
            logger.debug("Got CSRF token: {}", csrf);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Cannot send GET command to mintos.com!", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot open www.mintos.com");
            return new MintosAccountOverview();
        }

        StringContentProvider content = new StringContentProvider("_csrf_token=" + csrf + "&_username=" + config.login + "&_password=" + config.password);
        try {
            response = httpClient.newRequest("https://www.mintos.com/en/login/check")
                    .method(HttpMethod.POST)
                    .content(content, "application/x-www-form-urlencoded")
                    .header(":authority", "www.mintos.com")
                    .header(":method", "POST")
                    .header(":path", "/en/login/check")
                    .header(":scheme", "https")
                    .header("origin", "https://www.mintos.com")
                    .header("referer", "https://www.mintos.com/en/login")
                    .agent(AGENT)
                    .send();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Cannot login to mintos.com!", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot login to www.mintos.com");
            return new MintosAccountOverview();
        }

        if (response.getStatus() == 200) {
            logger.debug("Got login response with length: {}", response.getContentAsString().length());
            String txt = response.getContentAsString();

            logoutURL = getLogoutUrl(txt);
            logger.debug("logout url: {}", logoutURL);

            int pos = txt.indexOf("bindTabs('");
            while( pos > 0) {
                String currency = MintosCurrencies.getAbbreviation(txt.substring(pos + 10, pos + 10 + 3));
                logger.info("Found currency: {}", currency);
                if (config.currency == null || config.currency.isEmpty() || config.currency.equals(currency.toUpperCase())) {
                    int posEnd = txt.indexOf("Recent News");
                    String subs = txt.substring(pos, posEnd);

                    updateStatus(ThingStatus.ONLINE);
                    return parseOverview(subs);
                }
                //skip "bindTabs"
                txt = txt.substring(pos + 8);
                pos = txt.indexOf("bindTabs('");
            }
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Cannot find currency " + config.currency);
        } else {
            logger.error("Got response code: {} and message: {}", response.getStatus(), response.getContentAsString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Got response code: " + response.getStatus());
        }
        return new MintosAccountOverview();
    }

    private MintosAccountOverview parseOverview(String text) {
        Double balance = getOverviewItemTab("Total", text);
        Double availFunds = getOverviewItemTab("Available Funds", text);
        Double investedFunds = getOverviewItemTab("Invested Funds", text);
        Double percent = getOverviewItem("Net Annual Return", text);
        Double interest = getOverviewItemTab("Interest", text);
        Double latePaymentFees = getOverviewItemTab("Late Payment Fees", text);
        Double badDebt = getOverviewItemTab("Bad Debt", text);
        Double secMarketTrans = getOverviewItemWithToolTip("Secondary Market Transactions", text);
        Double serviceFees = getOverviewItemWithToolTip("Service Fees", text);
        Double campaignRewards = getOverviewItemTab("Campaign Rewards", text);
        Double totalProfit = getOverviewItemTab("Total Profit", text);

        MintosAccountOverview overview = new MintosAccountOverview();
        overview.setAccountBalance(balance);
        overview.setAvailableFunds(availFunds);
        overview.setInvestedFunds(investedFunds);
        overview.setNetAnnualReturn(percent);
        overview.setInterest(interest);
        overview.setLatePaymentFees(latePaymentFees);
        overview.setBadDebt(badDebt);
        overview.setSecMarketTransactions(secMarketTrans);
        overview.setCampaignRewards(campaignRewards);
        overview.setServiceFees(serviceFees);
        overview.setTotalProfit(totalProfit);
        return overview;
    }

    private String getLogoutUrl(String text) {
        int pos = text.indexOf(LOGOUT_URL);
        int posEnd = text.indexOf("\" class=\"logout main-nav-logout");
        return text.substring(pos + 9, posEnd);
    }

    private Double getOverviewItem(String item, String content) {
        int pos = content.indexOf(item);
        String subs = content.substring(pos);
        pos = subs.indexOf(OVERVIEW_INDEX);
        int posEnd = subs.indexOf(END_DIV_INDEX);
        String value = subs.substring(pos + OVERVIEW_INDEX.length(), posEnd).replaceAll("[^\\d.]", "");
        logger.info(item + ": {}", value);

        return Double.parseDouble(value);
    }

    private Double getOverviewItemWithToolTip(String item, String content) {
        int pos = content.indexOf(item);
        String subs = content.substring(pos);
        pos = subs.indexOf(TOOLTIP_INDEX);
        subs = subs.substring(pos + TOOLTIP_INDEX.length());
        int posEnd = subs.indexOf(END_INDEX);
        String value = subs.substring(0, posEnd).replaceAll("[^\\d.]", "");
        logger.info(item + ": {}", value);

        return Double.parseDouble(value);
    }

    private Double getOverviewItemTab(String item, String content) {
        int pos = content.indexOf(item + OVERVIEW_TAB_INDEX);
        String subs = content.substring(pos + item.length() + OVERVIEW_TAB_INDEX.length());
        int posEnd = subs.indexOf(END_INDEX);
        String value = subs.substring(0, posEnd).replaceAll("[^\\d.]", "");
        logger.info(item + ": {}", value);

        return Double.parseDouble(value);
    }

    private String getCsrfToken(String content) {
        final String field = "_csrf_token";
        int pos = content.indexOf(field);
        return content.substring(pos + field.length() + 9, pos + field.length() + 9 + 43);
    }
}
