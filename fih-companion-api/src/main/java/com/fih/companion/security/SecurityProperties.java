package com.fih.companion.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fih.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
     private String deviceToken = "dev-device-token";


    private InvitationsAccount invitationsAccount = new InvitationsAccount();

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

    public InvitationsAccount getInvitationsAccount() {
        return invitationsAccount;
    }

    public void setInvitationsAccount(InvitationsAccount invitationsAccount) {
        this.invitationsAccount = invitationsAccount;
    }

    /** Credentials + display name for the restricted invitations-only account. */
    public static class InvitationsAccount {
        /** Login username (may be an e-mail). Blank/unset disables the account. */
        private String username = "";
        /** Plaintext password, matching the rest of this legacy system. */
        private String password = "";
        /** Name shown in the backoffice header for this account. */
        private String displayName = "Invitations & Badges";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

         public boolean isConfigured() {
            return username != null && !username.isBlank()
                    && password != null && !password.isBlank();
        }
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
