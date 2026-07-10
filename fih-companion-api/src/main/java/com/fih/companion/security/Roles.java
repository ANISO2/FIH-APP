package com.fih.companion.security;


public final class Roles {

    private Roles() {
    }

    /** Claim value put in the JWT for the restricted "Invitations &amp; Badges only" account. */
    public static final String INVITATIONS_CLAIM = "INVITATIONS";

    /** Spring role names (without the {@code ROLE_} prefix, as used by hasRole/hasAnyRole). */
    public static final String ADMIN = "ADMIN";
    public static final String INVITATIONS = "INVITATIONS";
}
