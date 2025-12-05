package com.arsw.bomberdino.controller.rest.v1;

import com.arsw.bomberdino.controller.websocket.WebSocketController;
import com.arsw.bomberdino.model.dto.request.CreateRoomRequestDTO;
import com.arsw.bomberdino.model.dto.request.JoinRoomRequestDTO;
import com.arsw.bomberdino.model.dto.response.GameRoomDTO;
import com.arsw.bomberdino.model.dto.response.GameStateDTO;
import com.arsw.bomberdino.model.entity.GameMap;
import com.arsw.bomberdino.model.entity.GameSession;
import com.arsw.bomberdino.model.entity.Player;
import com.arsw.bomberdino.model.enums.GameStatus;
import com.arsw.bomberdino.service.impl.GameSessionService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.junit.jupiter.api.AfterEach;

import java.awt.Point;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private GameSessionService gameSessionService;

    @Mock
    private WebSocketController webSocketController;

    @InjectMocks
    private GameController controller;

    private Level originalLevel;

    @BeforeEach
    void init() {
        Logger logger = (Logger) LoggerFactory.getLogger(GameController.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(GameController.class);
        logger.setLevel(originalLevel);
    }

    @Test
    void createRoomReturnsCreatedRoomDto() {
        CreateRoomRequestDTO request = CreateRoomRequestDTO.builder()
                .roomName("Test Room")
                .maxPlayers(4)
                .isPrivate(false)
                .password(null)
                .build();

        GameSession session = mock(GameSession.class);
        when(session.getSessionId()).thenReturn(UUID.randomUUID());
        when(session.getPlayers()).thenReturn(List.of());
        when(gameSessionService.createSession(any(), eq(request.getMaxPlayers())))
                .thenReturn(session);

        ResponseEntity<GameRoomDTO> response = controller.createRoom(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(GameStatus.WAITING, response.getBody().getStatus());
    }

    @Test
    void createRoomRethrowsIllegalArgument() {
        CreateRoomRequestDTO request = CreateRoomRequestDTO.builder()
                .roomName("Bad Room")
                .maxPlayers(4)
                .isPrivate(false)
                .password(null)
                .build();

        when(gameSessionService.createSession(any(), anyInt()))
                .thenThrow(new IllegalArgumentException("invalid"));

        assertThrows(IllegalArgumentException.class,
                () -> withLoggerSilenced(() -> controller.createRoom(request)));
    }

    @Test
    void createRoomWrapsUnexpectedException() {
        CreateRoomRequestDTO request = CreateRoomRequestDTO.builder()
                .roomName("Bad Room")
                .maxPlayers(4)
                .isPrivate(false)
                .password(null)
                .build();

        when(gameSessionService.createSession(any(), anyInt()))
                .thenThrow(new RuntimeException("boom"));

        assertThrows(RuntimeException.class,
                () -> withLoggerSilenced(() -> controller.createRoom(request)));
    }

    @Test
    void joinRoomAddsPlayerAndBroadcastsState() {
        JoinRoomRequestDTO request = JoinRoomRequestDTO.builder()
                .roomId("ROOM01")
                .playerId("player1")
                .username("Alice")
                .build();

        GameMap map = mock(GameMap.class);
        when(map.getAvailableSpawnPoints()).thenReturn(List.of(new Point(1, 1)));

        GameSession session = mock(GameSession.class);
        when(session.getStatus()).thenReturn(GameStatus.WAITING);
        when(session.getMap()).thenReturn(map);
        when(session.getPlayers()).thenReturn(new ArrayList<>());
        GameStateDTO state = GameStateDTO.builder().sessionId("ROOM01").players(List.of()).bombs(List.of())
                .explosions(List.of()).powerUps(List.of()).build();
        when(session.getCurrentState()).thenReturn(state);
        when(gameSessionService.getSession("ROOM01")).thenReturn(session);

        ResponseEntity<GameRoomDTO> response = controller.joinRoom(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(webSocketController).broadcastGameState("ROOM01", state);
    }

    @Test
    void joinRoomReturnsBadRequestOnValidationError() {
        JoinRoomRequestDTO request = JoinRoomRequestDTO.builder()
                .roomId("ROOM01")
                .playerId("player1")
                .username("Alice")
                .build();

        when(gameSessionService.getSession("ROOM01")).thenThrow(new IllegalArgumentException("bad"));

        ResponseEntity<GameRoomDTO> response = withLoggerSilenced(() -> controller.joinRoom(request));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void joinRoomReturnsConflictWhenGameStarted() {
        JoinRoomRequestDTO request = JoinRoomRequestDTO.builder()
                .roomId("ROOM01")
                .playerId("player1")
                .username("Alice")
                .build();

        GameSession session = mock(GameSession.class);
        when(session.getStatus()).thenReturn(GameStatus.IN_PROGRESS);
        when(gameSessionService.getSession("ROOM01")).thenReturn(session);

        ResponseEntity<GameRoomDTO> response = controller.joinRoom(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(webSocketController, never()).broadcastGameState(any(), any());
    }

    @Test
    void joinRoomReturnsConflictWhenNoSpawnPoints() {
        JoinRoomRequestDTO request = JoinRoomRequestDTO.builder()
                .roomId("ROOM01")
                .playerId("player1")
                .username("Alice")
                .build();

        GameSession session = mock(GameSession.class);
        GameMap map = mock(GameMap.class);
        when(map.getAvailableSpawnPoints()).thenReturn(List.of());
        when(session.getStatus()).thenReturn(GameStatus.WAITING);
        when(session.getMap()).thenReturn(map);
        when(gameSessionService.getSession("ROOM01")).thenReturn(session);

        ResponseEntity<GameRoomDTO> response = controller.joinRoom(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(webSocketController, never()).broadcastGameState(any(), any());
    }

    @Test
    void getRoomsByStatusReturnsList() {
        GameSession session = mock(GameSession.class);
        when(session.getSessionId()).thenReturn(UUID.randomUUID());
        when(session.getStatus()).thenReturn(GameStatus.WAITING);
        when(session.getPlayers()).thenReturn(List.of());
        when(gameSessionService.getSessionsByStatus(GameStatus.WAITING))
                .thenReturn(List.of(session));

        ResponseEntity<List<GameRoomDTO>> response = controller.getRoomsByStatus(GameStatus.WAITING);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getRoomsByStatusReturnsBadRequestOnInvalidParam() {
        when(gameSessionService.getSessionsByStatus(GameStatus.WAITING))
                .thenThrow(new IllegalArgumentException("bad"));

        ResponseEntity<List<GameRoomDTO>> response = controller.getRoomsByStatus(GameStatus.WAITING);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void startGameWithInsufficientPlayersReturnsBadRequest() {
        GameSession session = mock(GameSession.class);
        when(session.getPlayers()).thenReturn(List.of());
        when(gameSessionService.getSession("sid")).thenReturn(session);

        ResponseEntity<Void> response = withLoggerSilenced(() -> controller.startGame("sid", "pid"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(webSocketController, never()).broadcastGameStart(any(), any());
    }

    @Test
    void startGameWithEnoughPlayersBroadcastsStart() {
        GameSession session = mock(GameSession.class);
        Player p1 = Player.builder().id(UUID.randomUUID()).posX(0).posY(0).lifeCount(3).bombCount(1)
                .bombRange(2).speed(1).status(com.arsw.bomberdino.model.enums.PlayerStatus.ALIVE)
                .spawnPoint(new Point(0, 0)).build();
        Player p2 = Player.builder().id(UUID.randomUUID()).posX(1).posY(1).lifeCount(3).bombCount(1)
                .bombRange(2).speed(1).status(com.arsw.bomberdino.model.enums.PlayerStatus.ALIVE)
                .spawnPoint(new Point(1, 1)).build();
        when(session.getPlayers()).thenReturn(List.of(p1, p2));
        GameStateDTO state = GameStateDTO.builder().sessionId("sid").players(List.of()).bombs(List.of())
                .explosions(List.of()).powerUps(List.of()).build();
        when(session.getCurrentState()).thenReturn(state);
        when(gameSessionService.getSession("sid")).thenReturn(session);

        ResponseEntity<Void> response = withLoggerSilenced(() -> controller.startGame("sid", "pid"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(webSocketController).broadcastGameStart("sid", state);
    }

    @Test
    void startGameHandlesExceptions() {
        when(gameSessionService.getSession("sid")).thenThrow(new RuntimeException("boom"));

        ResponseEntity<Void> response = withLoggerSilenced(() -> controller.startGame("sid", "pid"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void leaveRoomRemovesPlayer() {
        ResponseEntity<Void> response = controller.leaveRoom("sid", "pid");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(gameSessionService).removePlayer("sid", "pid");
    }

    @Test
    void leaveRoomReturnsBadRequestOnValidation() {
        doThrow(new IllegalArgumentException("bad")).when(gameSessionService).removePlayer("sid", "pid");

        ResponseEntity<Void> response = withLoggerSilenced(() -> controller.leaveRoom("sid", "pid"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void leaveRoomReturnsNotFoundOnIllegalState() {
        doThrow(new IllegalStateException("missing")).when(gameSessionService).removePlayer("sid", "pid");

        ResponseEntity<Void> response = withLoggerSilenced(() -> controller.leaveRoom("sid", "pid"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getGameStateReturnsStateOrNotFound() {
        GameSession session = mock(GameSession.class);
        GameStateDTO state = GameStateDTO.builder()
                .sessionId("sid")
                .players(List.of())
                .bombs(List.of())
                .explosions(List.of())
                .powerUps(List.of())
                .build();
        when(session.getCurrentState()).thenReturn(state);
        when(gameSessionService.getSession("sid")).thenReturn(session);

        ResponseEntity<GameStateDTO> okResponse = controller.getGameState("sid");
        assertEquals(HttpStatus.OK, okResponse.getStatusCode());

        when(gameSessionService.getSession("missing")).thenThrow(new IllegalStateException("not found"));
        ResponseEntity<GameStateDTO> notFound = withLoggerSilenced(() -> controller.getGameState("missing"));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
    }

    private <T> T withLoggerSilenced(Supplier<T> supplier) {
        Logger logger = (Logger) LoggerFactory.getLogger(GameController.class);
        Level original = logger.getLevel();
        logger.setLevel(Level.OFF);
        try {
            return supplier.get();
        } finally {
            logger.setLevel(original);
        }
    }
}
