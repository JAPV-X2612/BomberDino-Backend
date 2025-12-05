package com.arsw.bomberdino.scheduler;

import com.arsw.bomberdino.controller.websocket.WebSocketController;
import com.arsw.bomberdino.model.dto.response.GameStateDTO;
import com.arsw.bomberdino.model.entity.GameSession;
import com.arsw.bomberdino.model.enums.GameStatus;
import com.arsw.bomberdino.service.impl.GameSessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PeriodicSyncSchedulerTest {

    @Mock
    private GameSessionService gameSessionService;

    @Mock
    private WebSocketController webSocketController;

    @InjectMocks
    private PeriodicSyncScheduler scheduler;

    @Test
    void sendPeriodicSyncBroadcastsFullStateForActiveSessions() {
        GameSession session = mock(GameSession.class);
        UUID sessionId = UUID.randomUUID();
        GameStateDTO state = GameStateDTO.builder()
                .sessionId(sessionId.toString())
                .players(List.of())
                .bombs(List.of())
                .explosions(List.of())
                .powerUps(List.of())
                .build();

        when(session.getSessionId()).thenReturn(sessionId);
        when(session.getCurrentState()).thenReturn(state);
        when(gameSessionService.getSessionsByStatus(GameStatus.IN_PROGRESS))
                .thenReturn(List.of(session));

        scheduler.sendPeriodicSync();

        verify(webSocketController).broadcastPeriodicSync(sessionId.toString(), state);
    }

    @Test
    void sendPeriodicSyncContinuesWhenPerSessionFails() {
        GameSession session = mock(GameSession.class);
        UUID sessionId = UUID.randomUUID();
        GameStateDTO state = GameStateDTO.builder().sessionId(sessionId.toString()).build();

        when(session.getSessionId()).thenReturn(sessionId);
        when(session.getCurrentState()).thenReturn(state);
        when(gameSessionService.getSessionsByStatus(GameStatus.IN_PROGRESS))
                .thenReturn(List.of(session));
        doThrow(new RuntimeException("boom"))
                .when(webSocketController).broadcastPeriodicSync(anyString(), any(GameStateDTO.class));

        scheduler.sendPeriodicSync();

        verify(webSocketController).broadcastPeriodicSync(sessionId.toString(), state);
    }
}
