package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.model.dto.response.GameStateDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for subscribing to game events from Redis PubSub channels.
 * Receives events from other backend instances and broadcasts to local WebSocket clients.
 *
 * @author Yisus-Rex
 * @version 1.0
 * @since 2025-12-01
 */
@Service
public class GameEventSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(GameEventSubscriber.class);
    private static final String GAME_CHANNEL_PREFIX = "game:";
    private static final String GAME_CHANNEL_SUFFIX = ":events";

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<String, Integer> activeSubscriptions = new ConcurrentHashMap<>();

    public GameEventSubscriber(RedissonClient redissonClient,
                               ObjectMapper objectMapper,
                               SimpMessagingTemplate messagingTemplate) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Subscribes to game events for a specific session.
     * Multiple calls increment subscriber count.
     *
     * @param sessionId unique session identifier
     */
    public void subscribeToSession(UUID sessionId) {
        String channelName = buildChannelName(sessionId);

        activeSubscriptions.compute(channelName, (key, count) -> {
            if (count == null) {
                RTopic topic = redissonClient.getTopic(channelName);

                topic.addListener(String.class, (channel, message) -> {
                    handleIncomingEvent(sessionId, message);
                });

                logger.info("Subscribed to Redis channel: {}", channelName);
                return 1;
            } else {
                return count + 1;
            }
        });
    }

    /**
     * Unsubscribes from game events for a specific session.
     * Only removes subscription when subscriber count reaches zero.
     *
     * @param sessionId unique session identifier
     */
    public void unsubscribeFromSession(UUID sessionId) {
        String channelName = buildChannelName(sessionId);

        activeSubscriptions.computeIfPresent(channelName, (key, count) -> {
            if (count <= 1) {
                RTopic topic = redissonClient.getTopic(channelName);
                topic.removeAllListeners();
                logger.info("Unsubscribed from Redis channel: {}", channelName);
                return null;
            } else {
                return count - 1;
            }
        });
    }

    /**
     * Handles incoming event from Redis PubSub.
     * Broadcasts to local WebSocket clients connected to this backend instance.
     *
     * @param sessionId unique session identifier
     * @param jsonMessage serialized event message
     */
    private void handleIncomingEvent(UUID sessionId, String jsonMessage) {
        try {
            GameStateDTO gameState = objectMapper.readValue(jsonMessage, GameStateDTO.class);

            String destination = "/topic/game/" + sessionId + "/state";
            messagingTemplate.convertAndSend(destination, gameState);

            logger.debug("Broadcasted Redis event to WebSocket clients: {}", destination);
        } catch (Exception e) {
            logger.error("Failed to process incoming Redis event for session {}", sessionId, e);
        }
    }

    /**
     * Builds Redis PubSub channel name for session.
     *
     * @param sessionId unique session identifier
     * @return formatted channel name
     */
    private String buildChannelName(UUID sessionId) {
        return GAME_CHANNEL_PREFIX + sessionId + GAME_CHANNEL_SUFFIX;
    }
}
