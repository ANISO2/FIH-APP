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
