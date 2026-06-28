package com.fih.companion.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

 @ConfigurationProperties(prefix = "fih.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    /** Shared secret the mobile app sends in the X-Device-Token header. */
    private String deviceToken = "dev-device-token";

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public static class Jwt {
        /** HS256 secret; must be at least 32 characters. */
        private String secret = "change-me-to-a-long-random-secret-of-32+chars!!";
        private long expirationMinutes = 480;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(long expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
    }
}
