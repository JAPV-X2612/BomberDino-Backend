package com.arsw.bomberdino.model.entity;

import com.arsw.bomberdino.model.enums.PowerUpType;
import org.junit.jupiter.api.Test;

import java.awt.Point;

import static org.junit.jupiter.api.Assertions.*;

class PowerUpTest {

    private Player player() {
        return Player.builder()
                .id(java.util.UUID.randomUUID())
                .username("u")
                .posX(0)
                .posY(0)
                .lifeCount(1)
                .bombCount(1)
                .bombRange(1)
                .speed(1)
                .status(com.arsw.bomberdino.model.enums.PlayerStatus.ALIVE)
                .spawnPoint(new Point(0, 0))
                .build();
    }

    @Test
    void applyToAdjustsStats() {
        Player p = player();
        PowerUp power = PowerUp.builder()
                .type(PowerUpType.BOMB_RANGE_UP)
                .value(2)
                .posX(0)
                .posY(0)
                .spawnTime(System.currentTimeMillis())
                .duration(10_000)
                .build();
        power.initDefaults();

        power.applyTo(p);

        assertEquals(3, p.getBombRange());
    }

    @Test
    void applyToThrowsOnNullOrExpired() {
        PowerUp power = PowerUp.builder()
                .type(PowerUpType.SPEED_UP)
                .value(1)
                .posX(0)
                .posY(0)
                .spawnTime(System.currentTimeMillis() - 10_000)
                .duration(0)
                .build();
        power.initDefaults();

        assertThrows(IllegalArgumentException.class, () -> power.applyTo(null));
        assertTrue(power.isExpired());
        assertThrows(IllegalStateException.class, () -> power.applyTo(player()));
    }
}
