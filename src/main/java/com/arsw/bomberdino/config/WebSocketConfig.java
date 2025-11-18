package com.arsw.bomberdino.config;

import java.util.Arrays;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time game communication.
 * Configures STOMP messaging protocol and message broker.
 *
 * @author Mapunix, Rivaceratops, Yisus-Rex
 * @version 1.0
 * @since 2025-10-28
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String raw = System.getenv("WEBSOCKET_ALLOWED_ORIGINS");

        String[] origins = (raw != null && !raw.isBlank())
                ? Arrays.stream(raw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toArray(String[]::new)
                : new String[] {
                        "http://localhost:*",
                        "https://localhost:*",
                        "http://127.0.0.1:*",
                        "https://127.0.0.1:*"
                };

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins)
                .withSockJS();
    }
}
