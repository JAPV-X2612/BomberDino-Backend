package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.exception.ValidationException;
import com.arsw.bomberdino.model.dto.response.PowerUpEffect;
import com.arsw.bomberdino.model.entity.PowerUp;
import com.arsw.bomberdino.model.enums.PowerUpType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PowerUpServiceTest {

    private PowerUpService service;

    private java.util.concurrent.ConcurrentHashMap<String, PowerUp> powerUpStore;

    @BeforeEach
    void setUp() {
        service = new PowerUpService();
        powerUpStore = getPowerUpStore(service);
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void spawnPowerUpCreatesAndStoresPowerUp() {
        PowerUp powerUp = service.spawnPowerUp("session-1", PowerUpType.BOMB_RANGE_UP,
                new Point(2, 3));

        assertNotNull(powerUp.getId());
        assertEquals(PowerUpType.BOMB_RANGE_UP, powerUp.getType());
        assertEquals(1, powerUp.getValue());
        assertFalse(powerUp.isExpired());

        List<PowerUp> active = service.getActivePowerUps("session-1");
        assertTrue(active.stream().anyMatch(pu -> pu.getId().equals(powerUp.getId())));
    }

    @Test
    void applyPowerUpEffectReturnsEffectAndRemovesPowerUp() {
        PowerUp powerUp = buildPowerUp(PowerUpType.SPEED_UP, new Point(1, 1), 5_000);
        powerUpStore.put(powerUp.getId().toString(), powerUp);

        PowerUpEffect effect = service.applyPowerUpEffect("player-1", powerUp.getId().toString());

        assertEquals(PowerUpType.SPEED_UP, effect.getType());
        assertTrue(service.getActivePowerUps("session-1").isEmpty());
    }

    @Test
    void applyPowerUpEffectFailsForMissingOrExpired() {
        assertThrows(ValidationException.class,
                () -> service.applyPowerUpEffect(null, "id"));
        assertThrows(ValidationException.class,
                () -> service.applyPowerUpEffect("player-1", null));
        assertThrows(IllegalStateException.class,
                () -> service.applyPowerUpEffect("player-1", "missing"));

        PowerUp powerUp = buildPowerUp(PowerUpType.EXTRA_LIFE, new Point(0, 0), 1_000);
        powerUp.setSpawnTime(System.currentTimeMillis() - 40_000);
        powerUpStore.put(powerUp.getId().toString(), powerUp);

        assertThrows(IllegalStateException.class,
                () -> service.applyPowerUpEffect("player-1", powerUp.getId().toString()));
    }

    @Test
    void getActivePowerUpsFiltersExpired() {
        PowerUp active = buildPowerUp(PowerUpType.BOMB_COUNT_UP, new Point(1, 1), 10_000);
        PowerUp expired = buildPowerUp(PowerUpType.BOMB_RANGE_UP, new Point(2, 2), 1_000);
        expired.setSpawnTime(System.currentTimeMillis() - 50_000);
        powerUpStore.put(active.getId().toString(), active);
        powerUpStore.put(expired.getId().toString(), expired);

        List<PowerUp> activeList = service.getActivePowerUps("session-1");

        assertTrue(activeList.contains(active));
        assertFalse(activeList.contains(expired));
    }

    @Test
    void scheduleExpirationRemovesExpiredPowerUp() throws InterruptedException {
        PowerUp powerUp = buildPowerUp(PowerUpType.BOMB_COUNT_UP, new Point(0, 0), 1_000);
        powerUp.setSpawnTime(System.currentTimeMillis() - 10_000);
        powerUpStore.put(powerUp.getId().toString(), powerUp);

        service.scheduleExpiration(powerUp.getId().toString(), 10);
        Thread.sleep(30); // allow scheduled task to run quickly

        assertTrue(service.getActivePowerUps("session-1").isEmpty());
    }

    @Test
    void validationsRejectInvalidInput() {
        assertThrows(ValidationException.class,
                () -> service.spawnPowerUp(null, PowerUpType.BOMB_COUNT_UP, new Point(0, 0)));
        assertThrows(ValidationException.class,
                () -> service.spawnPowerUp("session", null, new Point(0, 0)));
        assertThrows(ValidationException.class,
                () -> service.spawnPowerUp("session", PowerUpType.BOMB_COUNT_UP, null));
        assertThrows(ValidationException.class,
                () -> service.getActivePowerUps(null));
        assertThrows(ValidationException.class,
                () -> service.scheduleExpiration("id", 0));
    }

    private PowerUp buildPowerUp(PowerUpType type, Point position, long duration) {
        PowerUp powerUp = PowerUp.builder()
                .id(UUID.randomUUID())
                .posX(position.x)
                .posY(position.y)
                .type(type)
                .value(1)
                .spawnTime(System.currentTimeMillis())
                .duration(duration)
                .build();
        powerUp.initDefaults();
        return powerUp;
    }

    @SuppressWarnings("unchecked")
    private java.util.concurrent.ConcurrentHashMap<String, PowerUp> getPowerUpStore(PowerUpService svc) {
        try {
            var field = PowerUpService.class.getDeclaredField("powerUps");
            field.setAccessible(true);
            return (java.util.concurrent.ConcurrentHashMap<String, PowerUp>) field.get(svc);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to access powerUps store", e);
        }
    }
}
