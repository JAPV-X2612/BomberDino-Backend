package com.arsw.bomberdino.controller.websocket;

import com.arsw.bomberdino.exception.InvalidMoveException;
import com.arsw.bomberdino.model.dto.request.PlaceBombRequestDTO;
import com.arsw.bomberdino.model.dto.request.PlayerMoveRequestDTO;
import com.arsw.bomberdino.model.dto.response.GameStateDTO;
import com.arsw.bomberdino.model.enums.Direction;
import com.arsw.bomberdino.service.impl.GameFacadeService;
import com.arsw.bomberdino.service.impl.GameSessionService;
import com.arsw.bomberdino.util.SequenceNumberManager;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {

    @Mock
    private GameFacadeService gameFacadeService;
    @Mock
    private GameSessionService gameSessionService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private SequenceNumberManager sequenceNumberManager;

    private WebSocketController controller;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        controller = new WebSocketController(gameFacadeService, gameSessionService,
                messagingTemplate, sequenceNumberManager);
        Logger logger = (Logger) LoggerFactory.getLogger(WebSocketController.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(WebSocketController.class);
        logger.setLevel(originalLevel);
    }

    @Test
    void handlePlayerMoveSendsErrorOnInvalidMove() {
        PlayerMoveRequestDTO request = PlayerMoveRequestDTO.builder()
                .sessionId("sid")
                .playerId("pid")
                .direction(Direction.UP)
                .timestamp(System.currentTimeMillis())
                .build();
        doThrow(new InvalidMoveException("pid", "reason"))
                .when(gameFacadeService).handlePlayerMove("sid", "pid", Direction.UP);

        controller.handlePlayerMove(request);

        verify(messagingTemplate).convertAndSendToUser(eq("pid"), eq("/queue/errors"), any());
    }

    @Test
    void handlePlayerMoveSuccessInvokesFacade() {
        PlayerMoveRequestDTO request = PlayerMoveRequestDTO.builder()
                .sessionId("sid")
                .playerId("pid")
                .direction(Direction.LEFT)
                .timestamp(System.currentTimeMillis())
                .build();

        controller.handlePlayerMove(request);

        verify(gameFacadeService).handlePlayerMove("sid", "pid", Direction.LEFT);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void handlePlaceBombSendsErrorOnFailure() {
        PlaceBombRequestDTO request = PlaceBombRequestDTO.builder()
                .sessionId("sid")
                .playerId("pid")
                .position(new Point(1, 1))
                .timestamp(System.currentTimeMillis())
                .build();
        doThrow(new IllegalStateException("fail"))
                .when(gameFacadeService).handlePlaceBomb("sid", "pid", request.getPosition());

        controller.handlePlaceBomb(request);

        verify(messagingTemplate).convertAndSendToUser(eq("pid"), eq("/queue/errors"), any());
    }

    @Test
    void handlePlaceBombSuccessInvokesFacade() {
        PlaceBombRequestDTO request = PlaceBombRequestDTO.builder()
                .sessionId("sid")
                .playerId("pid")
                .position(new Point(1, 1))
                .timestamp(System.currentTimeMillis())
                .build();

        controller.handlePlaceBomb(request);

        verify(gameFacadeService).handlePlaceBomb("sid", "pid", request.getPosition());
    }

    @Test
    void handlePowerUpCollectCoversBranches() {
        var request = com.arsw.bomberdino.model.dto.request.PowerUpCollectRequestDTO.builder()
                .sessionId("sid")
                .playerId("pid")
                .powerUpId("pup")
                .timestamp(System.currentTimeMillis())
                .build();
        controller.handlePowerUpCollect(request);
        verify(gameFacadeService).handlePowerUpCollection("sid", "pid", "pup");

        doThrow(new IllegalArgumentException("bad")).when(gameFacadeService)
                .handlePowerUpCollection("sid", "pid", "pup");
        controller.handlePowerUpCollect(request);
        verify(messagingTemplate).convertAndSendToUser(eq("pid"), eq("/queue/errors"), any());

        doThrow(new IllegalStateException("fail")).when(gameFacadeService)
                .handlePowerUpCollection("sid", "pid", "pup");
        controller.handlePowerUpCollect(request);
        verify(messagingTemplate, atLeast(2)).convertAndSendToUser(eq("pid"), eq("/queue/errors"), any());

        doThrow(new RuntimeException("boom")).when(gameFacadeService)
                .handlePowerUpCollection("sid", "pid", "pup");
        assertDoesNotThrow(() -> controller.handlePowerUpCollect(request));
    }

    @Test
    void broadcastGameStateSendsToTopic() {
        GameStateDTO state = GameStateDTO.builder()
                .sessionId("sid")
                .players(java.util.List.of())
                .build();

        controller.broadcastGameState("sid", state);

        verify(messagingTemplate).convertAndSend("/topic/game/sid/state", state);
    }

    @Test
    void broadcastGameStateSwallowsErrors() {
        GameStateDTO state = GameStateDTO.builder().sessionId("sid").players(java.util.List.of()).build();
        doThrow(new RuntimeException("fail")).when(messagingTemplate)
                .convertAndSend("/topic/game/sid/state", state);

        assertDoesNotThrow(() -> controller.broadcastGameState("sid", state));
    }

    @Test
    void handlePlayerConnectSendsInitialStateToUser() {
        GameStateDTO state = GameStateDTO.builder().sessionId("sid").build();
        when(gameFacadeService.getGameState("sid")).thenReturn(state);

        controller.onPlayerConnect("sid", "pid");

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSendToUser(eq("pid"), destinationCaptor.capture(),
                eq(state));
        assertEquals("/topic/game/sid/state", destinationCaptor.getValue());
    }

    @Test
    void handlePlayerConnectSwallowsError() {
        doThrow(new RuntimeException("boom")).when(gameFacadeService).getGameState("sid");

        assertDoesNotThrow(() -> controller.onPlayerConnect("sid", "pid"));
    }

    @Test
    void handleSessionSubscribeEventSendsInitialState() {
        GameStateDTO state = GameStateDTO.builder().sessionId("sid").players(List.of()).build();
        var session = mock(com.arsw.bomberdino.model.entity.GameSession.class);
        when(session.getCurrentState()).thenReturn(state);
        when(gameSessionService.getSession("sid")).thenReturn(session);

        var message = MessageBuilder.withPayload(new byte[0])
                .setHeader("simpDestination", "/topic/game/sid/state")
                .build();
        SessionSubscribeEvent event = new SessionSubscribeEvent(new Object(), message);

        controller.handleSessionSubscribeEvent(event);

        verify(messagingTemplate).convertAndSend("/topic/game/sid/state", state);
    }

    @Test
    void handleSessionSubscribeEventSwallowsErrors() {
        when(gameSessionService.getSession("sid")).thenThrow(new RuntimeException("boom"));
        var message = MessageBuilder.withPayload(new byte[0])
                .setHeader("simpDestination", "/topic/game/sid/state")
                .build();
        SessionSubscribeEvent event = new SessionSubscribeEvent(new Object(), message);

        controller.handleSessionSubscribeEvent(event);

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void handleSessionSubscribeEventIgnoresOtherDestinations() {
        var message = MessageBuilder.withPayload(new byte[0])
                .setHeader("simpDestination", "/topic/other")
                .build();
        SessionSubscribeEvent event = new SessionSubscribeEvent(new Object(), message);

        controller.handleSessionSubscribeEvent(event);

        verifyNoInteractions(gameSessionService);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void onPlayerDisconnectHappyPath() {
        controller.onPlayerDisconnect("sid", "pid");

        verify(gameSessionService).removePlayer("sid", "pid");
        verify(messagingTemplate).convertAndSend(eq("/topic/game/sid/disconnect"), any(Object.class));
    }

    @Test
    void onPlayerDisconnectMissingPlayer() {
        doThrow(new IllegalStateException("missing")).when(gameSessionService).removePlayer("sid", "pid");

        controller.onPlayerDisconnect("sid", "pid");

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void sendErrorToPlayerHandlesMessagingFailure() {
        PlayerMoveRequestDTO request = PlayerMoveRequestDTO.builder()
                .sessionId("sid")
                .playerId("pid")
                .direction(Direction.UP)
                .timestamp(System.currentTimeMillis())
                .build();
        doThrow(new RuntimeException("fail")).when(messagingTemplate)
                .convertAndSendToUser(eq("pid"), eq("/queue/errors"), any());
        doThrow(new InvalidMoveException("pid", "reason")).when(gameFacadeService)
                .handlePlayerMove("sid", "pid", Direction.UP);

        assertDoesNotThrow(() -> controller.handlePlayerMove(request));
    }
}
