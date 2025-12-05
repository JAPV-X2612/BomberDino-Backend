package com.arsw.bomberdino.model.entity;

import com.arsw.bomberdino.model.enums.GameStatus;
import com.arsw.bomberdino.model.enums.PlayerStatus;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionEntityTest {

    private GameSession newSession() {
        Tile[][] tiles = new Tile[2][2];
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                tiles[y][x] = Tile.builder().posX(x).posY(y).type(com.arsw.bomberdino.model.enums.TileType.EMPTY)
                        .destructible(false).occupied(false).hasBomb(false).build();
            }
        }
        GameMap map = GameMap.builder()
                .mapId(UUID.randomUUID())
                .name("map")
                .width(2)
                .height(2)
                .tiles(tiles)
                .spawnPoints(new ArrayList<>(java.util.List.of(new Point(0, 0))))
                .build();
        return GameSession.builder()
                .sessionId(UUID.randomUUID())
                .status(GameStatus.WAITING)
                .map(map)
                .players(new ArrayList<>())
                .activeBombs(new ArrayList<>())
                .activeExplosions(new ArrayList<>())
                .availablePowerUps(new ArrayList<>())
                .roundDuration(180)
                .build();
    }

    private Player alivePlayer(Point pos) {
        return Player.builder()
                .id(UUID.randomUUID())
                .username("p")
                .posX(pos.x)
                .posY(pos.y)
                .lifeCount(1)
                .bombCount(1)
                .bombRange(1)
                .speed(1)
                .status(PlayerStatus.ALIVE)
                .spawnPoint(pos)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void startValidatesStatus() {
        GameSession session = newSession();
        session.addPlayer(alivePlayer(new Point(0, 0)));
        session.start();
        assertEquals(GameStatus.IN_PROGRESS, session.getStatus());
        assertNotNull(session.getStartTime());

        assertThrows(IllegalStateException.class, session::start);
    }

    @Test
    void addRemovePlayerAndEndSession() {
        GameSession session = newSession();
        Player player = alivePlayer(new Point(0, 0));
        session.addPlayer(player);
        assertEquals(1, session.getPlayers().size());

        session.removePlayer(player);
        assertEquals(GameStatus.FINISHED, session.getStatus());
        assertNotNull(session.getEndTime());
    }

    @Test
    void updateSkipsWhenNotInProgress() {
        GameSession session = newSession();
        session.update(0.1f); // WAITING, no crash
        session.setStatus(GameStatus.IN_PROGRESS);
        session.update(0.1f);
        assertTrue(session.getStatus() == GameStatus.IN_PROGRESS || session.getStatus() == GameStatus.FINISHED);
    }

    @Test
    void updateKeepsSessionRunningWhenNoWinConditionMet() {
        GameSession session = newSession();
        session.setStatus(GameStatus.IN_PROGRESS);
        session.getPlayers().add(alivePlayer(new Point(0, 0)));
        session.getPlayers().add(alivePlayer(new Point(1, 0)));

        session.update(0.016f);

        assertEquals(GameStatus.IN_PROGRESS, session.getStatus());
    }

    @Test
    void updateProcessesExpiredBombsAndDamagesPlayers() {
        GameSession session = newSession();
        session.setStatus(GameStatus.IN_PROGRESS);

        // player at bomb position
        Player victim = alivePlayer(new Point(0, 0));
        session.getPlayers().add(victim);

        Bomb bomb = Bomb.builder()
                .id(UUID.randomUUID())
                .posX(0)
                .posY(0)
                .range(1)
                .state(com.arsw.bomberdino.model.enums.BombState.PLACED)
                .placedTime(System.currentTimeMillis() - 10_000)
                .explosionDelay(1000)
                .build();
        bomb.initDefaults();
        session.getActiveBombs().add(bomb);

        session.update(0.016f);

        assertTrue(session.getActiveBombs().isEmpty());
        assertFalse(victim.isAlive());
        assertFalse(session.getActiveExplosions().isEmpty());
    }

    @Test
    void updateRemovesExpiredExplosionsAndPowerUpsAndChecksWin() {
        GameSession session = newSession();
        session.setStatus(GameStatus.IN_PROGRESS);

        // add two alive players to avoid immediate end until we set deaths
        Player p1 = alivePlayer(new Point(0, 0));
        Player p2 = alivePlayer(new Point(1, 0));
        session.getPlayers().add(p1);
        session.getPlayers().add(p2);

        Explosion explosion = Explosion.builder()
                .posX(0).posY(0)
                .duration(1L)
                .damage(1)
                .affectedTiles(new ArrayList<>())
                .build();
        explosion.initDefaults();
        explosion.setCreatedAt(explosion.getCreatedAt().minusSeconds(5)); // force expired
        session.getActiveExplosions().add(explosion);

        PowerUp powerUp = PowerUp.builder()
                .id(UUID.randomUUID())
                .posX(0)
                .posY(1)
                .type(com.arsw.bomberdino.model.enums.PowerUpType.BOMB_COUNT_UP)
                .spawnTime(System.currentTimeMillis() - 10_000)
                .duration(0)
                .build();
        powerUp.initDefaults();
        session.getAvailablePowerUps().add(powerUp);

        // mark one player dead so win condition will finish session after update
        p2.setStatus(PlayerStatus.DEAD);
        p2.setDeaths(p2.getLifeCount());

        session.update(0.016f);

        assertTrue(session.getActiveExplosions().isEmpty());
        assertTrue(session.getAvailablePowerUps().isEmpty());
        assertEquals(GameStatus.FINISHED, session.getStatus());
    }

    @Test
    void processExpiredBombsSkipsNotReadyBombs() {
        GameSession session = newSession();
        session.setStatus(GameStatus.IN_PROGRESS);
        Bomb bomb = Bomb.builder()
                .id(UUID.randomUUID())
                .posX(0).posY(0)
                .range(1)
                .state(com.arsw.bomberdino.model.enums.BombState.PLACED)
                .placedTime(System.currentTimeMillis())
                .explosionDelay(1_000_000) // far future
                .build();
        bomb.initDefaults();
        session.getActiveBombs().add(bomb);

        session.update(0.016f);

        assertFalse(session.getActiveBombs().isEmpty());
        assertTrue(session.getActiveExplosions().isEmpty());
    }

    @Test
    void removePlayerDoesNotEndWhenStillMoreThanOneAlive() {
        GameSession session = newSession();
        Player p1 = alivePlayer(new Point(0, 0));
        Player p2 = alivePlayer(new Point(1, 0));
        Player p3 = alivePlayer(new Point(0, 1));
        session.getPlayers().add(p1);
        session.getPlayers().add(p2);
        session.getPlayers().add(p3);

        session.removePlayer(p1);
        assertNotEquals(GameStatus.FINISHED, session.getStatus());

        session.removePlayer(p2);
        assertEquals(GameStatus.FINISHED, session.getStatus());
    }

    @Test
    void damagePlayersInExplosionHandlesEmptyList() throws Exception {
        GameSession session = newSession();
        Player p = alivePlayer(new Point(0, 0));
        session.getPlayers().add(p);
        Explosion explosion = Explosion.builder()
                .affectedTiles(new ArrayList<>())
                .damage(1)
                .posX(0).posY(0)
                .duration(100L)
                .build();
        explosion.initDefaults();

        Method damage = GameSession.class.getDeclaredMethod("damagePlayersInExplosion", Explosion.class);
        damage.setAccessible(true);
        damage.invoke(session, explosion);

        assertEquals(PlayerStatus.ALIVE, p.getStatus());
    }

    @Test
    void mapTilesToDtoHandlesEmptyGrid() {
        GameSession session = newSession();
        session.getMap().setTiles(new Tile[0][0]);
        session.getMap().setWidth(0);
        session.getMap().setHeight(0);

        assertNotNull(session.getCurrentState().getTiles());
    }

    @Test
    void getWinnerReturnsSingleAlive() {
        GameSession session = newSession();
        Player alive = alivePlayer(new Point(0, 0));
        Player dead = alivePlayer(new Point(1, 0));
        dead.setStatus(PlayerStatus.DEAD);
        dead.setDeaths(dead.getLifeCount()); // ensure isAlive false
        session.getPlayers().add(alive);
        session.getPlayers().add(dead);

        assertEquals(alive, session.getWinner());

        // when multiple alive players, no single winner
        session.getPlayers().add(alivePlayer(new Point(1, 1)));
        assertNull(session.getWinner());
    }

    @Test
    void getCurrentStateMapsFields() {
        GameSession session = newSession();
        session.getPlayers().add(alivePlayer(new Point(0, 0)));
        var state = session.getCurrentState();
        assertEquals(session.getSessionId().toString(), state.getSessionId());
        assertEquals(1, state.getPlayers().size());
    }
}
