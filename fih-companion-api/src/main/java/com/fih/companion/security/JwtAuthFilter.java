package com.fih.companion.security;

import com.fih.companion.diagnostics.ConsoleLog;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String TAG = "JWT-FILTER";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String endpoint = request.getMethod() + " " + request.getRequestURI();
        String header = request.getHeader("Authorization");

        if (header == null) {
            ConsoleLog.log(TAG, "no Authorization header on " + endpoint
                    + " — leaving request ANONYMOUS (a device-token or a public route may still allow it).");
        } else if (!header.startsWith("Bearer ")) {
            ConsoleLog.log(TAG, "Authorization header present on " + endpoint
                    + " but not a 'Bearer ' token (starts with '"
                    + header.substring(0, Math.min(8, header.length())) + "\u2026') — IGNORED, staying anonymous.");
        } else {
            String token = header.substring(7);
            ConsoleLog.log(TAG, "Bearer token found on " + endpoint
                    + " (length=" + token.length() + ") — validating\u2026");
            try {
                Claims claims = jwtService.parse(token);
                String subject = claims.getSubject();

                Object roleClaim = claims.get("role");
                String authority = Roles.INVITATIONS_CLAIM.equals(roleClaim)
                        ? "ROLE_" + Roles.INVITATIONS
                        : "ROLE_" + Roles.ADMIN;
                var auth = new UsernamePasswordAuthenticationToken(
                        subject, null,
                        List.of(new SimpleGrantedAuthority(authority)));
                SecurityContextHolder.getContext().setAuthentication(auth);
                ConsoleLog.log(TAG, "DECISION=AUTHENTICATED on " + endpoint
                        + " — subject=" + subject + ", granted authority=" + authority
                        + ", reason=signature valid and token not expired.");
            } catch (Exception ex) {

                ConsoleLog.error(TAG, "DECISION=REJECTED on " + endpoint
                        + " — token rejected, staying ANONYMOUS. reason="
                        + ex.getClass().getSimpleName() + ": " + ex.getMessage()
                        + ". (A '...signature does not match...' here across two servers means the "
                        + "FIH_JWT_SECRET differs between them.)", ex);
            }
        }

        chain.doFilter(request, response);
    }
}
