package com.arsw.bomberdino.util;

import com.arsw.bomberdino.model.dto.response.BombDTO;
import com.arsw.bomberdino.model.dto.response.PlayerDTO;
import com.arsw.bomberdino.model.dto.response.PowerUpDTO;
import com.arsw.bomberdino.model.entity.Bomb;
import com.arsw.bomberdino.model.entity.Player;
import com.arsw.bomberdino.model.entity.PowerUp;
import com.arsw.bomberdino.model.enums.BombState;
import com.arsw.bomberdino.model.enums.PlayerStatus;
import com.arsw.bomberdino.model.enums.PowerUpType;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameStateMapperTest {

    @Test
    void utilityConstructorIsHidden() {
        var ctor = assertDoesNotThrow(() -> GameStateMapper.class.getDeclaredConstructor());
        ctor.setAccessible(true);
        Exception ex = assertThrows(Exception.class, ctor::newInstance);
        assertTrue(ex.getCause() instanceof UnsupportedOperationException);
    }

    @Test
    void mapsPlayerBombAndPowerUpEntities() {
        Player player = Player.builder()
                .id(UUID.randomUUID())
                .username("user")
                .posX(1)
                .posY(2)
                .lifeCount(3)
                .status(PlayerStatus.ALIVE)
                .kills(4)
                .deaths(1)
                .spawnPoint(new Point(0, 0))
                .bombCount(1)
                .bombRange(2)
                .speed(1)
                .build();

        Bomb bomb = Bomb.builder()
                .id(UUID.randomUUID())
                .posX(5)
                .posY(6)
                .range(3)
                .state(BombState.PLACED)
                .placedTime(1L)
                .explosionDelay(2000L)
                .build();

        PowerUp powerUp = PowerUp.builder()
                .id(UUID.randomUUID())
                .type(PowerUpType.EXTRA_LIFE)
                .posX(7)
                .posY(8)
                .duration(1000L)
                .spawnTime(System.currentTimeMillis())
                .value(1)
                .build();

        PlayerDTO playerDTO = GameStateMapper.toPlayerDTO(player);
        BombDTO bombDTO = GameStateMapper.toBombDTO(bomb);
        PowerUpDTO powerUpDTO = GameStateMapper.toPowerUpDTO(powerUp);

        assertEquals(player.getId().toString(), playerDTO.getId());
        assertEquals(bomb.getId().toString(), bombDTO.getId());
        assertEquals(powerUp.getId().toString(), powerUpDTO.getId());
        assertEquals(powerUp.getType(), powerUpDTO.getType());
    }

    @Test
    void listMappersHandleNulls() {
        assertNull(GameStateMapper.toPlayerDTO(null));
        assertNull(GameStateMapper.toBombDTO(null));
        assertNull(GameStateMapper.toPowerUpDTO(null));
        assertNull(GameStateMapper.toPlayerDTOList(null));
        assertNull(GameStateMapper.toBombDTOList(null));
        assertNull(GameStateMapper.toPowerUpDTOList(null));
    }

    @Test
    void listMappersMapValues() {
        Player player = Player.builder()
                .id(UUID.randomUUID())
                .username("user")
                .posX(0)
                .posY(0)
                .lifeCount(1)
                .status(PlayerStatus.ALIVE)
                .spawnPoint(new Point(0, 0))
                .bombCount(1)
                .bombRange(1)
                .speed(1)
                .build();
        Bomb bomb = Bomb.builder()
                .id(UUID.randomUUID())
                .posX(1)
                .posY(2)
                .range(2)
                .state(BombState.PLACED)
                .placedTime(1L)
                .explosionDelay(2000L)
                .build();
        PowerUp powerUp = PowerUp.builder()
                .id(UUID.randomUUID())
                .type(PowerUpType.BOMB_COUNT_UP)
                .posX(3)
                .posY(4)
                .value(1)
                .duration(1000L)
                .spawnTime(System.currentTimeMillis())
                .build();

        List<PlayerDTO> players = GameStateMapper.toPlayerDTOList(List.of(player));
        List<BombDTO> bombs = GameStateMapper.toBombDTOList(List.of(bomb));
        List<PowerUpDTO> powerUps = GameStateMapper.toPowerUpDTOList(List.of(powerUp));

        assertEquals(1, players.size());
        assertEquals(1, bombs.size());
        assertEquals(1, powerUps.size());
    }
}
