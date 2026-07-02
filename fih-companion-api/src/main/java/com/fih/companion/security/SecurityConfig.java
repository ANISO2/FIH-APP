package com.fih.companion.security;

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

    /**
     * Comma-separated list of origins allowed to call the API from a browser.
     * Set FIH_CORS_ALLOWED_ORIGINS in production to your frontend URL, e.g.
     *   FIH_CORS_ALLOWED_ORIGINS=https://fih-admin.example.tn
     * Multiple origins are allowed: "https://a.tn,https://b.tn".
     * Default "*" is convenient for testing but you should pin it in prod.
     */
    @Value("${FIH_CORS_ALLOWED_ORIGINS:*}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthFilter jwtAuthFilter,
                                           DeviceTokenFilter deviceTokenFilter) throws Exception {
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
        } else {
            cfg.setAllowedOrigins(origins);
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