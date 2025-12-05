package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.exception.ValidationException;
import com.arsw.bomberdino.model.entity.Bomb;
import com.arsw.bomberdino.model.entity.GameMap;
import com.arsw.bomberdino.model.entity.GameSession;
import com.arsw.bomberdino.model.entity.PowerUp;
import com.arsw.bomberdino.model.entity.Tile;
import com.arsw.bomberdino.model.enums.BombState;
import com.arsw.bomberdino.model.enums.PowerUpType;
import com.arsw.bomberdino.model.enums.TileType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.Point;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollisionServiceTest {

    @Mock
    private TileService tileService;

    @Mock
    private GameMapService gameMapService;

    @Mock
    private GameSessionService gameSessionService;

    @InjectMocks
    private CollisionService service;

    @Test
    void canMoveToReturnsTrueWhenWalkableAndFree() {
        GameMap map = buildMap();
        Point target = new Point(2, 2);

        when(gameMapService.getMap("session")).thenReturn(map);
        when(gameMapService.getTileType("session", target)).thenReturn(TileType.EMPTY);
        when(tileService.isOccupied("session", target)).thenReturn(false);

        assertTrue(service.canMoveTo("session", target));
    }

    @Test
    void canMoveToRejectsNonWalkableOrOccupiedTiles() {
        GameMap map = buildMap();
        Point wall = new Point(0, 0);
        when(gameMapService.getMap("session")).thenReturn(map);
        when(gameMapService.getTileType("session", wall)).thenReturn(TileType.SOLID_WALL);

        assertFalse(service.canMoveTo("session", wall));

        Point occupied = new Point(1, 1);
        when(gameMapService.getTileType("session", occupied)).thenReturn(TileType.EMPTY);
        when(tileService.isOccupied("session", occupied)).thenReturn(true);

        assertFalse(service.canMoveTo("session", occupied));
    }

    @Test
    void detectPowerUpCollisionReturnsNonExpiredPowerUp() {
        PowerUp active = PowerUp.builder()
                .id(UUID.randomUUID())
                .posX(2)
                .posY(2)
                .createdAt(LocalDateTime.now())
                .type(PowerUpType.BOMB_COUNT_UP)
                .value(1)
                .spawnTime(System.currentTimeMillis())
                .duration(10_000)
                .build();
        PowerUp expired = PowerUp.builder()
                .id(UUID.randomUUID())
                .posX(1)
                .posY(1)
                .createdAt(LocalDateTime.now())
                .type(PowerUpType.BOMB_RANGE_UP)
                .value(1)
                .spawnTime(System.currentTimeMillis() - 20_000)
                .duration(1_000)
                .build();

        GameSession session = new GameSession();
        session.setAvailablePowerUps(new ArrayList<>(List.of(active, expired)));
        when(gameSessionService.getSession("session")).thenReturn(session);

        PowerUp found = service.detectPowerUpCollision("session", new Point(2, 2));
        assertSame(active, found);

        assertNull(service.detectPowerUpCollision("session", new Point(1, 1)));
    }

    @Test
    void handleBombExplosionStopsAtWallsAndDestructibles() {
        GameMap map = buildMap();
        Bomb bomb = Bomb.builder()
                .id(UUID.randomUUID())
                .posX(2)
                .posY(2)
                .range(3)
                .state(BombState.PLACED)
                .placedTime(System.currentTimeMillis())
                .explosionDelay(3000)
                .build();
        GameSession session = new GameSession();
        session.setActiveBombs(List.of(bomb));

        when(gameMapService.getMap("session")).thenReturn(map);
        when(gameSessionService.getSession("session")).thenReturn(session);

        List<Point> tiles = service.handleBombExplosion("session", bomb.getId().toString(), 3);

        assertTrue(tiles.contains(new Point(2, 2))); // center
        assertTrue(tiles.contains(new Point(2, 1))); // destructible included then stop upward
        assertTrue(tiles.contains(new Point(2, 3)));
        assertTrue(tiles.contains(new Point(2, 4))); // border solid included, then stop
        assertTrue(tiles.contains(new Point(1, 2)));
        assertTrue(tiles.contains(new Point(0, 2))); // border solid included, then stop
        assertTrue(tiles.contains(new Point(3, 2)));
        assertTrue(tiles.contains(new Point(4, 2))); // border solid included, then stop
        assertEquals(8, tiles.size());
    }

    @Test
    void isValidPositionUsesMapBounds() {
        GameMap map = buildMap();
        when(gameMapService.getMap("session")).thenReturn(map);

        assertTrue(service.isValidPosition("session", new Point(2, 2)));
        assertFalse(service.isValidPosition("session", new Point(10, 10)));
    }

    @Test
    void validationGuardsAgainstNullsOrInvalidRange() {
        assertThrows(ValidationException.class, () -> service.canMoveTo(null, new Point(1, 1)));
        assertThrows(ValidationException.class, () -> service.canMoveTo("session", null));
        assertThrows(ValidationException.class,
                () -> service.handleBombExplosion("session", null, 1));
        assertThrows(ValidationException.class,
                () -> service.handleBombExplosion("session", "bomb", 0));
    }

    private GameMap buildMap() {
        int width = 5;
        int height = 5;
        Tile[][] tiles = new Tile[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                TileType type = (x == 0 || y == 0 || x == width - 1 || y == height - 1)
                        ? TileType.SOLID_WALL
                        : TileType.EMPTY;
                tiles[y][x] = Tile.builder()
                        .posX(x)
                        .posY(y)
                        .type(type)
                        .destructible(type == TileType.DESTRUCTIBLE_WALL)
                        .occupied(false)
                        .hasBomb(false)
                        .createdAt(LocalDateTime.now())
                        .build();
            }
        }

        tiles[1][2].setType(TileType.DESTRUCTIBLE_WALL); // block upward propagation

        return GameMap.builder()
                .mapId(UUID.randomUUID())
                .name("map")
                .width(width)
                .height(height)
                .tiles(tiles)
                .spawnPoints(List.of(new Point(1, 1)))
                .build();
    }
}
