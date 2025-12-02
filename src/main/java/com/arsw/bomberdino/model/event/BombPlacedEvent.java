package com.arsw.bomberdino.model.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain event published when a player places a bomb.
 *
 * This is a LIGHTWEIGHT event used in the hybrid synchronization architecture.
 * Contains only the delta (what changed), not the full game state.
 *
 * Payload size: ~150 bytes (vs ~5KB for full state) Frequency: Medium (when
 * players place bombs)
 *
 * Used by HybridWebSocketEventListener to broadcast BombPlacedEventDTO.
 *
 * @author Mapunix, Rivaceratops, Yisus-Rex
 * @version 1.0
 * @since 2025-12-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BombPlacedEvent {

    private String sessionId;
    private String playerId;
    private String bombId;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Creates a new BombPlacedEvent with current timestamp.
     *
     * @param sessionId session identifier
     * @param playerId player identifier who placed the bomb
     * @param bombId bomb identifier
     * @return BombPlacedEvent instance
     */
    public static BombPlacedEvent of(String sessionId, String playerId, String bombId) {
        return BombPlacedEvent.builder()
                .sessionId(sessionId)
                .playerId(playerId)
                .bombId(bombId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getBombId() {
        return bombId;
    }
}
