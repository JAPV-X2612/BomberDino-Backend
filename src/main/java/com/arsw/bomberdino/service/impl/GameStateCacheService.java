package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.model.entity.GameSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Service for caching game state in Redis.
 * Provides centralized state management for horizontal scaling.
 *
 * @author Yisus-Rex
 * @version 1.0
 * @since 2025-12-01
 */
@Service
public class GameStateCacheService {

    private static final Logger logger = LoggerFactory.getLogger(GameStateCacheService.class);
    private static final String GAME_STATE_KEY_PREFIX = "game:";
    private static final String GAME_STATE_KEY_SUFFIX = ":state";
    private static final Duration DEFAULT_TTL = Duration.ofHours(2);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public GameStateCacheService(RedisTemplate<String, Object> redisTemplate,
                                 ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Saves game session state to Redis cache.
     *
     * @param sessionId unique session identifier
     * @param gameSession session object to cache
     * @throws RuntimeException if serialization fails
     */
    public void saveGameState(UUID sessionId, GameSession gameSession) {
        String key = buildStateKey(sessionId);

        try {
            String jsonState = objectMapper.writeValueAsString(gameSession);
            redisTemplate.opsForValue().set(key, jsonState, DEFAULT_TTL);
            logger.debug("Game state saved to Redis: {}", sessionId);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize game state for session {}", sessionId, e);
            throw new RuntimeException("Redis serialization error", e);
        }
    }

    /**
     * Retrieves game session state from Redis cache.
     *
     * @param sessionId unique session identifier
     * @return GameSession object or null if not found
     * @throws RuntimeException if deserialization fails
     */
    public GameSession getGameState(UUID sessionId) {
        String key = buildStateKey(sessionId);
        Object cached = redisTemplate.opsForValue().get(key);

        if (cached == null) {
            logger.debug("Game state not found in Redis: {}", sessionId);
            return null;
        }

        try {
            return objectMapper.readValue(cached.toString(), GameSession.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize game state for session {}", sessionId, e);
            throw new RuntimeException("Redis deserialization error", e);
        }
    }

    /**
     * Deletes game session state from Redis cache.
     * @param sessionId unique session identifier
     */
    public void deleteGameState(UUID sessionId) {
        String key = buildStateKey(sessionId);
        redisTemplate.delete(key);
        logger.debug("Game state deleted from Redis: {}", sessionId);
    }

    /**
     * Checks if game state exists in cache.
     *
     * @param sessionId unique session identifier
     * @return true if state exists in Redis
     */
    public boolean existsGameState(UUID sessionId) {
        String key = buildStateKey(sessionId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Builds Redis key for game state.
     *
     * @param sessionId unique session identifier
     * @return formatted Redis key
     */
    private String buildStateKey(UUID sessionId) {
        return GAME_STATE_KEY_PREFIX + sessionId + GAME_STATE_KEY_SUFFIX;
    }
}
