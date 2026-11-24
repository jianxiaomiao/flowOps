package com.example.demo.integration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;

@Configuration
class TicketIntegrationSecurity {
    private static final String INGEST = "integration:ticket:ingest";
    private static final Pattern CREDENTIAL = Pattern.compile("Bearer ([A-Za-z0-9_-]{1,64})\\.([A-Za-z0-9_-]{43})");

    @Bean @Order(1)
    SecurityFilterChain ticketIntegrationChain(HttpSecurity http, JdbcTemplate jdbc) throws Exception {
        return http.securityMatcher("/integration/v1/**")
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((req, res, ex) -> res.setStatus(401)))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.POST, "/integration/v1/tickets")
                        .hasAuthority(INGEST).anyRequest().denyAll())
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                                    FilterChain chain) throws ServletException, IOException {
                        authenticate(jdbc, request.getHeader("Authorization"));
                        chain.doFilter(request, response);
                    }
                }, AnonymousAuthenticationFilter.class)
                .build();
    }

    private static void authenticate(JdbcTemplate jdbc, String header) {
        if (header == null || header.length() > 115) return;
        var match = CREDENTIAL.matcher(header);
        if (!match.matches()) return;
        byte[] suppliedDigest = sha256(match.group(2).getBytes(StandardCharsets.US_ASCII));
        jdbc.query("""
                SELECT i.id, i.tenant_id, i.department_id, i.actor_id, i.priority, k.secret_digest
                FROM ticket_integration_key k JOIN ticket_integration i ON i.id = k.integration_id
                JOIN tenant t ON t.id = i.tenant_id
                WHERE k.key_id = ? AND k.enabled = TRUE AND i.enabled = TRUE AND t.status = 'ACTIVE'
                """, rs -> {
            if (rs.next() && MessageDigest.isEqual(suppliedDigest, rs.getBytes("secret_digest"))) {
                var principal = new TicketIntegration(rs.getString("id"), rs.getLong("tenant_id"),
                        rs.getLong("department_id"), rs.getLong("actor_id"), rs.getString("priority"));
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        List.of(new SimpleGrantedAuthority(INGEST))));
                SecurityContextHolder.setContext(context);
            }
            return null;
        }, match.group(1));
    }

    static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
