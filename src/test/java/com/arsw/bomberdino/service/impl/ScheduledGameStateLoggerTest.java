package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.model.entity.GameSession;
import com.arsw.bomberdino.model.enums.GameStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledGameStateLoggerTest {

    @Mock
    private GameSessionService gameSessionService;

    @Mock
    private RedisGameStateLogger redisLogger;

    @InjectMocks
    private ScheduledGameStateLogger scheduledLogger;

    @Test
    void logActiveGameStatesLogsSnapshotsForInProgressSessions() {
        GameSession session = mock(GameSession.class);
        when(session.getSessionId()).thenReturn(UUID.randomUUID());
        when(gameSessionService.getSessionsByStatus(GameStatus.IN_PROGRESS))
                .thenReturn(List.of(session));

        scheduledLogger.logActiveGameStates();

        verify(redisLogger).logGameState(anyString(), eq(session), eq("SNAPSHOT"));
    }

    @Test
    void logActiveGameStatesSkipsWhenNoActiveSessions() {
        when(gameSessionService.getSessionsByStatus(GameStatus.IN_PROGRESS))
                .thenReturn(List.of());

        scheduledLogger.logActiveGameStates();

        verifyNoInteractions(redisLogger);
    }

    @Test
    void logActiveGameStatesSwallowsExceptions() {
        when(gameSessionService.getSessionsByStatus(GameStatus.IN_PROGRESS))
                .thenThrow(new RuntimeException("boom"));

        scheduledLogger.logActiveGameStates();

        verifyNoInteractions(redisLogger);
    }
}
