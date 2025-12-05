package com.arsw.bomberdino.util;

import com.arsw.bomberdino.model.dto.response.PointDTO;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PointMapperTest {

    @Test
    void toDtoAndPointRoundTrip() {
        Point p = new Point(3, 4);
        PointDTO dto = PointMapper.toDTO(p);
        assertEquals(3, dto.getX());
        assertEquals(4, dto.getY());
        Point back = PointMapper.toPoint(dto);
        assertEquals(p, back);
    }

    @Test
    void listConversionsHandleNull() {
        assertNull(PointMapper.toDTO(null));
        assertNull(PointMapper.toPoint(null));
        assertNull(PointMapper.toDTOList(null));
        assertNull(PointMapper.toPointList(null));
    }

    @Test
    void listConversionsMapValues() {
        List<Point> points = List.of(new Point(1, 2), new Point(3, 4));
        List<PointDTO> dtos = PointMapper.toDTOList(points);
        assertEquals(2, dtos.size());
        List<Point> back = PointMapper.toPointList(dtos);
        assertEquals(points, back);
    }

    @Test
    void equalityAndDistanceChecks() {
        Point a = new Point(1, 1);
        Point b = new Point(1, 1);
        Point c = new Point(2, 3);

        assertTrue(PointMapper.areEqual(a, b));
        assertFalse(PointMapper.areEqual(a, c));
        assertTrue(PointMapper.areEqual(null, null));
        assertFalse(PointMapper.areEqual(null, a));

        assertEquals(3, PointMapper.manhattanDistance(a, c));
        assertThrows(IllegalArgumentException.class, () -> PointMapper.manhattanDistance(null, c));
    }

    @Test
    void boundsCheck() {
        assertTrue(PointMapper.isInBounds(new Point(0, 0), 5, 5));
        assertFalse(PointMapper.isInBounds(new Point(5, 0), 5, 5));
        assertFalse(PointMapper.isInBounds(new Point(-1, 0), 5, 5));
        assertFalse(PointMapper.isInBounds(null, 5, 5));
    }
}
