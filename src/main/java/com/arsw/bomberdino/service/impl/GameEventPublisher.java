package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.model.dto.response.GameStateDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for publishing game events to Redis PubSub channels.
 * Enables real-time synchronization across multiple backend instances.
 *
 * @author Yisus-Rex
 * @version 1.0
 * @since 2025-12-01
 */
@Service
public class GameEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(GameEventPublisher.class);
    private static final String GAME_CHANNEL_PREFIX = "game:";
    private static final String GAME_CHANNEL_SUFFIX = ":events";

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public GameEventPublisher(RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes game state update to all backend instances.
     *
     * @param sessionId unique session identifier
     * @param gameState current game state snapshot
     */
    public void publishGameState(UUID sessionId, GameStateDTO gameState) {
        String channelName = buildChannelName(sessionId);
        RTopic topic = redissonClient.getTopic(channelName);

        try {
            String jsonPayload = objectMapper.writeValueAsString(gameState);
            long subscribers = topic.publish(jsonPayload);
            logger.debug("Published game state to {} subscribers on channel: {}", subscribers, channelName);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize game state for session {}", sessionId, e);
        }
    }

    /**
     * Publishes generic game event to all backend instances.
     *
     * @param sessionId unique session identifier
     * @param event event object to publish
     */
    public void publishEvent(UUID sessionId, Object event) {
        String channelName = buildChannelName(sessionId);
        RTopic topic = redissonClient.getTopic(channelName);

        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            topic.publish(jsonPayload);
            logger.debug("Published event to channel: {}", channelName);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event for session {}", sessionId, e);
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
