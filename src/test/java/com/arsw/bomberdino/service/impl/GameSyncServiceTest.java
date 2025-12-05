package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.controller.websocket.WebSocketController;
import com.arsw.bomberdino.model.dto.response.GameStateDTO;
import com.arsw.bomberdino.model.dto.response.HeartbeatEventDTO;
import com.arsw.bomberdino.model.entity.GameSession;
import com.arsw.bomberdino.model.entity.Player;
import com.arsw.bomberdino.model.enums.GameStatus;
import com.arsw.bomberdino.model.enums.PlayerStatus;
import com.arsw.bomberdino.util.SequenceNumberManager;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameSyncServiceTest {

    @Mock
    private GameSessionService gameSessionService;

    @Mock
    private WebSocketController webSocketController;

    @Mock
    private SequenceNumberManager sequenceNumberManager;

    @InjectMocks
    private GameSyncService service;

    @Test
    void sendHeartbeatsBroadcastsAliveCountForActiveSessions() {
        GameSession session = mock(GameSession.class);
        UUID sessionId = UUID.randomUUID();

        Player alive = mock(Player.class);
        when(alive.getStatus()).thenReturn(PlayerStatus.ALIVE);
        Player dead = mock(Player.class);
        when(dead.getStatus()).thenReturn(PlayerStatus.DEAD);

        when(session.getStatus()).thenReturn(GameStatus.IN_PROGRESS);
        when(session.getSessionId()).thenReturn(sessionId);
        when(session.getPlayers()).thenReturn(List.of(alive, dead));
        when(gameSessionService.getSessionsByStatus(GameStatus.IN_PROGRESS))
                .thenReturn(List.of(session));
        when(sequenceNumberManager.getNextSequenceNumber(sessionId.toString())).thenReturn(5L);

        service.sendHeartbeats();

        ArgumentCaptor<HeartbeatEventDTO> captor = ArgumentCaptor.forClass(HeartbeatEventDTO.class);
        verify(webSocketController).broadcastHeartbeat(eq(sessionId.toString()),
                captor.capture());
        HeartbeatEventDTO heartbeat = captor.getValue();
        assertEquals(5L, heartbeat.getSequenceNumber());
        assertEquals(GameStatus.IN_PROGRESS, heartbeat.getStatus());
        assertEquals(1, heartbeat.getAlivePlayersCount());
    }

    @Test
    void sendHeartbeatsSkipsFinishedSessions() {
        GameSession finished = mock(GameSession.class);
        when(finished.getStatus()).thenReturn(GameStatus.FINISHED);
        when(gameSessionService.getSessionsByStatus(GameStatus.IN_PROGRESS))
                .thenReturn(List.of(finished));

        service.sendHeartbeats();

        verifyNoInteractions(webSocketController);
    }

    @Test
    void sendPeriodicSyncBroadcastsFullState() {
        GameSession session = mock(GameSession.class);
        UUID sessionId = UUID.randomUUID();
        GameStateDTO state = GameStateDTO.builder()
                .sessionId(sessionId.toString())
                .status(GameStatus.IN_PROGRESS)
                .players(List.of())
                .bombs(List.of())
                .explosions(List.of())
                .powerUps(List.of())
                .serverTime(System.currentTimeMillis())
                .build();

        when(session.getStatus()).thenReturn(GameStatus.IN_PROGRESS);
        when(session.getSessionId()).thenReturn(sessionId);
        when(session.getCurrentState()).thenReturn(state);
        when(gameSessionService.getSessionsByStatus(GameStatus.IN_PROGRESS))
                .thenReturn(List.of(session));

        service.sendPeriodicSync();

        verify(webSocketController).broadcastPeriodicSync(sessionId.toString(),
                state);
    }

    @Test
    void cleanupFinishedSessionsResetsSequences() {
        GameSession finished = new GameSession();
        UUID sessionId = UUID.randomUUID();
        finished.setSessionId(sessionId);
        when(gameSessionService.getSessionsByStatus(GameStatus.FINISHED))
                .thenReturn(List.of(finished));

        service.cleanupFinishedSessions();

        verify(sequenceNumberManager).resetSequence(sessionId.toString());
    }

    @Test
    void methodsSwallowExceptionsToAvoidCrashingScheduler() {
        Logger logger = (Logger) LoggerFactory.getLogger(GameSyncService.class);
        Level original = logger.getLevel();
        logger.setLevel(Level.OFF);
        try {
            when(gameSessionService.getSessionsByStatus(any())).thenThrow(new RuntimeException("boom"));

            assertDoesNotThrow(() -> service.sendHeartbeats());
            assertDoesNotThrow(() -> service.sendPeriodicSync());
            assertDoesNotThrow(() -> service.cleanupFinishedSessions());
        } finally {
            logger.setLevel(original);
        }
    }
}
