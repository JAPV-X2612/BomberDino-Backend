package com.arsw.bomberdino.model.entity;

import com.arsw.bomberdino.model.enums.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TileTest {

    @Test
    void destroyChangesStateWhenDestructible() {
        Tile tile = Tile.builder().posX(0).posY(0).type(TileType.DESTRUCTIBLE_WALL)
                .destructible(true).occupied(true).hasBomb(true).build();

        tile.destroy();

        assertEquals(TileType.EMPTY, tile.getType());
        assertFalse(tile.isDestructible());
        assertFalse(tile.isOccupied());
        assertFalse(tile.hasBomb());
    }

    @Test
    void destroyThrowsWhenIndestructible() {
        Tile tile = Tile.builder().posX(0).posY(0).type(TileType.SOLID_WALL)
                .destructible(false).occupied(false).build();

        assertThrows(IllegalStateException.class, tile::destroy);
    }

    @Test
    void placeAndRemoveBombThreadSafe() {
        Tile tile = Tile.builder().posX(0).posY(0).type(TileType.EMPTY)
                .destructible(false).occupied(false).hasBomb(false).build();

        assertTrue(tile.tryPlaceBomb());
        assertFalse(tile.tryPlaceBomb()); // already placed

        tile.removeBomb();
        assertFalse(tile.hasBomb());
    }

    @Test
    void setOccupiedRespectsWalkable() {
        Tile tile = Tile.builder().posX(0).posY(0).type(TileType.SOLID_WALL)
                .destructible(false).occupied(false).hasBomb(false).build();

        assertFalse(tile.setOccupied(true)); // not walkable
        tile.setType(TileType.EMPTY);
        assertTrue(tile.setOccupied(true));
        assertTrue(tile.isOccupied());
    }

    @Test
    void damageDestroysDestructible() {
        Tile tile = Tile.builder().posX(0).posY(0).type(TileType.DESTRUCTIBLE_WALL)
                .destructible(true).occupied(false).hasBomb(false).build();
        tile.takeDamage(1);
        assertEquals(TileType.EMPTY, tile.getType());
        assertTrue(tile.isDestroyed());
    }
}
