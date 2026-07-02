package com.fih.companion.security;

import com.fih.companion.diagnostics.ConsoleLog;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsLoggingFilter extends OncePerRequestFilter {

    private static final String TAG = "CORS";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String endpoint = request.getMethod() + " " + request.getRequestURI();
        String origin = request.getHeader("Origin");

        if (origin == null) {
            // Same-origin / non-browser (curl, mobile app): CORS does not apply.
            ConsoleLog.log(TAG, "no Origin header on " + endpoint
                    + " — same-origin or non-browser client, CORS not involved.");
        } else {
            String preflightMethod = request.getHeader("Access-Control-Request-Method");
            boolean preflight = "OPTIONS".equalsIgnoreCase(request.getMethod()) && preflightMethod != null;
            ConsoleLog.log(TAG, (preflight ? "PREFLIGHT " : "cross-origin ") + endpoint
                    + " — Origin=" + origin
                    + (preflight ? ", requested method=" + preflightMethod : "")
                    + " — checking against fih.cors.allowed-origins (FIH_CORS_ALLOWED_ORIGINS)\u2026");
        }

        chain.doFilter(request, response);

        if (origin != null) {
            String allowOrigin = response.getHeader("Access-Control-Allow-Origin");
            if (allowOrigin != null) {
                ConsoleLog.log(TAG, "DECISION=ALLOWED on " + endpoint
                        + " — Origin=" + origin + " accepted (Access-Control-Allow-Origin=" + allowOrigin
                        + ", HTTP status=" + response.getStatus() + ").");
            } else {
                ConsoleLog.log(TAG, "DECISION=NOT ALLOWED on " + endpoint
                        + " — Origin=" + origin + " produced NO Access-Control-Allow-Origin header"
                        + " (HTTP status=" + response.getStatus() + "). The browser will block the response."
                        + " reason=origin not in the allowed list; set FIH_CORS_ALLOWED_ORIGINS to include it.");
            }
        }
    }
}
