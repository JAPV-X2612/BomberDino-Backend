package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.exception.ValidationException;
import com.arsw.bomberdino.model.entity.GameMap;
import com.arsw.bomberdino.model.enums.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameMapServiceTest {

    private GameMapService service;

    @BeforeEach
    void setUp() {
        service = new GameMapService();
    }

    @Test
    void createMapStoresAndRetrievesMap() {
        GameMap map = service.createMap("session-1", 12, 12);

        assertNotNull(map);
        assertEquals(12, map.getWidth());
        assertSame(map, service.getMap("session-1"));
    }

    @Test
    void createMapRejectsDuplicateSession() {
        service.createMap("session-1", 12, 12);

        assertThrows(IllegalStateException.class,
                () -> service.createMap("session-1", 12, 12));
    }

    @Test
    void createMapValidatesSessionAndDimensions() {
        assertThrows(ValidationException.class,
                () -> service.createMap(null, 12, 12));
        assertThrows(ValidationException.class,
                () -> service.createMap("   ", 12, 12));
        assertThrows(ValidationException.class,
                () -> service.createMap("session-1", 11, 12));
        assertThrows(ValidationException.class,
                () -> service.createMap("session-1", 12, 11));
    }

    @Test
    void getMapThrowsWhenMissing() {
        assertThrows(IllegalStateException.class, () -> service.getMap("missing"));
    }

    @Test
    void getValidSpawnPointsReturnsFourCornersWhenUnoccupied() {
        service.createMap("session-1", 12, 12);

        List<Point> spawnPoints = service.getValidSpawnPoints("session-1");

        assertEquals(4, spawnPoints.size());
        assertTrue(spawnPoints.contains(new Point(1, 1)));
        assertTrue(spawnPoints.contains(new Point(10, 1)));
        assertTrue(spawnPoints.contains(new Point(1, 10)));
        assertTrue(spawnPoints.contains(new Point(10, 10)));
    }

    @Test
    void isPositionValidRejectsNullAndChecksBounds() {
        assertThrows(ValidationException.class,
                () -> service.isPositionValid(null, 12, 12));
        assertTrue(service.isPositionValid(new Point(0, 0), 12, 12));
        assertFalse(service.isPositionValid(new Point(-1, 0), 12, 12));
    }

    @Test
    void getTileTypeReturnsExpectedTypeOrThrows() {
        service.createMap("session-1", 12, 12);

        assertEquals(TileType.EMPTY, service.getTileType("session-1", new Point(1, 1)));
        assertThrows(ValidationException.class,
                () -> service.getTileType("session-1", null));
        assertThrows(IllegalStateException.class,
                () -> service.getTileType("session-1", new Point(50, 50)));
    }

    @Test
    void getRandomEmptyPositionReturnsValidPoint() {
        int width = 12;
        int height = 12;
        service.createMap("session-1", width, height);

        Point randomEmpty = service.getRandomEmptyPosition("session-1");

        assertNotNull(randomEmpty);
        assertTrue(service.isPositionValid(randomEmpty, width, height));
        assertEquals(TileType.EMPTY,
                service.getMap("session-1").getTile(randomEmpty.x, randomEmpty.y).getType());
    }

    @Test
    void clearSessionRemovesMap() {
        service.createMap("session-1", 12, 12);

        service.clearSession("session-1");

        assertThrows(IllegalStateException.class, () -> service.getMap("session-1"));
    }
}
