package com.arsw.bomberdino.scheduler;

import com.arsw.bomberdino.controller.websocket.WebSocketController;
import com.arsw.bomberdino.model.dto.response.HeartbeatEventDTO;
import com.arsw.bomberdino.model.entity.GameSession;
import com.arsw.bomberdino.model.entity.Player;
import com.arsw.bomberdino.model.enums.GameStatus;
import com.arsw.bomberdino.service.impl.GameSessionService;
import com.arsw.bomberdino.util.SequenceNumberManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeartbeatSchedulerTest {

    @Mock
    private GameSessionService gameSessionService;
    @Mock
    private WebSocketController webSocketController;
    @Mock
    private SequenceNumberManager sequenceNumberManager;

    @InjectMocks
    private HeartbeatScheduler scheduler;

    @Test
    void sendHeartbeatsBroadcastsAliveCount() {
        GameSession session = mock(GameSession.class);
        Player alive = mock(Player.class);
        Player dead = mock(Player.class);
        when(alive.isAlive()).thenReturn(true);
        when(dead.isAlive()).thenReturn(false);

        UUID sessionId = UUID.randomUUID();
        when(session.getSessionId()).thenReturn(sessionId);
        when(session.getStatus()).thenReturn(GameStatus.IN_PROGRESS);
        when(session.getPlayers()).thenReturn(List.of(alive, dead));
        when(gameSessionService.getSessionsByStatus(GameStatus.IN_PROGRESS))
                .thenReturn(List.of(session));
        when(sequenceNumberManager.getNextSequenceNumber(sessionId.toString())).thenReturn(10L);

        scheduler.sendHeartbeats();

        ArgumentCaptor<HeartbeatEventDTO> captor = ArgumentCaptor.forClass(HeartbeatEventDTO.class);
        verify(webSocketController).broadcastHeartbeat(eq(sessionId.toString()), captor.capture());
        assertEquals(1, captor.getValue().getAlivePlayersCount());
        assertEquals(10L, captor.getValue().getSequenceNumber());
    }

    @Test
    void sendHeartbeatsContinuesWhenPerSessionFails() {
        GameSession session = mock(GameSession.class);
        UUID sessionId = UUID.randomUUID();
        when(session.getSessionId()).thenReturn(sessionId);
        when(session.getStatus()).thenReturn(GameStatus.IN_PROGRESS);
        when(session.getPlayers()).thenReturn(List.of(mock(Player.class)));
        when(gameSessionService.getSessionsByStatus(GameStatus.IN_PROGRESS))
                .thenReturn(List.of(session));
        when(sequenceNumberManager.getNextSequenceNumber(anyString()))
                .thenThrow(new RuntimeException("boom"));

        scheduler.sendHeartbeats();

        verify(webSocketController, never()).broadcastHeartbeat(anyString(), any());
    }
}
