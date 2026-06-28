package com.fih.companion.verification;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "fih.voucher-service")
public class VoucherServiceProperties {


    private String baseUrl = "";

    /** Optional API key. When set, sent as the {@code X-Api-Key} header. */
    private String apiKey = "";

    /** Connect timeout in milliseconds. Kept short so a slow service never hangs a scan. */
    private int connectTimeoutMs = 2000;

    /** Read timeout in milliseconds. */
    private int readTimeoutMs = 3000;

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

    /** True only when a non-blank base-url has been configured. */
    public boolean isEnabled() {
        return baseUrl != null && !baseUrl.isBlank();
    }
}
