package com.fih.companion.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Minimal device auth for the mobile verifier: if the "X-Device-Token" header
 * matches the configured shared token, grants ROLE_DEVICE.
 *
 * Hardened for the "jeton appareil invalide" (401) class of bugs:
 *  - Both the incoming header and the configured token are TRIMMED, so a stray
 *    space/newline (very common when the token comes from an env var, an IDE run
 *    config, or --dart-define) no longer causes a silent mismatch.
 *  - On startup the EXPECTED token is logged (masked), so you can confirm what
 *    the server wants.
 *  - On a mismatch the received vs expected are logged (masked), so a 401 tells
 *    you exactly why instead of being silent.
 * Masking keeps the secret out of the logs: only the first 3 chars + length.
 */
@Component
public class DeviceTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DeviceTokenFilter.class);
    private static final String HEADER = "X-Device-Token";

    private final SecurityProperties properties;

    public DeviceTokenFilter(SecurityProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void logExpectedToken() {
        log.info("[device-auth] ready — expected {} = {}. The app must send the SAME value "
                        + "(app default 'dev-device-token', or --dart-define=FIH_DEVICE_TOKEN=...).",
                HEADER, mask(expected()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String raw = request.getHeader(HEADER);
        String received = raw == null ? null : raw.trim();
        String expected = expected();

        if (received != null && !received.isEmpty()
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (received.equals(expected)) {
                var auth = new UsernamePasswordAuthenticationToken(
                        "mobile-device", null,
                        List.of(new SimpleGrantedAuthority("ROLE_DEVICE")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                // Header WAS sent but did not match — the #1 cause of the 401.
                log.warn("[device-auth] token mismatch on {} {} — received {} but expected {}. "
                                + "Align the app's FIH_DEVICE_TOKEN with the server's fih.security.device-token.",
                        request.getMethod(), request.getRequestURI(), mask(received), mask(expected));
            }
        }
        chain.doFilter(request, response);
    }

    private String expected() {
        String t = properties.getDeviceToken();
        return t == null ? "" : t.trim();
    }

    /** e.g. "dev…(len=16)" — enough to compare, safe to log. */
    private static String mask(String s) {
        if (s == null || s.isEmpty()) return "<empty>";
        return s.substring(0, Math.min(3, s.length())) + "…(len=" + s.length() + ")";
    }
}