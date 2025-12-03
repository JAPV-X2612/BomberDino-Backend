package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.model.entity.GameSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for logging game state snapshots to Redis.
 * All operations are asynchronous and fail silently to avoid impacting game performance.
 *
 * @author Mapunix, Rivaceratops, Yisus-Rex
 * @version 1.0
 * @since 2025-12-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisGameStateLogger {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${redis.game-state.ttl-seconds:10800}")
    private long ttlSeconds;

    @Value("${redis.game-state.key-prefix:game:session}")
    private String keyPrefix;

    /**
     * Logs complete game session state snapshot to Redis asynchronously.
     * Creates/updates Hash with session data and sets TTL.
     *
     * @param sessionId unique session identifier
     * @param session GameSession instance to log
     * @param eventType type of event triggering the log (e.g., "SNAPSHOT", "BOMB_EXPLODED")
     */
    @Async
    public void logGameState(String sessionId, GameSession session, String eventType) {
        try {
            String key = buildKey(sessionId);

            Map<String, Object> stateData = new HashMap<>();
            stateData.put("sessionId", sessionId);
            stateData.put("status", session.getStatus().toString());
            stateData.put("gameState", objectMapper.writeValueAsString(session.getCurrentState()));
            stateData.put("lastUpdate", LocalDateTime.now().toString());
            stateData.put("eventType", eventType);
            stateData.put("playerCount", session.getPlayers().size());
            stateData.put("activeBombCount", session.getActiveBombs().size());

            if (session.getStartTime() != null) {
                stateData.put("startTime", session.getStartTime().toString());
            }

            if (session.getEndTime() != null) {
                stateData.put("endTime", session.getEndTime().toString());
            }

            redisTemplate.opsForHash().putAll(key, stateData);
            redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);

            log.debug("Logged game state for session {} - Event: {}", sessionId, eventType);

        } catch (Exception e) {
            log.warn("Failed to log game state to Redis for session {}: {}",
                    sessionId, e.getMessage());
        }
    }

    /**
     * Logs game session event without full state snapshot.
     * Used for lightweight event tracking.
     *
     * @param sessionId unique session identifier
     * @param eventType type of event
     * @param eventData additional event metadata
     */
    @Async
    public void logEvent(String sessionId, String eventType, Map<String, Object> eventData) {
        try {
            String key = buildKey(sessionId);

            Map<String, Object> data = new HashMap<>(eventData);
            data.put("eventType", eventType);
            data.put("timestamp", LocalDateTime.now().toString());

            redisTemplate.opsForHash().putAll(key, data);
            redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);

            log.debug("Logged event {} for session {}", eventType, sessionId);

        } catch (Exception e) {
            log.warn("Failed to log event to Redis for session {}: {}",
                    sessionId, e.getMessage());
        }
    }

    /**
     * Deletes game state from Redis when session ends or expires.
     *
     * @param sessionId unique session identifier
     */
    @Async
    public void deleteGameState(String sessionId) {
        try {
            String key = buildKey(sessionId);
            redisTemplate.delete(key);
            log.debug("Deleted game state for session {}", sessionId);

        } catch (Exception e) {
            log.warn("Failed to delete game state from Redis for session {}: {}",
                    sessionId, e.getMessage());
        }
    }

    /**
     * Builds Redis key for game session state.
     *
     * @param sessionId unique session identifier
     * @return formatted Redis key
     */
    private String buildKey(String sessionId) {
        return String.format("%s:%s:state", keyPrefix, sessionId);
    }
}
