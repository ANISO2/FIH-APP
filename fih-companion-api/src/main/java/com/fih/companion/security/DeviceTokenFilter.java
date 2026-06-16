package com.fih.companion.security;

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

/**
 * Minimal device auth for the mobile verifier: if the "X-Device-Token" header
 * matches the configured shared token, grants ROLE_DEVICE. We can harden this
 * later (per-device tokens, rotation, etc.).
 */
@Component
public class DeviceTokenFilter extends OncePerRequestFilter {

    private final SecurityProperties properties;

    public DeviceTokenFilter(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = request.getHeader("X-Device-Token");
        if (token != null && token.equals(properties.getDeviceToken())
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            var auth = new UsernamePasswordAuthenticationToken(
                    "mobile-device", null,
                    List.of(new SimpleGrantedAuthority("ROLE_DEVICE")));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}
