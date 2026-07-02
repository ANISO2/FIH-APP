package com.fih.companion.security;

import com.fih.companion.diagnostics.ConsoleLog;
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


@Component
public class DeviceTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DeviceTokenFilter.class);
    private static final String TAG = "DEVICE-AUTH";
    private static final String HEADER = "X-Device-Token";

    private final SecurityProperties properties;

    public DeviceTokenFilter(SecurityProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void logExpectedToken() {
        log.info("[device-auth] ready — expected {} = {}. The app must send the SAME value "
                        + "(app default 'dev-device-token', or --dart-define=FIH_DEVICE_TOKEN=...).",
                HEADER, ConsoleLog.mask(expected()));
        // Feature 1 — also emit the always-on console trace line.
        ConsoleLog.log(TAG, "ready — expected " + HEADER + "=" + ConsoleLog.mask(expected())
                + " (the mobile app must send the SAME value).");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String endpoint = request.getMethod() + " " + request.getRequestURI();
        String raw = request.getHeader(HEADER);
        String received = raw == null ? null : raw.trim();
        String expected = expected();
        boolean alreadyAuthed = SecurityContextHolder.getContext().getAuthentication() != null;

        if (received == null || received.isEmpty()) {
            ConsoleLog.log(TAG, "no " + HEADER + " header on " + endpoint
                    + " — skipping device auth (this is normal for the Angular backoffice, which uses a JWT).");
        } else if (alreadyAuthed) {
            ConsoleLog.log(TAG, HEADER + " present on " + endpoint
                    + " but request is ALREADY authenticated (JWT ran first) — device auth skipped.");
        } else if (received.equals(expected)) {
            var auth = new UsernamePasswordAuthenticationToken(
                    "mobile-device", null,
                    List.of(new SimpleGrantedAuthority("ROLE_DEVICE")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            ConsoleLog.log(TAG, "DECISION=AUTHENTICATED on " + endpoint
                    + " — received=" + ConsoleLog.mask(received) + " matches expected"
                    + ", granted authority=ROLE_DEVICE.");
        } else {
            // Header WAS sent but did not match — the #1 cause of the 401.
            log.warn("[device-auth] token mismatch on {} — received {} but expected {}.",
                    endpoint, ConsoleLog.mask(received), ConsoleLog.mask(expected));
            ConsoleLog.log(TAG, "DECISION=REJECTED on " + endpoint
                    + " — received=" + ConsoleLog.mask(received) + " but expected=" + ConsoleLog.mask(expected)
                    + ". reason=token mismatch. Align the app's FIH_DEVICE_TOKEN with the server's "
                    + "fih.security.device-token. (Staying anonymous — the endpoint will 401 if it needs a role.)");
        }

        chain.doFilter(request, response);
    }

    private String expected() {
        String t = properties.getDeviceToken();
        return t == null ? "" : t.trim();
    }
}
