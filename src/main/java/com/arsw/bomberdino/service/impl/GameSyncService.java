package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.controller.websocket.WebSocketController;
import com.arsw.bomberdino.model.dto.response.GameStateDTO;
import com.arsw.bomberdino.model.dto.response.HeartbeatEventDTO;
import com.arsw.bomberdino.model.entity.GameSession;
import com.arsw.bomberdino.model.entity.Player;
import com.arsw.bomberdino.model.enums.GameStatus;
import com.arsw.bomberdino.model.enums.PlayerStatus;
import com.arsw.bomberdino.util.SequenceNumberManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameSyncService {

    private static final Logger logger = LoggerFactory.getLogger(GameSyncService.class);

    private final GameSessionService gameSessionService;
    private final WebSocketController webSocketController;
    private final SequenceNumberManager sequenceNumberManager;

    /**
     *
     * Sends heartbeat events to all active game sessions.
     *
     * Runs every 500ms to keep WebSocket connections alive and allow
     *
     * clients to detect connection loss.
     *
     *
     *
     * Heartbeats include:
     *
     * - Session ID
     *
     * - Game status
     *
     * - Sequence number (to detect packet loss)
     *
     * - Timestamp
     *
     * - Alive players count
     *
     */
    @Scheduled(fixedRate = 500) // Every 500ms
    public void sendHeartbeats() {
        try {
            var activeSessions = gameSessionService.getSessionsByStatus(GameStatus.IN_PROGRESS);

            for (GameSession session : activeSessions) {
                String sessionId = session.getSessionId().toString();

                int alivePlayersCount = (int) session.getPlayers().stream()
                        .filter(p -> p.getStatus() == PlayerStatus.ALIVE).count();

                HeartbeatEventDTO heartbeat = HeartbeatEventDTO.builder().sessionId(sessionId)
                        .status(session.getStatus())
                        .sequenceNumber(sequenceNumberManager.getNextSequenceNumber(sessionId))
                        .timestamp(System.currentTimeMillis()).alivePlayersCount(alivePlayersCount)
                        .build();

                webSocketController.broadcastHeartbeat(sessionId, heartbeat);
            }
        } catch (Exception e) {
            logger.error("❌ Error sending heartbeats: {}", e.getMessage(), e);
        }
    }

    /**
     *
     * Sends periodic full state synchronization to all active sessions.
     *
     * Runs every 5 seconds to prevent drift from accumulated delta updates.
     *
     *
     *
     * This acts as a "checkpoint" that resets any accumulated errors from
     *
     * lost packets or client-side bugs.
     *
     */
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void sendPeriodicSync() {
        try {
            var activeSessions = gameSessionService.getSessionsByStatus(GameStatus.IN_PROGRESS);

            for (GameSession session : activeSessions) {
                String sessionId = session.getSessionId().toString();
                GameStateDTO fullState = session.getCurrentState();

                webSocketController.broadcastPeriodicSync(sessionId, fullState);
                logger.debug("🔄 Sent periodic sync to session {} ({} players, {} bombs)",
                        sessionId, fullState.getPlayers().size(), fullState.getBombs().size());
            }
        } catch (Exception e) {
            logger.error("❌ Error sending periodic sync: {}", e.getMessage(), e);
        }
    }

    /**
     *
     * Cleans up sequence numbers for finished sessions.
     *
     * Runs every minute to prevent memory leaks.
     *
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void cleanupFinishedSessions() {
        try {
            var finishedSessions = gameSessionService.getSessionsByStatus(GameStatus.FINISHED);

            for (GameSession session : finishedSessions) {
                String sessionId = session.getSessionId().toString();
                sequenceNumberManager.resetSequence(sessionId);
                logger.debug("🧹 Cleaned up sequence numbers for finished session {}", sessionId);
            }
        } catch (Exception e) {
            logger.error("❌ Error cleaning up finished sessions: {}", e.getMessage(), e);
        }
    }
}
