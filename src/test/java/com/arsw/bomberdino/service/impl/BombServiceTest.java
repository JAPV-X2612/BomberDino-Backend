package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.exception.ValidationException;
import com.arsw.bomberdino.model.entity.Bomb;
import com.arsw.bomberdino.model.enums.BombState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BombServiceTest {

    @Mock
    private CollisionService collisionService;

    @InjectMocks
    private BombService service;

    private final Point position = new Point(2, 3);

    @BeforeEach
    void setup() {
        when(collisionService.isValidPosition(any(), any())).thenReturn(true);
    }

    @Test
    void placeBombCreatesPlacedBombWhenPositionValid() {
        Bomb bomb = service.placeBomb("session-1", "player-1", position, 3);

        assertNotNull(bomb);
        assertEquals(BombState.PLACED, bomb.getState());
        assertEquals(position.x, bomb.getPosX());
        assertEquals(position.y, bomb.getPosY());
        assertEquals(2, bomb.getRange()); // hardcoded in service

        List<Bomb> active = service.getActiveBombs("session-1");
        assertTrue(active.stream().anyMatch(b -> b.getId().equals(bomb.getId())));
    }

    @Test
    void placeBombReturnsNullWhenPositionInvalid() {
        when(collisionService.isValidPosition(eq("session-1"), eq(position))).thenReturn(false);

        Bomb bomb = service.placeBomb("session-1", "player-1", position, 3);

        assertNull(bomb);
    }

    @Test
    void explodeBombReturnsCrossPatternAndUpdatesState() {
        Bomb bomb = service.placeBomb("session-1", "player-1", position, 3);
        String bombId = bomb.getId().toString();

        List<Point> affected = service.explodeBomb(bombId);

        assertEquals(1 + 4 * bomb.getRange(), affected.size());
        assertTrue(affected.contains(new Point(position.x, position.y)));
        assertEquals(BombState.EXPLODED, bomb.getState());
    }

    @Test
    void explodeBombValidatesStateAndExistence() {
        assertThrows(ValidationException.class, () -> service.explodeBomb(null));
        assertThrows(IllegalStateException.class, () -> service.explodeBomb("missing"));

        Bomb bomb = service.placeBomb("session-1", "player-1", position, 3);
        bomb.setState(BombState.EXPLODED);

        assertThrows(IllegalStateException.class, () -> service.explodeBomb(bomb.getId().toString()));
    }

    @Test
    void getActiveBombsValidatesSessionId() {
        assertThrows(ValidationException.class, () -> service.getActiveBombs(null));
        assertThrows(ValidationException.class, () -> service.getActiveBombs(" "));
    }

    @Test
    void isReadyToExplodeReflectsCountdownAndMissingBomb() {
        Bomb bomb = service.placeBomb("session-1", "player-1", position, 3);
        String bombId = bomb.getId().toString();

        bomb.setPlacedTime(System.currentTimeMillis() - 5000);
        bomb.setExplosionDelay(1000);

        assertTrue(service.isReadyToExplode(bombId));
        assertFalse(service.isReadyToExplode("missing"));
    }

    @Test
    void removeBombDeletesFromStorage() {
        Bomb bomb = service.placeBomb("session-1", "player-1", position, 3);
        String bombId = bomb.getId().toString();

        service.removeBomb(bombId);

        assertFalse(service.isReadyToExplode(bombId));
        assertTrue(service.getActiveBombs("session-1").isEmpty());
    }
}
