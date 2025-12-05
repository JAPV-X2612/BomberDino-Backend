package com.arsw.bomberdino.model.entity;

import com.arsw.bomberdino.model.enums.TileType;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameMapTest {

    private GameMap buildMap() {
        Tile[][] tiles = new Tile[3][3];
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                tiles[y][x] = Tile.builder()
                        .posX(x)
                        .posY(y)
                        .type(TileType.EMPTY)
                        .destructible(false)
                        .occupied(false)
                        .build();
            }
        }
        tiles[1][1].setOccupied(true);
        tiles[2][2].setType(TileType.SOLID_WALL);

        return GameMap.builder()
                .mapId(UUID.randomUUID())
                .name("test")
                .width(3)
                .height(3)
                .tiles(tiles)
                .spawnPoints(List.of(new Point(0, 0), new Point(1, 1)))
                .build();
    }

    @Test
    void getTileValidatesBounds() {
        GameMap map = buildMap();
        assertNotNull(map.getTile(0, 0));
        assertNull(map.getTile(-1, 0));
        assertNull(map.getTile(3, 0));
    }

    @Test
    void availableSpawnsSkipOccupied() {
        GameMap map = buildMap();
        List<Point> available = map.getAvailableSpawnPoints();
        assertEquals(1, available.size());
        assertTrue(available.contains(new Point(0, 0)));
    }

    @Test
    void emptyTilePositionsSkipWallsAndOccupied() {
        GameMap map = buildMap();
        List<Point> empties = map.getEmptyTilePositions();
        assertFalse(empties.contains(new Point(1, 1))); // occupied
        assertFalse(empties.contains(new Point(2, 2))); // solid wall
        assertTrue(empties.contains(new Point(0, 0)));
    }
}
