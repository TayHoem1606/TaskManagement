package org.example.tay.taskmanagmentkafkareact.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KeycloakJwtAuthConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    @Override
    public Mono<AbstractAuthenticationToken> convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractRoles(jwt);
        String username = jwt.getClaimAsString("preferred_username");
        log.debug("JWT converted: user={}, authrories={}", username, authorities);
        return Mono.just(new JwtAuthenticationToken(jwt, authorities, username));
    }

    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess =
                jwt.getClaimAsMap("realm_access");    // get realm_access map
        if (realmAccess == null) {
            log.warn("JWT has no realm_access claim - no roles assigned for user:{}",jwt.getClaimAsString("preferred_username"));
            return List.of();
        }

        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof List)) {
            return List.of();
        }
        List<String> roles = (List<String>) rolesObj;

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                // "ADMIN" becomes "ROLE_ADMIN", "USER" becomes "ROLE_USER"
                .collect(Collectors.toList());
    }
}

