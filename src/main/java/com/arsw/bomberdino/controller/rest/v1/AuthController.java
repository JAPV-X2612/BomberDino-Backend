package com.arsw.bomberdino.controller.rest.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for authentication-related endpoints. Handles user information retrieval from JWT tokens.
 *
 * @author Mapunix, Rivaceratops, Yisus-Rex
 * @version 1.0
 * @since 2025-21-15
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /**
     * Returns authenticated user information from JWT claims.
     *
     * @param jwt authenticated user's JWT token
     * @return user information map
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("authenticated", true);
        userInfo.put("id", jwt.getSubject());
        userInfo.put("email", jwt.getClaimAsString("preferred_username"));
        userInfo.put("name", jwt.getClaimAsString("name"));
        userInfo.put("tokenExpiry", jwt.getExpiresAt());

        return ResponseEntity.ok(userInfo);
    }

    /**
     * Public endpoint to verify API availability.
     *
     * @return status message
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of("status", "online", "authProvider", "Microsoft Entra ID"));
    }
}
