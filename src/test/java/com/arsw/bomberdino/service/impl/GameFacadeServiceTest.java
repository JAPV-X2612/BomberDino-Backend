package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.exception.BombPlacementException;
import com.arsw.bomberdino.exception.InvalidMoveException;
import com.arsw.bomberdino.exception.PowerUpNotFoundException;
import com.arsw.bomberdino.exception.ValidationException;
import com.arsw.bomberdino.model.dto.response.GameStateDTO;
import com.arsw.bomberdino.model.dto.response.PowerUpEffect;
import com.arsw.bomberdino.model.entity.Bomb;
import com.arsw.bomberdino.model.entity.GameSession;
import com.arsw.bomberdino.model.entity.Player;
import com.arsw.bomberdino.model.entity.PowerUp;
import com.arsw.bomberdino.model.entity.Tile;
import com.arsw.bomberdino.model.enums.Direction;
import com.arsw.bomberdino.model.enums.PlayerStatus;
import com.arsw.bomberdino.model.enums.PowerUpType;
import com.arsw.bomberdino.model.enums.TileType;
import com.arsw.bomberdino.model.event.BombExplodedEvent;
import com.arsw.bomberdino.model.event.PlayerKilledEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameFacadeServiceTest {

    @Mock
    private GameSessionService gameSessionService;
    @Mock
    private PlayerService playerService;
    @Mock
    private BombService bombService;
    @Mock
    private PowerUpService powerUpService;
    @Mock
    private CollisionService collisionService;
    @Mock
    private TileService tileService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GameFacadeService service;

    private Level originalLevel;

    @BeforeEach
    void silenceLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger(GameFacadeService.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
    }

    @AfterEach
    void restoreLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger(GameFacadeService.class);
        logger.setLevel(originalLevel);
    }

    @Test
    void handlePlayerMoveUpdatesPositionAndPublishesEvent() {
        String sessionId = "session-1";
        String playerId = UUID.randomUUID().toString();
        Player player = Player.builder()
                .id(UUID.fromString(playerId))
                .posX(0)
                .posY(0)
                .status(PlayerStatus.ALIVE)
                .spawnPoint(new Point(0, 0))
                .build();
        GameSession session = mockSessionWithPlayer(player);
        GameStateDTO state = GameStateDTO.builder().sessionId(sessionId).build();

        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        when(collisionService.canMoveTo(eq(sessionId), any(Point.class))).thenReturn(true);
        when(tileService.tryOccupy(eq(sessionId), any(Point.class), eq(false))).thenReturn(true);
        when(session.getCurrentState()).thenReturn(state);

        GameStateDTO result = service.handlePlayerMove(sessionId, playerId, Direction.RIGHT);

        assertEquals(1, player.getPosX());
        assertEquals(0, player.getPosY());
        assertSame(state, result);
        verify(eventPublisher).publishEvent(any(com.arsw.bomberdino.model.event.PlayerMovedEvent.class));
    }

    @Test
    void handlePlayerMoveThrowsWhenDestinationBlocked() {
        String sessionId = "session-1";
        String playerId = UUID.randomUUID().toString();
        Player player = Player.builder()
                .id(UUID.fromString(playerId))
                .posX(0)
                .posY(0)
                .status(PlayerStatus.ALIVE)
                .spawnPoint(new Point(0, 0))
                .build();
        GameSession session = mockSessionWithPlayer(player);

        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        when(collisionService.canMoveTo(eq(sessionId), any(Point.class))).thenReturn(false);

        assertThrows(InvalidMoveException.class,
                () -> service.handlePlayerMove(sessionId, playerId, Direction.DOWN));
    }

    @Test
    void handlePlayerMoveRejectsNulls() {
        assertThrows(ValidationException.class,
                () -> service.handlePlayerMove(null, "pid", Direction.UP));
        assertThrows(ValidationException.class,
                () -> service.handlePlayerMove("sid", null, Direction.UP));
        assertThrows(ValidationException.class,
                () -> service.handlePlayerMove("sid", "pid", null));
    }

    @Test
    void handlePlayerMoveUsesNameBasedUuidWhenIdNotParsable() {
        String rawId = "player-non-uuid";
        UUID derivedId = UUID.nameUUIDFromBytes(rawId.getBytes());
        Player player = Player.builder()
                .id(derivedId)
                .posX(0)
                .posY(0)
                .status(PlayerStatus.ALIVE)
                .spawnPoint(new Point(0, 0))
                .build();
        GameSession session = mockSessionWithPlayer(player);
        GameStateDTO state = GameStateDTO.builder().sessionId("sid").build();

        when(gameSessionService.getSession("sid")).thenReturn(session);
        when(collisionService.canMoveTo(eq("sid"), any(Point.class))).thenReturn(true);
        when(tileService.tryOccupy(eq("sid"), any(Point.class), eq(false))).thenReturn(true);
        when(session.getCurrentState()).thenReturn(state);

        GameStateDTO result = service.handlePlayerMove("sid", rawId, Direction.RIGHT);

        assertSame(state, result);
    }

    @Test
    void handlePlayerMoveThrowsWhenPlayerNotAlive() {
        String sessionId = "session-1";
        String playerId = UUID.randomUUID().toString();
        Player player = Player.builder()
                .id(UUID.fromString(playerId))
                .posX(0)
                .posY(0)
                .status(PlayerStatus.SPECTATING)
                .spawnPoint(new Point(0, 0))
                .build();
        GameSession session = mockSessionWithPlayer(player);

        when(gameSessionService.getSession(sessionId)).thenReturn(session);

        assertThrows(InvalidMoveException.class,
                () -> service.handlePlayerMove(sessionId, playerId, Direction.UP));
    }

    @Test
    void handlePlayerMoveRetriesOriginalTileWhenDestinationCannotBeOccupied() {
        String sessionId = "session-1";
        String playerId = UUID.randomUUID().toString();
        Player player = Player.builder()
                .id(UUID.fromString(playerId))
                .posX(0)
                .posY(0)
                .status(PlayerStatus.ALIVE)
                .spawnPoint(new Point(0, 0))
                .build();
        GameSession session = mockSessionWithPlayer(player);
        GameStateDTO state = GameStateDTO.builder().sessionId(sessionId).build();

        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        when(collisionService.canMoveTo(eq(sessionId), any(Point.class))).thenReturn(true);
        when(tileService.tryOccupy(eq(sessionId), any(Point.class), eq(false))).thenReturn(false);
        when(session.getCurrentState()).thenReturn(state);

        assertThrows(InvalidMoveException.class,
                () -> service.handlePlayerMove(sessionId, playerId, Direction.RIGHT));

        verify(tileService).tryOccupy(eq(sessionId), eq(new Point(0, 0)), eq(false));
    }

    @Test
    void handlePlayerMoveSkipsExpiredPowerUp() {
        String sessionId = "session-1";
        String playerId = UUID.randomUUID().toString();
        Player player = Player.builder()
                .id(UUID.fromString(playerId))
                .posX(0)
                .posY(0)
                .status(PlayerStatus.ALIVE)
                .spawnPoint(new Point(0, 0))
                .build();
        PowerUp expired = PowerUp.builder()
                .id(UUID.randomUUID())
                .posX(1)
                .posY(0)
                .type(PowerUpType.BOMB_RANGE_UP)
                .value(1)
                .spawnTime(System.currentTimeMillis() - 10_000)
                .duration(0)
                .build();
        GameSession session = mockSessionWithPlayer(player);
        session.getAvailablePowerUps().add(expired);
        GameStateDTO state = GameStateDTO.builder().sessionId(sessionId).build();

        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        when(collisionService.canMoveTo(eq(sessionId), any(Point.class))).thenReturn(true);
        when(tileService.tryOccupy(eq(sessionId), any(Point.class), eq(false))).thenReturn(true);
        when(session.getCurrentState()).thenReturn(state);

        GameStateDTO result = service.handlePlayerMove(sessionId, playerId, Direction.RIGHT);

        assertSame(state, result);
        verify(powerUpService, never()).applyPowerUpEffect(any(), any());
    }

    @Test
    void handlePlayerMoveCollectsActivePowerUp() {
        String sessionId = "session-1";
        String playerId = UUID.randomUUID().toString();
        Player player = Player.builder()
                .id(UUID.fromString(playerId))
                .posX(0)
                .posY(0)
                .status(PlayerStatus.ALIVE)
                .spawnPoint(new Point(0, 0))
                .build();
        PowerUp powerUp = PowerUp.builder()
                .id(UUID.randomUUID())
                .posX(1)
                .posY(0)
                .type(PowerUpType.BOMB_COUNT_UP)
                .value(1)
                .spawnTime(System.currentTimeMillis())
                .duration(10_000)
                .build();
        GameSession session = mockSessionWithPlayer(player);
        session.getAvailablePowerUps().add(powerUp);
        GameStateDTO state = GameStateDTO.builder().sessionId(sessionId).build();

        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        when(collisionService.canMoveTo(eq(sessionId), any(Point.class))).thenReturn(true);
        when(tileService.tryOccupy(eq(sessionId), any(Point.class), eq(false))).thenReturn(true);
        when(session.getCurrentState()).thenReturn(state);
        GameFacadeService spyService = spy(service);
        doReturn(state).when(spyService).handlePowerUpCollection(sessionId, playerId,
                powerUp.getId().toString());

        GameStateDTO result = spyService.handlePlayerMove(sessionId, playerId, Direction.RIGHT);

        assertSame(state, result);
        verify(spyService).handlePowerUpCollection(sessionId, playerId, powerUp.getId().toString());
    }

    @Test
    void handlePlaceBombPlacesBombAndPublishesEvent() {
        String sessionId = "session-1";
        String playerId = UUID.randomUUID().toString();
        Point pos = new Point(1, 1);
        Player player = Player.builder()
                .id(UUID.fromString(playerId))
                .posX(pos.x)
                .posY(pos.y)
                .status(PlayerStatus.ALIVE)
                .bombRange(2)
                .spawnPoint(pos)
                .build();
        GameSession session = mock(GameSession.class);
        List<Player> players = new ArrayList<>();
        players.add(player);
        List<Bomb> activeBombs = new ArrayList<>();

        when(session.getPlayers()).thenReturn(players);
        when(session.getActiveBombs()).thenReturn(activeBombs);
        when(session.getCurrentState()).thenReturn(GameStateDTO.builder().sessionId(sessionId).build());
        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        Tile tile = Tile.builder()
                .posX(pos.x)
                .posY(pos.y)
                .type(TileType.EMPTY)
                .occupied(false)
                .destructible(false)
                .hasBomb(false)
                .build();
        when(tileService.getTile(sessionId, pos)).thenReturn(tile);
        Bomb bomb = Bomb.builder()
                .id(UUID.randomUUID())
                .posX(pos.x)
                .posY(pos.y)
                .range(2)
                .state(com.arsw.bomberdino.model.enums.BombState.PLACED)
                .placedTime(System.currentTimeMillis())
                .explosionDelay(50L)
                .build();
        when(bombService.placeBomb(sessionId, playerId, pos, player.getBombRange())).thenReturn(bomb);

        GameStateDTO result = service.handlePlaceBomb(sessionId, playerId, pos);

        assertTrue(activeBombs.contains(bomb));
        assertNotNull(result);
        verify(eventPublisher).publishEvent(any(com.arsw.bomberdino.model.event.BombPlacedEvent.class));
    }

    @Test
    void handlePlaceBombRejectsWhenTileHasBomb() {
        String sessionId = "session-1";
        String playerId = UUID.randomUUID().toString();
        Point pos = new Point(1, 1);
        Player player = Player.builder()
                .id(UUID.fromString(playerId))
                .posX(pos.x)
                .posY(pos.y)
                .status(PlayerStatus.ALIVE)
                .bombRange(2)
                .spawnPoint(pos)
                .build();
        GameSession session = mockSessionWithPlayer(player);
        Tile tile = Tile.builder()
                .posX(pos.x)
                .posY(pos.y)
                .type(TileType.EMPTY)
                .occupied(false)
                .destructible(false)
                .hasBomb(true)
                .build();

        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        when(tileService.getTile(sessionId, pos)).thenReturn(tile);

        assertThrows(BombPlacementException.class,
                () -> service.handlePlaceBomb(sessionId, playerId, pos));
    }

    @Test
    void handlePlaceBombRejectsWhenPlayerDead() {
        String sessionId = "session-1";
        String playerId = UUID.randomUUID().toString();
        Point pos = new Point(1, 1);
        Player player = Player.builder()
                .id(UUID.fromString(playerId))
                .posX(pos.x)
                .posY(pos.y)
                .status(PlayerStatus.DEAD)
                .bombRange(2)
                .spawnPoint(pos)
                .build();
        GameSession session = mockSessionWithPlayer(player);
        Tile tile = Tile.builder()
                .posX(pos.x)
                .posY(pos.y)
                .type(TileType.EMPTY)
                .occupied(false)
                .destructible(false)
                .hasBomb(false)
                .build();

        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        when(tileService.getTile(sessionId, pos)).thenReturn(tile);

        assertThrows(BombPlacementException.class,
                () -> service.handlePlaceBomb(sessionId, playerId, pos));
    }

    @Test
    void handlePlaceBombRejectsWhenPositionDoesNotMatchPlayer() {
        String sessionId = "session-1";
        String playerId = UUID.randomUUID().toString();
        Point playerPos = new Point(0, 0);
        Point requestedPos = new Point(1, 1);
        Player player = Player.builder()
                .id(UUID.fromString(playerId))
                .posX(playerPos.x)
                .posY(playerPos.y)
                .status(PlayerStatus.ALIVE)
                .bombRange(2)
                .spawnPoint(playerPos)
                .build();
        GameSession session = mockSessionWithPlayer(player);
        Tile tile = Tile.builder()
                .posX(requestedPos.x)
                .posY(requestedPos.y)
                .type(TileType.EMPTY)
                .occupied(false)
                .destructible(false)
                .hasBomb(false)
                .build();

        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        when(tileService.getTile(sessionId, requestedPos)).thenReturn(tile);

        assertThrows(BombPlacementException.class,
                () -> service.handlePlaceBomb(sessionId, playerId, requestedPos));
    }

    @Test
    void handlePlaceBombRejectsNullPosition() {
        assertThrows(ValidationException.class,
                () -> service.handlePlaceBomb("sid", "pid", null));
    }

    @Test
    void handlePlaceBombFailsWhenServiceReturnsNull() {
        String sessionId = "session-1";
        String playerId = UUID.randomUUID().toString();
        Point pos = new Point(1, 1);
        Player player = Player.builder()
                .id(UUID.fromString(playerId))
                .posX(pos.x)
                .posY(pos.y)
                .status(PlayerStatus.ALIVE)
                .bombRange(2)
                .spawnPoint(pos)
                .build();
        GameSession session = mockSessionWithPlayer(player);
        Tile tile = Tile.builder()
                .posX(pos.x)
                .posY(pos.y)
                .type(TileType.EMPTY)
                .occupied(false)
                .destructible(false)
                .hasBomb(false)
                .build();

        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        when(tileService.getTile(sessionId, pos)).thenReturn(tile);
        when(bombService.placeBomb(sessionId, playerId, pos, player.getBombRange())).thenReturn(null);

        assertThrows(BombPlacementException.class,
                () -> service.handlePlaceBomb(sessionId, playerId, pos));
    }

    @Test
    void handlePowerUpCollectionAppliesEffectAndPublishesEvent() {
        String sessionId = "session-1";
        String playerId = UUID.randomUUID().toString();
        PowerUp powerUp = PowerUp.builder()
                .id(UUID.randomUUID())
                .posX(1)
                .posY(1)
                .spawnTime(System.currentTimeMillis())
                .duration(30_000)
                .type(PowerUpType.BOMB_COUNT_UP)
                .build();
        List<PowerUp> available = new ArrayList<>(List.of(powerUp));
        GameSession session = mock(GameSession.class);
        when(session.getAvailablePowerUps()).thenReturn(available);
        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        PowerUpEffect effect = PowerUpEffect.builder()
                .type(powerUp.getType())
                .duration(5)
                .multiplier(1.0f)
                .build();
        when(powerUpService.applyPowerUpEffect(playerId, powerUp.getId().toString())).thenReturn(effect);

        GameStateDTO state = GameStateDTO.builder().sessionId(sessionId).build();
        when(session.getCurrentState()).thenReturn(state);

        GameStateDTO result = service.handlePowerUpCollection(sessionId, playerId,
                powerUp.getId().toString());

        assertSame(state, result);
        assertTrue(available.isEmpty());
        verify(playerService).applyPowerUpEffect(playerId, effect);
        verify(tileService).releaseOccupation(eq(sessionId), any(Point.class));
        verify(eventPublisher).publishEvent(any(com.arsw.bomberdino.model.event.PowerUpCollectedEvent.class));
    }

    @Test
    void handlePowerUpCollectionValidatesInputs() {
        assertThrows(ValidationException.class,
                () -> service.handlePowerUpCollection(null, "pid", "power"));
        assertThrows(ValidationException.class,
                () -> service.handlePowerUpCollection("sid", " ", "power"));
        assertThrows(ValidationException.class,
                () -> service.handlePowerUpCollection("sid", "pid", ""));
        assertThrows(ValidationException.class,
                () -> service.handlePowerUpCollection("sid", "pid", null));
    }

    @Test
    void handlePowerUpCollectionThrowsWhenNotFound() {
        GameSession session = mock(GameSession.class);
        when(session.getAvailablePowerUps()).thenReturn(new ArrayList<>());
        when(gameSessionService.getSession("sid")).thenReturn(session);

        assertThrows(PowerUpNotFoundException.class,
                () -> service.handlePowerUpCollection("sid", "pid", "missing"));
    }

    @Test
    void handlePowerUpCollectionRejectsExpiredPowerUp() {
        String sessionId = "session-1";
        String playerId = UUID.randomUUID().toString();
        PowerUp powerUp = PowerUp.builder()
                .id(UUID.randomUUID())
                .posX(1)
                .posY(1)
                .spawnTime(System.currentTimeMillis() - 10_000)
                .duration(0)
                .type(PowerUpType.SPEED_UP)
                .value(1)
                .build();
        GameSession session = mock(GameSession.class);
        List<PowerUp> available = new ArrayList<>(List.of(powerUp));

        when(session.getAvailablePowerUps()).thenReturn(available);
        when(gameSessionService.getSession(sessionId)).thenReturn(session);

        assertThrows(PowerUpNotFoundException.class, () -> service.handlePowerUpCollection(
                sessionId, playerId, powerUp.getId().toString()));
    }

    @Test
    void getGameStateValidatesSessionId() {
        assertThrows(ValidationException.class, () -> service.getGameState(" "));
    }

    @Test
    void processBombExplosionRespawnsAlivePlayers() throws Exception {
        String sessionId = "sid";
        Bomb bomb = Bomb.builder()
                .id(UUID.randomUUID())
                .posX(0)
                .posY(0)
                .range(2)
                .build();
        List<Bomb> activeBombs = new ArrayList<>(List.of(bomb));

        Player wounded = Player.builder()
                .id(UUID.randomUUID())
                .posX(0)
                .posY(0)
                .lifeCount(2)
                .deaths(0)
                .status(PlayerStatus.ALIVE)
                .spawnPoint(new Point(5, 5))
                .build();
        List<Player> players = new ArrayList<>(List.of(wounded));

        GameSession session = mock(GameSession.class);
        when(session.getPlayers()).thenReturn(players);
        when(session.getActiveBombs()).thenReturn(activeBombs);

        List<Point> affectedTiles = List.of(new Point(0, 0));
        when(collisionService.handleBombExplosion(sessionId, bomb.getId().toString(), bomb.getRange()))
                .thenReturn(affectedTiles);
        when(gameSessionService.getAffectedPlayers(sessionId, affectedTiles))
                .thenReturn(List.of(wounded.getId().toString()));
        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        when(tileService.tryOccupy(eq(sessionId), eq(new Point(5, 5)), eq(false))).thenReturn(true);

        Method process = GameFacadeService.class.getDeclaredMethod("processBombExplosion", String.class, Bomb.class);
        process.setAccessible(true);

        process.invoke(service, sessionId, bomb);

        assertTrue(activeBombs.isEmpty());
        assertEquals(new Point(5, 5), new Point(wounded.getPosX(), wounded.getPosY()));
        verify(tileService).releaseOccupation(eq(sessionId), eq(new Point(0, 0)));
        verify(tileService).tryOccupy(eq(sessionId), eq(new Point(5, 5)), eq(false));
        verify(eventPublisher, never()).publishEvent(any(PlayerKilledEvent.class));
        verify(eventPublisher).publishEvent(any(BombExplodedEvent.class));
    }

    @Test
    void processBombExplosionRespawnFailsToOccupySpawnStillCompletes() throws Exception {
        String sessionId = "sid";
        Bomb bomb = Bomb.builder()
                .id(UUID.randomUUID())
                .posX(0)
                .posY(0)
                .range(2)
                .build();
        List<Bomb> activeBombs = new ArrayList<>(List.of(bomb));

        Player wounded = Player.builder()
                .id(UUID.randomUUID())
                .posX(0)
                .posY(0)
                .lifeCount(2)
                .deaths(0)
                .status(PlayerStatus.ALIVE)
                .spawnPoint(new Point(5, 5))
                .build();
        List<Player> players = new ArrayList<>(List.of(wounded));

        GameSession session = mock(GameSession.class);
        when(session.getPlayers()).thenReturn(players);
        when(session.getActiveBombs()).thenReturn(activeBombs);

        List<Point> affectedTiles = List.of(new Point(0, 0));
        when(collisionService.handleBombExplosion(sessionId, bomb.getId().toString(), bomb.getRange()))
                .thenReturn(affectedTiles);
        when(gameSessionService.getAffectedPlayers(sessionId, affectedTiles))
                .thenReturn(List.of(wounded.getId().toString()));
        when(gameSessionService.getSession(sessionId)).thenReturn(session);
        when(tileService.tryOccupy(eq(sessionId), eq(new Point(5, 5)), eq(false))).thenReturn(false);

        Method process = GameFacadeService.class.getDeclaredMethod("processBombExplosion", String.class, Bomb.class);
        process.setAccessible(true);

        assertDoesNotThrow(() -> process.invoke(service, sessionId, bomb));

        verify(tileService).releaseOccupation(eq(sessionId), eq(new Point(0, 0)));
        verify(tileService).tryOccupy(eq(sessionId), eq(new Point(5, 5)), eq(false));
        verify(eventPublisher, never()).publishEvent(any(PlayerKilledEvent.class));
        assertTrue(activeBombs.isEmpty());
    }

    @Test
    void checkForGameEndDoesNotEndWhenMultipleAlive() throws Exception {
        String sessionId = "sid";
        Player p1 = Player.builder()
                .id(UUID.randomUUID())
                .lifeCount(2)
                .deaths(0)
                .status(PlayerStatus.ALIVE)
                .build();
        Player p2 = Player.builder()
                .id(UUID.randomUUID())
                .lifeCount(2)
                .deaths(1)
                .status(PlayerStatus.ALIVE)
                .build();
        GameSession session = mock(GameSession.class);
        when(session.getPlayers()).thenReturn(List.of(p1, p2));
        when(gameSessionService.getSession(sessionId)).thenReturn(session);

        Method checkEnd = GameFacadeService.class.getDeclaredMethod("checkForGameEnd", String.class);
        checkEnd.setAccessible(true);

        checkEnd.invoke(service, sessionId);

        verify(gameSessionService, never()).endSession(sessionId);
    }

    @Test
    void processBombExplosionSkipsMissingPlayers() throws Exception {
        String sessionId = "sid";
        Bomb bomb = Bomb.builder()
                .id(UUID.randomUUID())
                .posX(0)
                .posY(0)
                .range(2)
                .build();
        List<Bomb> activeBombs = new ArrayList<>(List.of(bomb));

        GameSession session = mock(GameSession.class);
        when(session.getPlayers()).thenReturn(new ArrayList<>());
        when(session.getActiveBombs()).thenReturn(activeBombs);

        List<Point> affectedTiles = List.of(new Point(0, 0));
        when(collisionService.handleBombExplosion(sessionId, bomb.getId().toString(), bomb.getRange()))
                .thenReturn(affectedTiles);
        when(gameSessionService.getAffectedPlayers(sessionId, affectedTiles))
                .thenReturn(List.of("missing-player"));
        when(gameSessionService.getSession(sessionId)).thenReturn(session);

        Method process = GameFacadeService.class.getDeclaredMethod("processBombExplosion", String.class, Bomb.class);
        process.setAccessible(true);

        process.invoke(service, sessionId, bomb);

        assertTrue(activeBombs.isEmpty());
        verify(tileService, never()).releaseOccupation(eq(sessionId), any(Point.class));
        verify(eventPublisher).publishEvent(any(BombExplodedEvent.class));
    }

    @Test
    void handlePlayerDeathIncrementsKillerAndPublishes() throws Exception {
        String sessionId = "sid";
        String killerId = "killer";
        String victimId = "victim";

        Player killer = Player.builder().id(UUID.randomUUID()).lifeCount(1).deaths(0)
                .status(PlayerStatus.ALIVE).spawnPoint(new Point(0, 0)).build();
        Player victim = Player.builder().id(UUID.randomUUID()).lifeCount(1).deaths(1)
                .status(PlayerStatus.DEAD).spawnPoint(new Point(1, 1)).build();
        GameSession session = mock(GameSession.class);
        when(session.getPlayers()).thenReturn(List.of(killer, victim));
        when(gameSessionService.getSession(sessionId)).thenReturn(session);

        Method death = GameFacadeService.class.getDeclaredMethod("handlePlayerDeath", String.class, String.class, String.class);
        death.setAccessible(true);

        death.invoke(service, sessionId, killerId, victimId);

        verify(playerService).incrementKills(killerId);
        verify(eventPublisher).publishEvent(any(PlayerKilledEvent.class));
        verify(gameSessionService).endSession(sessionId);
    }

    @Test
    void detectAndCollectPowerUpReturnsNullWhenNoMatch() throws Exception {
        GameSession session = mock(GameSession.class);
        PowerUp powerUp = PowerUp.builder()
                .id(UUID.randomUUID())
                .posX(2)
                .posY(2)
                .type(PowerUpType.BOMB_RANGE_UP)
                .value(1)
                .spawnTime(System.currentTimeMillis())
                .duration(10_000)
                .build();
        when(session.getAvailablePowerUps()).thenReturn(new ArrayList<>(List.of(powerUp)));

        Method detect = GameFacadeService.class.getDeclaredMethod("detectAndCollectPowerUp", GameSession.class, Point.class);
        detect.setAccessible(true);

        Object result = detect.invoke(service, session, new Point(0, 0));

        assertNull(result);
    }

    @Test
    void checkForGameEndEndsWhenNoAlivePlayers() throws Exception {
        String sessionId = "sid";
        Player dead1 = Player.builder().id(UUID.randomUUID()).lifeCount(1).deaths(1).status(PlayerStatus.SPECTATING).build();
        Player dead2 = Player.builder().id(UUID.randomUUID()).lifeCount(1).deaths(2).status(PlayerStatus.SPECTATING).build();
        GameSession session = mock(GameSession.class);
        when(session.getPlayers()).thenReturn(List.of(dead1, dead2));
        when(gameSessionService.getSession(sessionId)).thenReturn(session);

        Method checkEnd = GameFacadeService.class.getDeclaredMethod("checkForGameEnd", String.class);
        checkEnd.setAccessible(true);

        checkEnd.invoke(service, sessionId);

        verify(gameSessionService).endSession(sessionId);
    }

    @Test
    void scheduleBombExplosionCatchesExceptionsFromProcessing() throws Exception {
        String sessionId = "sid";
        Bomb bomb = Bomb.builder().id(UUID.randomUUID()).explosionDelay(0L).build();
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        when(executor.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenAnswer(inv -> {
            Runnable r = inv.getArgument(0);
            r.run();
            return new ImmediateScheduledFuture();
        });

        Field field = GameFacadeService.class.getDeclaredField("explosionScheduler");
        field.setAccessible(true);
        field.set(service, executor);

        when(collisionService.handleBombExplosion(anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("boom"));

        Method schedule = GameFacadeService.class.getDeclaredMethod("scheduleBombExplosion", String.class, Bomb.class);
        schedule.setAccessible(true);

        assertDoesNotThrow(() -> schedule.invoke(service, sessionId, bomb));
        verify(executor).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    private static class ImmediateScheduledFuture implements ScheduledFuture<Object> {
        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(java.util.concurrent.Delayed o) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            return null;
        }
    }


    @Test
    void processBombExplosionHandlesDeathAndEndsGame() throws Exception {
        String sessionId = "sid";
        Bomb bomb = Bomb.builder()
                .id(UUID.randomUUID())
                .posX(0)
                .posY(0)
                .range(2)
                .build();
        List<Bomb> activeBombs = new ArrayList<>(List.of(bomb));

        Player victim = Player.builder()
                .id(UUID.randomUUID())
                .posX(0)
                .posY(0)
                .lifeCount(1)
                .deaths(0)
                .status(PlayerStatus.ALIVE)
                .spawnPoint(new Point(0, 0))
                .build();
        Player survivor = Player.builder()
                .id(UUID.randomUUID())
                .posX(5)
                .posY(5)
                .lifeCount(1)
                .deaths(0)
                .status(PlayerStatus.ALIVE)
                .spawnPoint(new Point(5, 5))
                .build();
        List<Player> players = new ArrayList<>(List.of(victim, survivor));

        GameSession session = mock(GameSession.class);
        when(session.getPlayers()).thenReturn(players);
        when(session.getActiveBombs()).thenReturn(activeBombs);

        List<Point> affectedTiles = List.of(new Point(0, 0));
        when(collisionService.handleBombExplosion(sessionId, bomb.getId().toString(), bomb.getRange()))
                .thenReturn(affectedTiles);
        when(gameSessionService.getAffectedPlayers(sessionId, affectedTiles))
                .thenReturn(List.of(victim.getId().toString()));
        when(gameSessionService.getSession(sessionId)).thenReturn(session);

        Method process = GameFacadeService.class.getDeclaredMethod("processBombExplosion", String.class, Bomb.class);
        process.setAccessible(true);

        assertDoesNotThrow(() -> process.invoke(service, sessionId, bomb));

        assertTrue(activeBombs.isEmpty());
        verify(tileService).applyExplosionToTile(eq(sessionId), eq(new Point(0, 0)));
        verify(tileService).markBomb(eq(sessionId), eq(new Point(0, 0)), eq(false));
        verify(tileService).releaseOccupation(eq(sessionId), eq(new Point(0, 0)));
        verify(eventPublisher, atLeastOnce()).publishEvent(any(PlayerKilledEvent.class));
        verify(eventPublisher, atLeastOnce()).publishEvent(any(BombExplodedEvent.class));
        verify(gameSessionService).endSession(sessionId);
    }

    @Test
    void processBombExplosionSkipsShieldedPlayers() throws Exception {
        String sessionId = "sid";
        Bomb bomb = Bomb.builder()
                .id(UUID.randomUUID())
                .posX(0)
                .posY(0)
                .range(2)
                .build();
        List<Bomb> activeBombs = new ArrayList<>(List.of(bomb));

        PowerUp shield = PowerUp.builder()
                .id(UUID.randomUUID())
                .posX(0)
                .posY(0)
                .type(PowerUpType.TEMPORARY_SHIELD)
                .value(1)
                .spawnTime(System.currentTimeMillis())
                .duration(10_000)
                .build();
        Player shielded = Player.builder()
                .id(UUID.randomUUID())
                .posX(0)
                .posY(0)
                .lifeCount(1)
                .deaths(0)
                .status(PlayerStatus.ALIVE)
                .spawnPoint(new Point(0, 0))
                .activePowerUps(new ArrayList<>(List.of(shield)))
                .build();
        List<Player> players = new ArrayList<>(List.of(shielded));

        GameSession session = mock(GameSession.class);
        when(session.getPlayers()).thenReturn(players);
        when(session.getActiveBombs()).thenReturn(activeBombs);

        List<Point> affectedTiles = List.of(new Point(0, 0));
        when(collisionService.handleBombExplosion(sessionId, bomb.getId().toString(), bomb.getRange()))
                .thenReturn(affectedTiles);
        when(gameSessionService.getAffectedPlayers(sessionId, affectedTiles))
                .thenReturn(List.of(shielded.getId().toString()));
        when(gameSessionService.getSession(sessionId)).thenReturn(session);

        Method process = GameFacadeService.class.getDeclaredMethod("processBombExplosion", String.class, Bomb.class);
        process.setAccessible(true);

        process.invoke(service, sessionId, bomb);

        assertTrue(activeBombs.isEmpty());
        verify(tileService, never()).releaseOccupation(eq(sessionId), any(Point.class));
        verify(eventPublisher).publishEvent(any(BombExplodedEvent.class));
    }

    private GameSession mockSessionWithPlayer(Player player) {
        GameSession session = mock(GameSession.class);
        when(session.getPlayers()).thenReturn(new ArrayList<>(List.of(player)));
        when(session.getActiveBombs()).thenReturn(new ArrayList<>());
        when(session.getAvailablePowerUps()).thenReturn(new ArrayList<>());
        when(session.getSessionId()).thenReturn(UUID.randomUUID());
        when(session.getCurrentState()).thenReturn(GameStateDTO.builder().sessionId("sid").build());
        return session;
    }
}
