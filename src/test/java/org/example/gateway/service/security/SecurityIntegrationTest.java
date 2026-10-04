package org.example.gateway.service.security;

import org.example.gateway.service.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "service.security.enabled=true")
@AutoConfigureMockMvc
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private Jwt createJwt(String username, String... roles) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("user-123")
                .issuer("http://localhost:8080/realms/my-realm")
                .audience(List.of("backend-api"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("preferred_username", username)
                .claim("realm_access", Map.of(
                        "roles", List.of(roles)
                ))
                .build();
    }


    private void mockToken(String token, String username, String... roles) {
        when(jwtDecoder.decode(token))
                .thenReturn(Jwt.withTokenValue(token)
                        .header("alg", "RS256")
                        .subject("user-123")
                        .issuer("http://localhost:8080/realms/my-app")
                        .audience(List.of("backend-api"))
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .claim("preferred_username", username)
                        .claim("realm_access", Map.of(
                                "roles", List.of(roles)
                        ))
                        .build());
    }

    @Test
    void shouldReturn401WithoutToken() throws Exception {

        mockMvc.perform(get("/api/v1/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200WithValidToken() throws Exception {
        mockToken("user-token", "john", "ADMIN");

        mockMvc.perform(get("/api/v1/test")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401WithNonAdminToken() throws Exception {
        mockToken("user-token", "jane", "USER");

        mockMvc.perform(get("/api/v1/test")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }
}
