package org.example.tay.taskmanagmentkafkareact.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final KeycloakJwtAuthConverter keycloakJwtAuthConverter;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // ── Session: stateless — JWT carries all auth info ──────────
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

                // ── CSRF: disabled — SPA + JWT, no cookie sessions ──────────
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // ── CORS: allow React frontend origin ────────────────────────
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── Endpoint authorization rules ─────────────────────────────
                .authorizeExchange(auth -> auth
                        // Health/actuator paths — allow without token (optional)
                        .pathMatchers("/actuator/health").permitAll()

                        // All task endpoints require a valid JWT (any role)
                        // Fine-grained ownership checks happen in TaskServiceImpl
                        .pathMatchers(HttpMethod.GET,    "/api/tasks").hasAnyRole("ADMIN", "USER")
                        .pathMatchers(HttpMethod.GET,    "/api/tasks/**").hasAnyRole("ADMIN", "USER")
                        .pathMatchers(HttpMethod.POST,   "/api/tasks").hasAnyRole("ADMIN", "USER")
                        .pathMatchers(HttpMethod.PUT,    "/api/tasks/**").hasAnyRole("ADMIN", "USER")
                        .pathMatchers(HttpMethod.PATCH,  "/api/tasks/**").hasAnyRole("ADMIN", "USER")
                        .pathMatchers(HttpMethod.DELETE, "/api/tasks/**").hasAnyRole("ADMIN", "USER")

                        // Everything else requires authentication
                        .anyExchange().authenticated()
                )

                // ── JWT Resource Server ──────────────────────────────────────
                // Spring validates the Bearer token from Authorization header,
                // verifies the signature against Keycloak's JWKS endpoint,
                // then calls keycloakJwtAuthConverter to build the Authentication.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthConverter))
                        // Return 401 JSON (not redirect) when token is missing/expired
                        .authenticationEntryPoint((exchange, ex) -> {
                            log.warn("Unauthorized access attempt on {}: {}",
                                    exchange.getRequest().getPath().value(), ex.getMessage());
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                )
                // ── Access denied handler: return 403 JSON ───────────────────
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(
                                new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN))
                )

                .build();
    }

    /**
     * CORS configuration.
     * AllowedMethods: all HTTP methods the frontend needs.
     * AllowedHeaders: "*" — allow Authorization, Content-Type, etc.
     * AllowCredentials: true — required for keycloak-js cookie-based silent SSO.
     * MaxAge: 3600 — browsers cache the preflight response for 1 hour.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Parse comma-separated origins from property
        List<String> origins = List.of(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

