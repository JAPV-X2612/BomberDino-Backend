package com.arsw.bomberdino.model.event;

import java.time.LocalDateTime;

import com.arsw.bomberdino.model.enums.Direction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerMovedEvent {

    private String sessionId;
    private String playerId;
    private Direction direction;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static PlayerMovedEvent of(String sessionId, String playerId, Direction direction) {
        return PlayerMovedEvent.builder()
                .sessionId(sessionId)
                .playerId(playerId)
                .direction(direction)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public Direction getDirection() {
        return direction;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
