package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.model.entity.GameSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.HashOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisGameStateLoggerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RedisGameStateLogger logger;

    @Test
    void logGameStateWritesHashAndSetsTtl() throws Exception {
        GameSession session = mock(GameSession.class);
        when(session.getStatus()).thenReturn(com.arsw.bomberdino.model.enums.GameStatus.IN_PROGRESS);
        when(session.getPlayers()).thenReturn(java.util.List.of());
        when(session.getActiveBombs()).thenReturn(java.util.List.of());
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        logger.logGameState("session-1", session, "SNAPSHOT");

        verify(hashOperations).putAll(anyString(), any(Map.class));
        verify(redisTemplate).expire(anyString(), anyLong(), any());
    }

    @Test
    void logEventWritesHashAndSetsTtl() {
        Map<String, Object> data = new HashMap<>();
        data.put("foo", "bar");

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        logger.logEvent("session-1", "EVT", data);

        verify(hashOperations).putAll(anyString(), any(Map.class));
        verify(redisTemplate).expire(anyString(), anyLong(), any());
    }

    @Test
    void deleteGameStateDeletesKey() {
        logger.deleteGameState("session-1");

        verify(redisTemplate).delete(anyString());
    }

    @Test
    void methodsSwallowExceptions() throws Exception {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        doThrow(new RuntimeException("boom")).when(objectMapper).writeValueAsString(any());
        GameSession session = mock(GameSession.class);
        when(session.getStatus()).thenReturn(com.arsw.bomberdino.model.enums.GameStatus.IN_PROGRESS);
        when(session.getPlayers()).thenReturn(java.util.List.of());
        when(session.getActiveBombs()).thenReturn(java.util.List.of());

        logger.logGameState("session-1", session, "SNAPSHOT");
        logger.logEvent("session-1", "EVT", Map.of());
        logger.deleteGameState("session-1");
    }
}
