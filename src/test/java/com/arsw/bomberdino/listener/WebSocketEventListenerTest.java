package com.arsw.bomberdino.listener;

import com.arsw.bomberdino.controller.websocket.WebSocketController;
import com.arsw.bomberdino.model.dto.response.GameStateDTO;
import com.arsw.bomberdino.model.dto.response.PlayerKilledDTO;
import com.arsw.bomberdino.model.dto.response.PlayerMovedEventDTO;
import com.arsw.bomberdino.model.entity.Bomb;
import com.arsw.bomberdino.model.entity.GameSession;
import com.arsw.bomberdino.model.entity.Player;
import com.arsw.bomberdino.model.enums.Direction;
import com.arsw.bomberdino.model.event.BombExplodedEvent;
import com.arsw.bomberdino.model.event.BombPlacedEvent;
import com.arsw.bomberdino.model.event.PlayerKilledEvent;
import com.arsw.bomberdino.model.event.PlayerMovedEvent;
import com.arsw.bomberdino.model.event.PowerUpCollectedEvent;
import com.arsw.bomberdino.service.impl.GameSessionService;
import com.arsw.bomberdino.util.SequenceNumberManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    @Mock
    private WebSocketController webSocketController;
    @Mock
    private GameSessionService gameSessionService;
    @Mock
    private SequenceNumberManager sequenceNumberManager;

    @InjectMocks
    private WebSocketEventListener listener;

    @Test
    void onPlayerMovedBroadcastsDelta() {
        String sessionId = "sid";
        Player player = Player.builder()
                .id(UUID.randomUUID())
                .posX(2)
                .posY(3)
                .build();
        GameSession session = mock(GameSession.class);
        when(session.getPlayers()).thenReturn(List.of(player));
        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        when(sequenceNumberManager.getNextSequenceNumber(sessionId)).thenReturn(5L);

        listener.onPlayerMoved(PlayerMovedEvent.of(sessionId, player.getId().toString(), Direction.UP));

        ArgumentCaptor<PlayerMovedEventDTO> captor = ArgumentCaptor.forClass(PlayerMovedEventDTO.class);
        verify(webSocketController).broadcastPlayerMoved(eq(sessionId), captor.capture());
        assertEquals(player.getPosX(), captor.getValue().getNewX());
        assertEquals(5L, captor.getValue().getSequenceNumber());
    }

    @Test
    void onBombPlacedBroadcastsDelta() {
        String sessionId = "sid";
        Bomb bomb = Bomb.builder()
                .id(UUID.randomUUID())
                .posX(1)
                .posY(1)
                .range(2)
                .placedTime(System.currentTimeMillis())
                .explosionDelay(3000L)
                .build();
        bomb.initDefaults();

        GameSession session = mock(GameSession.class);
        when(session.getActiveBombs()).thenReturn(new ArrayList<>(List.of(bomb)));
        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        when(sequenceNumberManager.getNextSequenceNumber(sessionId)).thenReturn(3L);

        listener.onBombPlaced(BombPlacedEvent.of(sessionId, "player", bomb.getId().toString()));

        verify(webSocketController).broadcastBombPlaced(eq(sessionId), any());
    }

    @Test
    void onBombExplodedBroadcastsEventAndState() {
        String sessionId = "sid";
        GameSession session = mock(GameSession.class);
        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        when(session.getCurrentState()).thenReturn(GameStateDTO.builder().sessionId(sessionId).build());

        BombExplodedEvent event = BombExplodedEvent.of(sessionId, "bomb",
                List.of(new Point(1, 1)), List.of("p1"));

        listener.onBombExploded(event);

        verify(webSocketController).broadcastBombExploded(eq(sessionId), any());
        verify(webSocketController).broadcastGameState(eq(sessionId), any(GameStateDTO.class));
    }

    @Test
    void onPlayerKilledBroadcastsEventAndState() {
        String sessionId = "sid";
        GameSession session = mock(GameSession.class);
        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        when(session.getCurrentState()).thenReturn(GameStateDTO.builder().sessionId(sessionId).build());

        listener.onPlayerKilled(PlayerKilledEvent.of(sessionId, "killer", "victim"));

        verify(webSocketController).broadcastPlayerKilled(eq(sessionId), any(PlayerKilledDTO.class));
        verify(webSocketController).broadcastGameState(eq(sessionId), any(GameStateDTO.class));
    }

    @Test
    void onPowerUpCollectedBroadcastsEventAndState() {
        String sessionId = "sid";
        GameSession session = mock(GameSession.class);
        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        when(session.getCurrentState()).thenReturn(GameStateDTO.builder().sessionId(sessionId).build());

        PowerUpCollectedEvent event = PowerUpCollectedEvent.of(sessionId, "player", "power",
                com.arsw.bomberdino.model.dto.response.PowerUpEffect.builder()
                        .type(com.arsw.bomberdino.model.enums.PowerUpType.BOMB_COUNT_UP)
                        .duration(5)
                        .multiplier(1.0f)
                        .build());

        listener.onPowerUpCollected(event);

        verify(webSocketController).broadcastToSession(eq(sessionId), eq("/powerup"), eq(event));
        verify(webSocketController).broadcastGameState(eq(sessionId), any(GameStateDTO.class));
    }
}
