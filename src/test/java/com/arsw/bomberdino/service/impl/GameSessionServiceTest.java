package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.exception.ValidationException;
import com.arsw.bomberdino.model.entity.GameMap;
import com.arsw.bomberdino.model.entity.GameSession;
import com.arsw.bomberdino.model.entity.Player;
import com.arsw.bomberdino.model.entity.Tile;
import com.arsw.bomberdino.model.enums.GameStatus;
import com.arsw.bomberdino.model.enums.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.Mockito;

import java.awt.Point;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameSessionServiceTest {

    @Mock
    private TileService tileService;

    @Mock
    private GameMapService gameMapService;

    @Mock
    private RedisGameStateLogger redisLogger;

    @InjectMocks
    private GameSessionService service;

    @BeforeEach
    void setup() {
        when(gameMapService.createMap(any(), anyInt(), anyInt()))
                .thenAnswer(invocation -> buildMap(invocation.getArgument(1), invocation.getArgument(2)));
    }

    @Test
    void createSessionInitializesMapTilesAndLogs() {
        var session = service.createSession("room-1", 4);

        assertNotNull(session);
        assertEquals(GameStatus.WAITING, session.getStatus());
        verify(tileService).initializeTiles(eq("room-1"), any(GameMap.class));
        verify(redisLogger).logGameState(eq("room-1"), any(), eq("SESSION_CREATED"));
        assertSame(session, service.getSession("room-1"));
    }

    @Test
    void createSessionRejectsDuplicateRoom() {
        service.createSession("room-1", 4);

        assertThrows(IllegalStateException.class, () -> service.createSession("room-1", 4));
    }

    @Test
    void createSessionValidatesInput() {
        assertThrows(ValidationException.class, () -> service.createSession(null, 4));
        assertThrows(ValidationException.class, () -> service.createSession("  ", 4));
        assertThrows(ValidationException.class, () -> service.createSession("room-1", 1));
        assertThrows(ValidationException.class, () -> service.createSession("room-1", 10));
    }

    @Test
    void startSessionTransitionsToInProgressWhenReady() {
        var session = service.createSession("room-1", 4);
        service.addPlayer("room-1", UUID.randomUUID().toString(), "p1", new Point(1, 1));
        service.addPlayer("room-1", UUID.randomUUID().toString(), "p2", new Point(2, 2));

        service.startSession("room-1");

        assertEquals(GameStatus.IN_PROGRESS, session.getStatus());
        assertNotNull(session.getStartTime());
        verify(redisLogger).logGameState("room-1", session, "SESSION_STARTED");
    }

    @Test
    void startSessionFailsWhenNotEnoughPlayersOrWrongStatus() {
        var session = service.createSession("room-1", 4);
        service.addPlayer("room-1", UUID.randomUUID().toString(), "only", new Point(1, 1));
        assertThrows(IllegalStateException.class, () -> service.startSession("room-1"));

        session.setStatus(GameStatus.FINISHED);
        session.getPlayers().add(buildPlayer("second", new Point(2, 2)));
        assertThrows(IllegalStateException.class, () -> service.startSession("room-1"));
    }

    @Test
    void pauseAndResumeRequireCorrectStatus() {
        var session = service.createSession("room-1", 4);
        session.setStatus(GameStatus.IN_PROGRESS);

        service.pauseSession("room-1");
        assertEquals(GameStatus.PAUSED, session.getStatus());

        service.resumeSession("room-1");
        assertEquals(GameStatus.IN_PROGRESS, session.getStatus());

        session.setStatus(GameStatus.WAITING);
        assertThrows(IllegalStateException.class, () -> service.pauseSession("room-1"));

        session.setStatus(GameStatus.FINISHED);
        assertThrows(IllegalStateException.class, () -> service.resumeSession("room-1"));
    }

    @Test
    void endSessionMarksFinishedAndLogs() {
        var session = service.createSession("room-1", 4);

        service.endSession("room-1");

        assertEquals(GameStatus.FINISHED, session.getStatus());
        assertNotNull(session.getEndTime());
        verify(redisLogger).logGameState("room-1", session, "SESSION_FINISHED");
    }

    @Test
    void addPlayerRequiresValidInputAndSessionWaiting() {
        service.createSession("room-1", 4);
        assertThrows(ValidationException.class,
                () -> service.addPlayer("room-1", UUID.randomUUID().toString(), "p1", null));

        var session = service.getSession("room-1");
        session.setStatus(GameStatus.IN_PROGRESS);
        assertThrows(IllegalStateException.class, () -> service.addPlayer("room-1",
                UUID.randomUUID().toString(), "p2", new Point(1, 1)));
    }

    @Test
    void addPlayerAcceptsNonUuidAndUsesNameBasedId() {
        service.createSession("room-1", 4);
        String rawId = "non-uuid";

        Player player = service.addPlayer("room-1", rawId, "p1", new Point(1, 1));

        assertEquals(UUID.nameUUIDFromBytes(rawId.getBytes()), player.getId());
    }

    @Test
    void addAndRemovePlayerManageOccupancyAndEndWhenEmpty() {
        var session = service.createSession("room-1", 4);
        var player = service.addPlayer("room-1", UUID.randomUUID().toString(), "p1",
                new Point(1, 1));

        service.removePlayer("room-1", player.getId().toString());

        verify(tileService).releaseOccupation("room-1", new Point(player.getPosX(), player.getPosY()));
        assertEquals(GameStatus.FINISHED, session.getStatus());
    }

    @Test
    void getSessionsByStatusFiltersByEnum() {
        var session1 = service.createSession("room-1", 4);
        var session2 = service.createSession("room-2", 4);
        session2.setStatus(GameStatus.IN_PROGRESS);

        List<?> waiting = service.getSessionsByStatus(GameStatus.WAITING);
        List<?> inProgress = service.getSessionsByStatus(GameStatus.IN_PROGRESS);

        assertTrue(waiting.contains(session1));
        assertFalse(waiting.contains(session2));
        assertTrue(inProgress.contains(session2));
        assertFalse(inProgress.contains(session1));

        assertThrows(ValidationException.class, () -> service.getSessionsByStatus(null));
    }

    @Test
    void clearSessionRemovesFromStorageAndClearsDependencies() {
        service.createSession("room-1", 4);

        service.clearSession("room-1");

        verify(tileService).clearSession("room-1");
        verify(gameMapService).clearSession("room-1");
        assertThrows(IllegalStateException.class, () -> service.getSession("room-1"));
    }

    @Test
    void getAffectedPlayersReturnsAlivePlayersInExplosionTiles() {
        service.createSession("room-1", 4);
        var p1 = service.addPlayer("room-1", UUID.randomUUID().toString(), "p1",
                new Point(1, 1));
        var p2 = service.addPlayer("room-1", UUID.randomUUID().toString(), "p2",
                new Point(2, 2));
        p2.setStatus(com.arsw.bomberdino.model.enums.PlayerStatus.DEAD);

        List<Point> explosion = List.of(new Point(p1.getPosX(), p1.getPosY()));

        List<String> affected = service.getAffectedPlayers("room-1", explosion);

        assertEquals(List.of(p1.getId().toString()), affected);
        assertThrows(IllegalArgumentException.class, () -> service.getAffectedPlayers("room-1", null));
        assertTrue(service.getAffectedPlayers("room-1", List.of()).isEmpty());
    }

    @Test
    void removePlayerValidatesInputAndMissingPlayer() {
        service.createSession("room-1", 4);
        assertThrows(ValidationException.class, () -> service.removePlayer(null, "pid"));
        assertThrows(ValidationException.class, () -> service.removePlayer("room-1", " "));

        assertThrows(IllegalStateException.class, () -> service.removePlayer("room-1", "missing"));
    }

    @Test
    void startSessionAllowsStartingStatus() {
        var session = service.createSession("room-1", 4);
        service.addPlayer("room-1", UUID.randomUUID().toString(), "p1", new Point(1, 1));
        service.addPlayer("room-1", UUID.randomUUID().toString(), "p2", new Point(2, 2));
        session.setStatus(GameStatus.STARTING);

        service.startSession("room-1");

        assertEquals(GameStatus.IN_PROGRESS, session.getStatus());
    }

    @Test
    void updateGameStateNoopsWhenNotInProgressAndCallsWhenInProgress() throws Exception {
        var session = service.createSession("room-1", 4);
        service.updateGameState("room-1"); // WAITING, should return

        GameSession spySession = Mockito.spy(session);
        replaceSession("room-1", spySession);
        spySession.setStatus(GameStatus.IN_PROGRESS);

        service.updateGameState("room-1");

        verify(spySession).update(anyFloat());
    }

    @Test
    void pauseResumeValidateSessionId() {
        assertThrows(ValidationException.class, () -> service.pauseSession(" "));
        assertThrows(ValidationException.class, () -> service.resumeSession(null));
    }

    @Test
    void endSessionValidatesSessionId() {
        assertThrows(ValidationException.class, () -> service.endSession(" "));
    }

    private void replaceSession(String key, GameSession session) throws Exception {
        Field field = GameSessionService.class.getDeclaredField("sessions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var map = (java.util.concurrent.ConcurrentHashMap<String, GameSession>) field.get(service);
        map.put(key, session);
    }

    private GameMap buildMap(int width, int height) {
        Tile[][] tiles = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = Tile.builder()
                        .posX(x)
                        .posY(y)
                        .type(TileType.EMPTY)
                        .occupied(false)
                        .destructible(false)
                        .hasBomb(false)
                        .createdAt(LocalDateTime.now())
                        .build();
            }
        }
        return GameMap.builder()
                .mapId(UUID.randomUUID())
                .name("map")
                .width(width)
                .height(height)
                .tiles(tiles)
                .spawnPoints(List.of(new Point(1, 1)))
                .build();
    }

    private Player buildPlayer(String username, Point spawn) {
        return Player.builder()
                .id(UUID.randomUUID())
                .username(username)
                .posX(spawn.x)
                .posY(spawn.y)
                .createdAt(LocalDateTime.now())
                .lifeCount(3)
                .bombCount(1)
                .bombRange(2)
                .speed(1)
                .status(com.arsw.bomberdino.model.enums.PlayerStatus.ALIVE)
                .spawnPoint(spawn)
                .build();
    }
}
