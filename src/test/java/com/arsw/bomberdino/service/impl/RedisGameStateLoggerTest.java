package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.model.entity.GameSession;
import com.arsw.bomberdino.model.dto.response.GameStateDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        when(session.getStartTime()).thenReturn(java.time.LocalDateTime.now());
        when(session.getEndTime()).thenReturn(java.time.LocalDateTime.now());
        GameStateDTO state = GameStateDTO.builder().sessionId("session-1").build();
        when(session.getCurrentState()).thenReturn(state);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        logger.logGameState("session-1", session, "SNAPSHOT");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(anyString(), captor.capture());
        Map<String, Object> payload = captor.getValue();
        assertTrue(payload.containsKey("startTime"));
        assertTrue(payload.containsKey("endTime"));
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

    @Test
    void logEventSwallowsErrors() {
        when(redisTemplate.opsForHash()).thenThrow(new RuntimeException("fail"));

        logger.logEvent("sid", "EVT", Map.of("foo", "bar"));
    }

    @Test
    void deleteGameStateSwallowsErrors() {
        doThrow(new RuntimeException("fail")).when(redisTemplate).delete(anyString());

        logger.deleteGameState("sid");
    }
}
