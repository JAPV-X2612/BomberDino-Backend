package com.arsw.bomberdino.model.entity;

import com.arsw.bomberdino.model.enums.TileType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExplosionTest {

    private Tile tile(int x, int y, TileType type) {
        return Tile.builder().posX(x).posY(y).type(type).destructible(type.isDestructible()).build();
    }

    @Test
    void expandStopsAtSolidAndIncludesDestructible() {
        Tile[][] grid = {
                {tile(0, 0, TileType.EMPTY), tile(1, 0, TileType.SOLID_WALL), tile(2, 0, TileType.EMPTY)},
                {tile(0, 1, TileType.EMPTY), tile(1, 1, TileType.EMPTY), tile(2, 1, TileType.DESTRUCTIBLE_WALL)},
                {tile(0, 2, TileType.EMPTY), tile(1, 2, TileType.EMPTY), tile(2, 2, TileType.EMPTY)}
        };

        Explosion explosion = Explosion.builder()
                .posX(1)
                .posY(1)
                .damage(1)
                .duration(500L)
                .affectedTiles(new java.util.ArrayList<>())
                .build();

        explosion.expand(grid, 2);

        List<Tile> affected = explosion.getAffectedTiles();
        // Should not pass through solid wall at (1,0)
        assertFalse(affected.contains(grid[0][0]));
        assertTrue(affected.contains(grid[1][2])); // stops at destructible
    }

    @Test
    void affectsTileAndDealDamage() {
        Tile destructible = tile(0, 0, TileType.DESTRUCTIBLE_WALL);
        Explosion explosion = Explosion.builder()
                .posX(0).posY(0).damage(1).duration(200L)
                .affectedTiles(new java.util.ArrayList<>(List.of(destructible)))
                .build();

        assertTrue(explosion.affectsTile(destructible));
        assertThrows(IllegalArgumentException.class, () -> explosion.affectsTile(null));

        explosion.dealDamage();
        assertEquals(TileType.EMPTY, destructible.getType());
    }
}
