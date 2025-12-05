package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.exception.PlayerNotFoundException;
import com.arsw.bomberdino.exception.ValidationException;
import com.arsw.bomberdino.model.dto.response.PowerUpEffect;
import com.arsw.bomberdino.model.entity.Player;
import com.arsw.bomberdino.model.enums.PowerUpType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerServiceTest {

    private PlayerService service;

    @BeforeEach
    void setUp() {
        service = new PlayerService();
    }

    @Test
    void createPlayerStoresAndReturnsPlayer() {
        Player player = service.createPlayer("player-1", "user", new Point(1, 2));

        assertNotNull(player.getId());
        assertEquals("user", player.getUsername());
        assertSame(player, service.getPlayer("player-1"));
    }

    @Test
    void createPlayerValidatesInputAndDuplicate() {
        assertThrows(ValidationException.class, () -> service.createPlayer(null, "user", new Point(1, 1)));
        assertThrows(ValidationException.class, () -> service.createPlayer("id", null, new Point(1, 1)));
        assertThrows(ValidationException.class, () -> service.createPlayer("id", "user", null));

        service.createPlayer("id", "user", new Point(1, 1));
        assertThrows(IllegalStateException.class, () -> service.createPlayer("id", "other", new Point(1, 1)));
    }

    @Test
    void movePlayerUpdatesPositionWhenAlive() {
        service.createPlayer("p1", "user", new Point(0, 0));

        Player updated = service.movePlayer("p1", new Point(2, 3));

        assertEquals(2, updated.getPosX());
        assertEquals(3, updated.getPosY());
    }

    @Test
    void movePlayerRejectsInvalidInputOrDeadPlayer() {
        assertThrows(ValidationException.class, () -> service.movePlayer(null, new Point(0, 0)));
        service.createPlayer("p1", "user", new Point(0, 0));
        Player player = service.getPlayer("p1");
        player.setStatus(com.arsw.bomberdino.model.enums.PlayerStatus.DEAD);

        assertThrows(IllegalStateException.class, () -> service.movePlayer("p1", new Point(1, 1)));
    }

    @Test
    void killPlayerIncrementsKillsAndDeaths() {
        service.createPlayer("killer", "k", new Point(0, 0));
        service.createPlayer("victim", "v", new Point(0, 0));

        service.killPlayer("killer", "victim");

        assertEquals(1, service.getPlayer("killer").getKills());
        assertEquals(1, service.getPlayer("victim").getDeaths());
    }

    @Test
    void respawnPlayerResetsPositionAndStatus() {
        service.createPlayer("p1", "user", new Point(1, 2));
        Player player = service.getPlayer("p1");
        player.die();
        player.setDeaths(0); // ensure alive logic passes

        Player respawned = service.respawnPlayer("p1");

        assertEquals(1, respawned.getPosX());
        assertEquals(2, respawned.getPosY());
        assertEquals(com.arsw.bomberdino.model.enums.PlayerStatus.ALIVE, respawned.getStatus());
    }

    @Test
    void applyPowerUpEffectAddsPowerUpToPlayer() {
        service.createPlayer("p1", "user", new Point(0, 0));
        PowerUpEffect effect = PowerUpEffect.builder()
                .type(PowerUpType.TEMPORARY_SHIELD)
                .duration(5)
                .multiplier(1.0f)
                .build();

        service.applyPowerUpEffect("p1", effect);

        assertFalse(service.getPlayer("p1").getActivePowerUps().isEmpty());
    }

    @Test
    void applyPowerUpEffectValidatesInputs() {
        assertThrows(ValidationException.class, () -> service.applyPowerUpEffect(null, PowerUpEffect.builder().build()));
        service.createPlayer("p1", "user", new Point(0, 0));
        assertThrows(ValidationException.class, () -> service.applyPowerUpEffect("p1", null));
    }

    @Test
    void isAliveReturnsFalseWhenMissingPlayer() {
        assertFalse(service.isAlive(UUID.randomUUID().toString()));
    }

    @Test
    void isAliveThrowsOnBlankId() {
        assertThrows(ValidationException.class, () -> service.isAlive(" "));
    }

    @Test
    void removePlayerDeletesFromStore() {
        service.createPlayer("p1", "user", new Point(0, 0));
        service.removePlayer("p1");

        assertThrows(PlayerNotFoundException.class, () -> service.getPlayer("p1"));
        assertThrows(ValidationException.class, () -> service.removePlayer(""));
    }

    @Test
    void validatePositionAndUsernameBranches() {
        assertThrows(ValidationException.class, () -> service.movePlayer("id", null));
        assertThrows(ValidationException.class, () -> service.createPlayer("id", " ", new Point(1, 1)));
    }

    @Test
    void respawnPlayerValidatesAlive() {
        service.createPlayer("p1", "user", new Point(1, 2));
        Player player = service.getPlayer("p1");
        player.setLifeCount(0);
        player.setDeaths(1);
        assertThrows(IllegalStateException.class, () -> service.respawnPlayer("p1"));
    }

    @Test
    void killPlayerValidatesIdsAndPlayerLookup() {
        assertThrows(ValidationException.class, () -> service.killPlayer(null, "v"));
        assertThrows(ValidationException.class, () -> service.killPlayer("k", " "));
        assertThrows(PlayerNotFoundException.class, () -> service.killPlayer("k", "missing"));
    }

    @Test
    void incrementDeathsValidates() {
        assertThrows(ValidationException.class, () -> service.incrementDeaths(" "));
        assertThrows(PlayerNotFoundException.class, () -> service.incrementDeaths("missing"));
    }
}
