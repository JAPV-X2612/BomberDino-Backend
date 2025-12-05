package com.arsw.bomberdino.controller.rest.v1;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthControllerTest {

    private final AuthController controller = new AuthController();

    @Test
    void getCurrentUserReturnsAuthenticatedInfo() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-id")
                .claim("preferred_username", "user@example.com")
                .claim("name", "Test User")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        ResponseEntity<Map<String, Object>> response = controller.getCurrentUser(jwt);

        assertEquals(200, response.getStatusCode().value());
        assertTrue((Boolean) response.getBody().get("authenticated"));
        assertEquals("user-id", response.getBody().get("id"));
        assertEquals("user@example.com", response.getBody().get("email"));
        assertEquals("Test User", response.getBody().get("name"));
    }

    @Test
    void getCurrentUserReturnsUnauthenticatedWhenNoJwt() {
        ResponseEntity<Map<String, Object>> response = controller.getCurrentUser(null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(false, response.getBody().get("authenticated"));
    }

    @Test
    void getStatusReturnsOnline() {
        ResponseEntity<Map<String, String>> response = controller.getStatus();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("online", response.getBody().get("status"));
    }
}
