package com.arsw.bomberdino.model.entity;

import com.arsw.bomberdino.model.enums.BombState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BombTest {

    private Bomb newBomb() {
        Bomb bomb = Bomb.builder()
                .id(java.util.UUID.randomUUID())
                .posX(1)
                .posY(2)
                .range(2)
                .state(BombState.PLACED)
                .placedTime(System.currentTimeMillis())
                .explosionDelay(1000)
                .build();
        bomb.initDefaults();
        return bomb;
    }

    @Test
    void explodeTransitionsStates() {
        Bomb bomb = newBomb();
        Explosion explosion = bomb.explode();

        assertNotNull(explosion);
        assertEquals(BombState.EXPLODED, bomb.getState());
        assertEquals(1, explosion.getPosX());
        assertEquals(2, explosion.getPosY());
    }

    @Test
    void explodeThrowsWhenNotPlaced() {
        Bomb bomb = newBomb();
        bomb.setState(BombState.EXPLODED);

        assertThrows(IllegalStateException.class, bomb::explode);
    }

    @Test
    void readinessAndRemainingTime() {
        Bomb bomb = newBomb();
        bomb.setPlacedTime(System.currentTimeMillis() - 10_000);
        bomb.setExplosionDelay(1_000);
        assertTrue(bomb.isReadyToExplode());
        assertEquals(0, bomb.getTimeUntilExplosion());
    }
}
