package com.arsw.bomberdino.model.entity;

import com.arsw.bomberdino.model.enums.Direction;
import com.arsw.bomberdino.model.enums.PlayerStatus;
import com.arsw.bomberdino.model.enums.PowerUpType;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    private Player newPlayer(Point spawn) {
        return Player.builder()
                .id(java.util.UUID.randomUUID())
                .username("u")
                .posX(spawn.x)
                .posY(spawn.y)
                .lifeCount(2)
                .bombCount(1)
                .bombRange(2)
                .speed(1)
                .status(PlayerStatus.ALIVE)
                .spawnPoint(spawn)
                .activePowerUps(new ArrayList<>())
                .currentBombsPlaced(new ArrayList<>())
                .build();
    }

    @Test
    void moveUpdatesPositionWhenAlive() {
        Player player = newPlayer(new Point(0, 0));
        player.move(Direction.RIGHT);
        assertEquals(1, player.getPosX());
        assertEquals(0, player.getPosY());

        player.move(Direction.LEFT); // back to origin
        assertEquals(0, player.getPosX());
        assertEquals(0, player.getPosY());
    }

    @Test
    void moveThrowsWhenDeadOrNullDirection() {
        Player player = newPlayer(new Point(0, 0));
        player.setStatus(PlayerStatus.DEAD);
        assertThrows(IllegalStateException.class, () -> player.move(Direction.UP));
        assertThrows(IllegalArgumentException.class, () -> player.move(null));
    }

    @Test
    void applyPowerUpAddsShieldAndStats() {
        Player player = newPlayer(new Point(0, 0));
        PowerUp shield = PowerUp.builder()
                .type(PowerUpType.TEMPORARY_SHIELD)
                .value(1)
                .posX(0)
                .posY(0)
                .spawnTime(System.currentTimeMillis())
                .duration(10_000)
                .build();
        shield.initDefaults();

        player.applyPowerUp(shield);

        assertTrue(player.hasActiveShield());

        PowerUp rangeUp = PowerUp.builder()
                .type(PowerUpType.BOMB_RANGE_UP)
                .value(1)
                .posX(0).posY(0)
                .spawnTime(System.currentTimeMillis())
                .duration(10_000)
                .build();
        rangeUp.initDefaults();
        player.applyPowerUp(rangeUp);
        assertEquals(3, player.getBombRange());

        PowerUp extraLife = PowerUp.builder()
                .type(PowerUpType.EXTRA_LIFE)
                .value(1)
                .posX(0)
                .posY(0)
                .spawnTime(System.currentTimeMillis())
                .duration(10_000)
                .build();
        extraLife.initDefaults();
        player.applyPowerUp(extraLife);
        assertEquals(3, player.getLifeCount());

        PowerUp speedUp = PowerUp.builder()
                .type(PowerUpType.SPEED_UP)
                .value(2)
                .posX(0)
                .posY(0)
                .spawnTime(System.currentTimeMillis())
                .duration(10_000)
                .build();
        speedUp.initDefaults();
        player.applyPowerUp(speedUp);
        assertEquals(3, player.getSpeed());

        PowerUp bombCountUp = PowerUp.builder()
                .type(PowerUpType.BOMB_COUNT_UP)
                .value(1)
                .posX(0)
                .posY(0)
                .spawnTime(System.currentTimeMillis())
                .duration(10_000)
                .build();
        bombCountUp.initDefaults();
        player.applyPowerUp(bombCountUp);
        assertEquals(2, player.getBombCount());
    }

    @Test
    void takeDamageRespectsShieldAndDeaths() {
        Player player = newPlayer(new Point(0, 0));
        PowerUp shield = PowerUp.builder()
                .type(PowerUpType.TEMPORARY_SHIELD)
                .value(1)
                .posX(0)
                .posY(0)
                .spawnTime(System.currentTimeMillis())
                .duration(10_000)
                .build();
        shield.initDefaults();
        player.getActivePowerUps().add(shield);

        player.takeDamage(1);
        assertEquals(PlayerStatus.ALIVE, player.getStatus());
        assertFalse(player.hasActiveShield()); // shield consumed

        player.takeDamage(1);
        assertEquals(PlayerStatus.DEAD, player.getStatus());
    }

    @Test
    void takeDamageThrowsOnNegative() {
        Player player = newPlayer(new Point(0, 0));
        assertThrows(IllegalArgumentException.class, () -> player.takeDamage(-1));
    }

    @Test
    void placeBombValidationsAndFailures() {
        Player player = newPlayer(new Point(0, 0));
        Tile tile = Tile.builder().posX(0).posY(0).type(com.arsw.bomberdino.model.enums.TileType.EMPTY)
                .destructible(false).occupied(false).hasBomb(false).build();

        assertThrows(IllegalArgumentException.class, () -> player.placeBomb(null));

        player.setStatus(PlayerStatus.DEAD);
        assertThrows(IllegalStateException.class, () -> player.placeBomb(tile));
        player.setStatus(PlayerStatus.ALIVE);

        // bomb limit reached
        Bomb existing = Bomb.builder().id(java.util.UUID.randomUUID()).placedTime(System.currentTimeMillis())
                .explosionDelay(1_000_000).state(com.arsw.bomberdino.model.enums.BombState.PLACED).build();
        existing.initDefaults();
        player.getCurrentBombsPlaced().add(existing);
        assertNull(player.placeBomb(tile));

        // tile already has bomb
        player.getCurrentBombsPlaced().clear();
        tile.setHasBomb(true);
        assertNull(player.placeBomb(tile));
    }

    @Test
    void placeBombHappyPathSetsBomb() {
        Player player = newPlayer(new Point(0, 0));
        Tile tile = Tile.builder().posX(0).posY(0).type(com.arsw.bomberdino.model.enums.TileType.EMPTY)
                .destructible(false).occupied(false).hasBomb(false).build();

        Bomb bomb = player.placeBomb(tile);

        assertNotNull(bomb);
        assertTrue(tile.hasBomb());
        assertEquals(player.getPosX(), bomb.getPosX());
        assertEquals(player.getPosY(), bomb.getPosY());
    }

    @Test
    void incrementKillsAndIsDestroyed() {
        Player player = newPlayer(new Point(0, 0));
        player.incrementKills();
        assertEquals(1, player.getKills());

        player.setStatus(PlayerStatus.SPECTATING);
        assertTrue(player.isDestroyed());
    }

    @Test
    void canMoveToNegativeCoordinatesReturnsFalse() {
        Player player = newPlayer(new Point(0, 0));
        assertFalse(player.canMoveTo(-1, 0));
        assertTrue(player.canMoveTo(1, 1));
    }

    @Test
    void respawnThrowsIfNoLives() {
        Player player = newPlayer(new Point(5, 5));
        player.setDeaths(2); // lifeCount=2 => no lives remaining
        assertThrows(IllegalStateException.class, player::respawn);
    }

    @Test
    void respawnResetsPositionAndStatus() {
        Player player = newPlayer(new Point(5, 5));
        player.setStatus(PlayerStatus.DEAD);
        player.setDeaths(1); // still has one life
        player.respawn();
        assertEquals(5, player.getPosX());
        assertEquals(5, player.getPosY());
        assertEquals(PlayerStatus.ALIVE, player.getStatus());
    }

    @Test
    void canPlaceBombCleansAndChecksLimit() {
        Player player = newPlayer(new Point(0, 0));
        Bomb bomb = Bomb.builder().id(java.util.UUID.randomUUID()).build();
        bomb.initDefaults();
        bomb.setPlacedTime(System.currentTimeMillis() - 10_000);
        bomb.setExplosionDelay(1000);
        player.getCurrentBombsPlaced().add(bomb);

        // after cleanup the ready bomb is removed, so passing 0 should be allowed
        assertTrue(player.canPlaceBomb(0));
        // but passing a count equal to bomb limit returns false
        assertFalse(player.canPlaceBomb(player.getBombCount()));
    }

    @Test
    void cleanupsRemoveExpiredEntities() {
        Player player = newPlayer(new Point(0, 0));
        PowerUp expired = PowerUp.builder()
                .type(PowerUpType.SPEED_UP)
                .value(1)
                .posX(0).posY(0)
                .spawnTime(System.currentTimeMillis() - 10_000)
                .duration(0)
                .build();
        expired.initDefaults();
        player.getActivePowerUps().add(expired);
        Bomb bomb = Bomb.builder().id(java.util.UUID.randomUUID()).build();
        bomb.initDefaults();
        bomb.setPlacedTime(System.currentTimeMillis() - 10_000);
        bomb.setExplosionDelay(1000);
        player.getCurrentBombsPlaced().add(bomb);

        player.cleanupExpiredPowerUps();
        player.cleanUpPlacedBombs();

        assertTrue(player.getActivePowerUps().isEmpty());
        assertTrue(player.getCurrentBombsPlaced().isEmpty());
    }

    @Test
    void onDestroyClearsState() {
        Player player = newPlayer(new Point(0, 0));
        player.onDestroy();
        assertEquals(PlayerStatus.DISCONNECTED, player.getStatus());
        assertTrue(player.getActivePowerUps().isEmpty());
    }

    @Test
    void getSpeedJustReturnsSpeed() {
        Player player = newPlayer(new Point(0, 0));
        assertEquals(1, player.getSpeed());
    }
}
