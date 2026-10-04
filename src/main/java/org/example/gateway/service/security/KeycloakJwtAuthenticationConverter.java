package org.example.gateway.service.security;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        Map<String, Object> realmAccess =
                jwt.getClaim("realm_access");

        List<String> roles = List.of();

        if (realmAccess != null) {
            Object rolesObject = realmAccess.get("roles");

            if (rolesObject instanceof Collection<?> collection) {
                roles = collection.stream()
                        .map(Object::toString)
                        .toList();
            }
        }

        var authorities = roles.stream()
                .map(role ->
                        new SimpleGrantedAuthority(
                                "ROLE_" + role.toUpperCase()
                        )
                )
                .toList();

        return new JwtAuthenticationToken(
                jwt,
                authorities,
                jwt.getClaimAsString("preferred_username")
        );
    }
}
