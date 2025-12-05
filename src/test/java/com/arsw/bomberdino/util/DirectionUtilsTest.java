package com.arsw.bomberdino.util;

import com.arsw.bomberdino.model.enums.Direction;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DirectionUtilsTest {

    @Test
    void getCardinalDirectionsReturnsFour() {
        Direction[] dirs = DirectionUtils.getCardinalDirections();

        assertEquals(4, dirs.length);
        assertArrayEquals(new Direction[] {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT}, dirs);
    }

    @Test
    void getAdjacentPointsReturnsNeighbors() {
        Point center = new Point(2, 2);

        List<Point> adjacent = DirectionUtils.getAdjacentPoints(center);

        assertTrue(adjacent.contains(new Point(2, 1)));
        assertTrue(adjacent.contains(new Point(2, 3)));
        assertTrue(adjacent.contains(new Point(1, 2)));
        assertTrue(adjacent.contains(new Point(3, 2)));
    }

    @Test
    void getAdjacentPointsThrowsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> DirectionUtils.getAdjacentPoints(null));
    }

    @Test
    void getAdjacentPointsInBoundsFiltersOutside() {
        Point center = new Point(0, 0);

        List<Point> adjacent = DirectionUtils.getAdjacentPointsInBounds(center, 2, 2);

        assertEquals(List.of(new Point(0, 1), new Point(1, 0)), adjacent);
    }

    @Test
    void getDirectionBetweenOnlyForAdjacent() {
        assertEquals(Direction.RIGHT, DirectionUtils.getDirectionBetween(new Point(0, 0), new Point(1, 0)));
        assertEquals(Direction.LEFT, DirectionUtils.getDirectionBetween(new Point(1, 0), new Point(0, 0)));
        assertEquals(Direction.UP, DirectionUtils.getDirectionBetween(new Point(0, 1), new Point(0, 0)));
        assertEquals(Direction.DOWN, DirectionUtils.getDirectionBetween(new Point(0, 0), new Point(0, 1)));
        assertNull(DirectionUtils.getDirectionBetween(new Point(0, 0), new Point(2, 2)));
        assertNull(DirectionUtils.getDirectionBetween(null, new Point(0, 0)));
    }

    @Test
    void getOppositeHandlesNull() {
        assertNull(DirectionUtils.getOpposite(null));
        assertEquals(Direction.LEFT, DirectionUtils.getOpposite(Direction.RIGHT));
        assertEquals(Direction.RIGHT, DirectionUtils.getOpposite(Direction.LEFT));
        assertEquals(Direction.UP, DirectionUtils.getOpposite(Direction.DOWN));
        assertEquals(Direction.DOWN, DirectionUtils.getOpposite(Direction.UP));
    }
}
