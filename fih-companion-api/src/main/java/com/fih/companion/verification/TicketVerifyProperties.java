package com.fih.companion.verification;

import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * Config under {@code fih.ticket-verify} for the external ticket-verification
 * web service (festivaldehammamet.com). Kept separate from the older, unused
 * {@code fih.voucher-service} scaffolding: that one is a GET voucher-info
 * lookup; this one is a POST {@code {qrcode, product_id}} verify with a
 * different response contract.
 */
@ConfigurationProperties(prefix = "fih.ticket-verify")
public class TicketVerifyProperties {

    /** Full endpoint, e.g. https://festivaldehammamet.com/ticket/verify */
    private String baseUrl = "";

    /** Sent as the {@code X-Api-Key} header. Externalize via env; never commit a live key. */
    private String apiKey = "";

    /** Connect timeout in ms. Short so a slow service never hangs a scan. */
    private int connectTimeoutMs = 2000;

    /** Read timeout in ms. */
    private int readTimeoutMs = 4000;

    /**
     * Catalogue endpoint listing the ongoing ("en cours") products, e.g.
     * https://festivaldehammamet.com/api/events. Left blank it is derived from
     * {@link #baseUrl} by swapping the {@code ticket/verify} path for {@code events},
     * so a single {@code base-url} keeps working out of the box.
     */
    private String eventsUrl = "";

    /** How long the ongoing-products list is cached, in ms (products don't change mid-scan). */
    private long eventsCacheTtlMs = 300_000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    /**
     * Resolved events URL: the explicit {@code events-url} when set, otherwise
     * derived from {@code base-url} (…/ticket/verify → …/events).
     */
    public String getEventsUrl() {
        if (eventsUrl != null && !eventsUrl.isBlank()) {
            return eventsUrl;
        }
        if (baseUrl != null && baseUrl.contains("ticket/verify")) {
            return baseUrl.replace("ticket/verify", "events");
        }
        return eventsUrl;
    }

    public void setEventsUrl(String eventsUrl) {
        this.eventsUrl = eventsUrl;
    }

    public long getEventsCacheTtlMs() {
        return eventsCacheTtlMs;
    }

    public void setEventsCacheTtlMs(long eventsCacheTtlMs) {
        this.eventsCacheTtlMs = eventsCacheTtlMs;
    }

    /** True only when a non-blank base-url has been configured. */
    public boolean isEnabled() {
        return baseUrl != null && !baseUrl.isBlank();
    }
}
