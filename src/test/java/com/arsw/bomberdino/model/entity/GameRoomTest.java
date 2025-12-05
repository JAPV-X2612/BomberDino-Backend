package com.arsw.bomberdino.model.entity;

import com.arsw.bomberdino.model.enums.GameStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameRoomTest {

    private GameRoom newRoom(int maxPlayers) {
        return GameRoom.builder()
                .roomId(UUID.randomUUID())
                .name("room")
                .roomCode("ABCDEF")
                .hostUserId(UUID.randomUUID())
                .playerIds(new ArrayList<>())
                .maxPlayers(maxPlayers)
                .status(GameStatus.WAITING)
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }

    @Test
    void addAndRemovePlayersUpdateHost() {
        GameRoom room = newRoom(2);
        room.addPlayer(UUID.randomUUID().toString());
        assertEquals(1, room.getPlayerIds().size());
        room.removePlayer(room.getPlayerIds().get(0).toString());
        assertTrue(room.getPlayerIds().isEmpty());
    }

    @Test
    void addPlayerValidatesState() {
        GameRoom room = newRoom(2);
        room.setStatus(GameStatus.IN_PROGRESS);
        assertThrows(IllegalStateException.class, () -> room.addPlayer(UUID.randomUUID().toString()));
    }

    @Test
    void addPlayerRejectsInvalidOrFull() {
        GameRoom room = newRoom(1);
        String id = UUID.randomUUID().toString();
        room.addPlayer(id);
        assertThrows(IllegalStateException.class, () -> room.addPlayer(UUID.randomUUID().toString()));
        assertThrows(IllegalArgumentException.class, () -> room.addPlayer(" "));
    }

    @Test
    void createSessionRequiresWaitingAndTwoPlayers() {
        GameRoom room = newRoom(2);
        room.getPlayerIds().add(UUID.randomUUID());
        assertThrows(IllegalStateException.class, room::createSession);
        room.getPlayerIds().add(UUID.randomUUID());
        GameSession session = room.createSession();
        assertEquals(GameStatus.STARTING, room.getStatus());
        assertEquals(GameStatus.STARTING, session.getStatus());
    }
}
