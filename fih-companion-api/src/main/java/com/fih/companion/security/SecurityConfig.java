package com.fih.companion.security;

import com.fih.companion.diagnostics.ConsoleLog;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;


@Configuration
@EnableWebSecurity
public class SecurityConfig {


    private static final String[] MOBILE_STATS_GET = {
            "/api/stats/years",
            "/api/stats/overview",
            "/api/stats/entries-by-day",
            "/api/stats/gate",
            "/api/stats/ticket-types",
            "/api/stats/tourniquets",
            "/api/stats/rejets"
    };


    @Value("${FIH_CORS_ALLOWED_ORIGINS:*}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthFilter jwtAuthFilter,
                                           DeviceTokenFilter deviceTokenFilter,
                                           CorsLoggingFilter corsLoggingFilter) throws Exception {
        ConsoleLog.log("SECURITY", "building filter chain — order: CorsLoggingFilter -> Spring CORS "
                + "-> DeviceTokenFilter -> JwtAuthFilter -> authorization rules. "
                + "Public: OPTIONS/**, /api/auth/login, /api/events/**, /api/diagnostics/**. "
                + "DEVICE or ADMIN: /api/verify/**, GET mobile stats. "
                + "ADMIN or INVITATIONS: /api/badges/**, /api/invitations/** (Invitations & Badges section). "
                + "ADMIN only: /api/stats/** (overview, recette, verification, ...).");
        http
                // Enable CORS using the bean below.
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Let every CORS preflight through without auth. A browser sends an
                        // OPTIONS request WITHOUT the Authorization header before the real call;
                        // if it is not permitted, the whole request is blocked and the user
                        // appears to be "logged out" / the page just errors.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/events/**", "/api/diagnostics/**").permitAll()
                        .requestMatchers("/api/verify/**").hasAnyRole("DEVICE", "ADMIN")
                        // [ADDED] mobile dashboard: device OR admin may read these global feeds (GET only).
                        // Placed BEFORE the broad /api/stats/** rule so it is not shadowed.
                        .requestMatchers(HttpMethod.GET, MOBILE_STATS_GET).hasAnyRole("DEVICE", "ADMIN")
                        // Feature 1 — the Invitations & Badges section: ADMIN or the restricted
                        // INVITATIONS role. This is the ONLY area the restricted account may reach.
                        .requestMatchers("/api/badges/**").hasAnyRole("ADMIN", "INVITATIONS")
                        // The only write endpoints in the app (badge name): ADMIN or INVITATIONS.
                        .requestMatchers("/api/invitations/**").hasAnyRole("ADMIN", "INVITATIONS")
                        // Everything else under stats (overview, recette/**, verification/**, ...):
                        // ADMIN only — the INVITATIONS role is rejected here even if called directly.
                        .requestMatchers("/api/stats/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
                .addFilterBefore(deviceTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // Feature 1 — trace the incoming Origin / preflight before anything else runs.
                .addFilterBefore(corsLoggingFilter, org.springframework.web.filter.CorsFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (origins.size() == 1 && "*".equals(origins.get(0))) {
            // Wildcard: allow any origin. With credentials off this is fine for token-in-header auth.
            cfg.addAllowedOriginPattern("*");
            ConsoleLog.log("CORS", "resolved allowed-origins = * (WILDCARD, any origin). "
                    + "Convenient for testing; pin FIH_CORS_ALLOWED_ORIGINS to the real frontend URL in production.");
        } else {
            cfg.setAllowedOrigins(origins);
            ConsoleLog.log("CORS", "resolved allowed-origins = " + origins
                    + " — any browser Origin NOT in this list will be blocked.");
        }

        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));           // includes Authorization and X-Device-Token
        cfg.setExposedHeaders(List.of("Content-Disposition"));
        cfg.setAllowCredentials(false);                // we use a Bearer token, not cookies
        cfg.setMaxAge(3600L);                          // cache preflight for 1h

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}