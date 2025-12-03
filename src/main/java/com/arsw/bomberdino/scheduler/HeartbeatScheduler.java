package com.arsw.bomberdino.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.arsw.bomberdino.controller.websocket.WebSocketController;
import com.arsw.bomberdino.model.dto.response.HeartbeatEventDTO;
import com.arsw.bomberdino.model.entity.GameSession;
import com.arsw.bomberdino.model.enums.GameStatus;
import com.arsw.bomberdino.service.impl.GameSessionService;
import com.arsw.bomberdino.util.SequenceNumberManager;

import lombok.RequiredArgsConstructor;

/**
 * Scheduler that sends periodic heartbeat events to all active game sessions.
 *
 * Heartbeats serve two purposes: 1. Keep WebSocket connections alive 2. Allow
 * clients to detect missed messages via sequence number gaps
 *
 * Runs every 500ms for active sessions.
 *
 * @author Mapunix, Rivaceratops, Yisus-Rex
 * @version 1.0
 * @since 2025-12-01
 */
@Component
@RequiredArgsConstructor
public class HeartbeatScheduler {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatScheduler.class);

    private final GameSessionService gameSessionService;
    private final WebSocketController webSocketController;
    private final SequenceNumberManager sequenceNumberManager;

    /**
     * Sends heartbeat events to all IN_PROGRESS sessions every 500ms.
     *
     * Each heartbeat includes: - Session ID - Current game status - Sequence
     * number (for detecting lost messages) - Timestamp (for latency
     * calculation) - Alive players count (quick status check)
     */
    @Scheduled(fixedRate = 500) // Every 500ms
    public void sendHeartbeats() {
        try {
            // Get all active sessions
            gameSessionService.getSessionsByStatus(GameStatus.IN_PROGRESS).forEach(session -> {
                try {
                    sendHeartbeatForSession(session);
                } catch (Exception e) {
                    logger.error("❌ Error sending heartbeat for session {}: {}",
                            session.getSessionId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            logger.error("❌ Error in heartbeat scheduler: {}", e.getMessage());
        }
    }

    /**
     * Sends a single heartbeat event for a specific session.
     *
     * @param session the game session to send heartbeat for
     */
    private void sendHeartbeatForSession(GameSession session) {
        String sessionId = session.getSessionId().toString();

        long alivePlayersCount = session.getPlayers().stream()
                .filter(p -> p.isAlive())
                .count();

        HeartbeatEventDTO heartbeat = HeartbeatEventDTO.builder()
                .sessionId(sessionId)
                .status(session.getStatus())
                .sequenceNumber(sequenceNumberManager.getNextSequenceNumber(sessionId))
                .timestamp(System.currentTimeMillis())
                .alivePlayersCount((int) alivePlayersCount)
                .build();

        webSocketController.broadcastHeartbeat(sessionId, heartbeat);
    }
}
