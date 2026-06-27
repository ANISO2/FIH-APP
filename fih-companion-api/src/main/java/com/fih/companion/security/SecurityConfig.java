package com.fih.companion.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless security.
 *
 *  - POST /api/auth/login              : public
 *  - GET  /api/events, /diagnostics    : public (Phase 1 endpoints)
 *  - /api/verify/**                    : ROLE_DEVICE or ROLE_ADMIN (mobile app)
 *  - GET global-stats read endpoints   : ROLE_DEVICE or ROLE_ADMIN (mobile dashboard) [ADDED]
 *  - /api/stats/**, /api/badges/**     : ROLE_ADMIN (everything else under stats stays admin-only)
 *  - /api/invitations/**               : ROLE_ADMIN (the only write endpoints)
 *  - anything else                     : authenticated
 *
 * ---------------------------------------------------------------------------
 * MOBILE-APP CHANGE (verifier live stats) — surgical, backoffice unaffected.
 * ---------------------------------------------------------------------------
 * The mobile verifier needs the GLOBAL read-only stats so an operator/manager
 * can see live totals on the phone without opening the backoffice. Those
 * endpoints live under the SHARED prefix /api/stats/**, which is otherwise
 * ADMIN-only and used by the backoffice. We did NOT blanket-open /api/stats/**.
 *
 * Instead we added ONE matcher, placed BEFORE the existing /api/stats/** rule,
 * that additionally grants ROLE_DEVICE on EXACTLY the seven GET endpoints the
 * mobile reads. Ordering matters: the more specific matcher must come first,
 * otherwise the broad /api/stats/** ADMIN rule would shadow it.
 *
 * Why the backoffice is provably unaffected:
 *  - No route was narrowed or removed; we only WIDENED seven specific GET paths
 *    to also allow ROLE_DEVICE.
 *  - The backoffice authenticates as ROLE_ADMIN (JWT). hasAnyRole("DEVICE","ADMIN")
 *    still passes for ADMIN, so those seven endpoints behave for the backoffice
 *    exactly as before.
 *  - Everything sensitive stays ADMIN-only and is NOT in the added list:
 *    /api/stats/recette/**, /api/stats/events, /api/badges/**, /api/invitations/**.
 *  - The mobile reaches these seven only by sending a valid X-Device-Token
 *    (see DeviceTokenFilter); it still cannot reach recette, events, badges or
 *    invitations.
 *
 * The mobile sends the shared X-Device-Token header on every call, so the
 * operator never logs in (no username/password, no per-user token) while the
 * endpoints are not left world-readable.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Global read-only stats the MOBILE app is allowed to read (GET only).
     * Keep this list tight: only the dashboard's global feeds. Anything not
     * listed here stays ADMIN-only via the /api/stats/** rule below.
     */
    private static final String[] MOBILE_STATS_GET = {
            "/api/stats/years",
            "/api/stats/overview",
            "/api/stats/entries-by-day",
            "/api/stats/gate",
            "/api/stats/ticket-types",
            "/api/stats/tourniquets",
            "/api/stats/rejets"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthFilter jwtAuthFilter,
                                           DeviceTokenFilter deviceTokenFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/events/**", "/api/diagnostics/**").permitAll()
                        .requestMatchers("/api/verify/**").hasAnyRole("DEVICE", "ADMIN")
                        // [ADDED] mobile dashboard: device OR admin may read these global feeds (GET only).
                        // Placed BEFORE the broad /api/stats/** rule so it is not shadowed.
                        .requestMatchers(HttpMethod.GET, MOBILE_STATS_GET).hasAnyRole("DEVICE", "ADMIN")
                        // Everything else under stats (recette/**, events, ...) and all of badges: ADMIN only — unchanged.
                        .requestMatchers("/api/stats/**", "/api/badges/**").hasRole("ADMIN")
                        // The only write endpoints in the app (badge name). Admin only.
                        .requestMatchers("/api/invitations/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
                .addFilterBefore(deviceTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
