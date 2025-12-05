package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.exception.SessionNotFoundException;
import com.arsw.bomberdino.exception.ValidationException;
import com.arsw.bomberdino.model.entity.GameMap;
import com.arsw.bomberdino.model.entity.Tile;
import com.arsw.bomberdino.model.enums.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TileServiceTest {

    private TileService service;
    private GameMap map;

    @BeforeEach
    void setUp() {
        service = new TileService();
        map = buildMap(4, 4);
        service.initializeTiles("session-1", map);
    }

    @Test
    void initializeTilesValidatesInputAndPreventsDuplicates() {
        assertThrows(ValidationException.class, () -> service.initializeTiles("session-2", null));
        assertThrows(IllegalStateException.class, () -> service.initializeTiles("session-1", map));
    }

    @Test
    void getTileThrowsWhenSessionMissing() {
        assertThrows(SessionNotFoundException.class,
                () -> service.getTile("missing", new Point(1, 1)));
    }

    @Test
    void isOccupiedChecksTileAndBombFlags() {
        Point pos = new Point(1, 1);
        assertFalse(service.isOccupied("session-1", pos));

        Tile tile = service.getTile("session-1", pos);
        tile.setOccupied(true);
        assertTrue(service.isOccupied("session-1", pos));

        tile.setOccupied(false);
        tile.tryPlaceBomb();
        assertTrue(service.isOccupied("session-1", pos));
    }

    @Test
    void isOccupiedReturnsFalseWhenTileDoesNotExist() {
        assertFalse(service.isOccupied("session-1", new Point(99, 99)));
    }

    @Test
    void tryOccupyReturnsFalseForNonWalkableOrOccupiedTiles() {
        Point wallPos = new Point(0, 0); // solid wall
        assertFalse(service.tryOccupy("session-1", wallPos, false));

        Point emptyPos = new Point(1, 1);
        assertTrue(service.tryOccupy("session-1", emptyPos, false));
        assertFalse(service.tryOccupy("session-1", emptyPos, false));
    }

    @Test
    void tryOccupyReturnsFalseWhenTileMissingOrAlreadyOccupiedByBomb() {
        assertFalse(service.tryOccupy("session-1", new Point(99, 99), false));

        Point pos = new Point(1, 1);
        assertTrue(service.tryOccupy("session-1", pos, false));
        assertFalse(service.tryOccupy("session-1", pos, true)); // occupied even if placing bomb
    }

    @Test
    void releaseOccupationFreesTile() {
        Point pos = new Point(1, 1);
        service.tryOccupy("session-1", pos, false);

        service.releaseOccupation("session-1", pos);

        assertFalse(service.isOccupied("session-1", pos));
    }

    @Test
    void markBombSetsAndClearsBombFlag() {
        Point pos = new Point(1, 1);
        service.markBomb("session-1", pos, true);
        assertTrue(service.getTile("session-1", pos).hasBomb());

        service.markBomb("session-1", pos, false);
        assertFalse(service.getTile("session-1", pos).hasBomb());
    }

    @Test
    void markBombAndReleaseIgnoreMissingTiles() {
        assertDoesNotThrow(() -> service.markBomb("session-1", new Point(99, 99), true));
        assertDoesNotThrow(() -> service.releaseOccupation("session-1", new Point(99, 99)));
    }

    @Test
    void applyExplosionToTileDestroysDestructibleTile() {
        Point pos = new Point(2, 2);
        Tile tile = service.getTile("session-1", pos);
        assertTrue(tile.isDestructible());

        service.applyExplosionToTile("session-1", pos);

        assertEquals(TileType.EMPTY, tile.getType());
        assertFalse(tile.isDestructible());
    }

    @Test
    void applyExplosionToTileDoesNothingForNonDestructibleOrMissingTile() {
        Tile nonDestructible = service.getTile("session-1", new Point(1, 1));
        assertNotNull(nonDestructible);
        assertDoesNotThrow(() -> service.applyExplosionToTile("session-1", new Point(99, 99)));

        service.applyExplosionToTile("session-1", new Point(1, 1));

        assertEquals(TileType.EMPTY, nonDestructible.getType());
        assertFalse(nonDestructible.isDestructible());
    }

    @Test
    void clearSessionRemovesTilesMap() {
        service.clearSession("session-1");

        assertThrows(SessionNotFoundException.class,
                () -> service.getTile("session-1", new Point(1, 1)));
    }

    @Test
    void validationGuardsAgainstNulls() {
        assertThrows(ValidationException.class, () -> service.isOccupied(null, new Point(0, 0)));
        assertThrows(ValidationException.class, () -> service.isOccupied("session-1", null));
        assertThrows(ValidationException.class,
                () -> service.tryOccupy("session-1", null, false));
        assertThrows(ValidationException.class,
                () -> service.releaseOccupation(null, new Point(0, 0)));
        assertThrows(ValidationException.class,
                () -> service.markBomb("session-1", null, true));
    }

    private GameMap buildMap(int width, int height) {
        Tile[][] tiles = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                TileType type = (x == 0 && y == 0) ? TileType.SOLID_WALL : TileType.EMPTY;
                boolean destructible = type == TileType.DESTRUCTIBLE_WALL;
                tiles[y][x] = Tile.builder()
                        .posX(x)
                        .posY(y)
                        .type(type)
                        .destructible(destructible || (x == 2 && y == 2))
                        .occupied(false)
                        .hasBomb(false)
                        .createdAt(LocalDateTime.now())
                        .build();
            }
        }
        tiles[2][2].setType(TileType.DESTRUCTIBLE_WALL);
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
