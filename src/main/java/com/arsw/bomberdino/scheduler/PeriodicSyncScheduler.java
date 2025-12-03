package com.arsw.bomberdino.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.arsw.bomberdino.controller.websocket.WebSocketController;
import com.arsw.bomberdino.model.dto.response.GameStateDTO;
import com.arsw.bomberdino.model.entity.GameSession;
import com.arsw.bomberdino.model.enums.GameStatus;
import com.arsw.bomberdino.service.impl.GameSessionService;

import lombok.RequiredArgsConstructor;

/**
 * Scheduler that sends periodic full game state synchronization checkpoints.
 *
 * Purpose: Prevent state drift from accumulated delta updates
 *
 * Strategy: - Every 5 seconds, send complete game state to all clients -
 * Clients use this as "source of truth" to correct any drift - Much less
 * frequent than per-action updates (5s vs 60fps)
 *
 * This is a KEY component of the hybrid architecture: - Delta updates
 * (player-moved, bomb-placed) = 95% of traffic - Periodic sync (this) = 5% of
 * traffic but ensures accuracy
 *
 * @author Mapunix, Rivaceratops, Yisus-Rex
 * @version 1.0
 * @since 2025-12-01
 */
@Component
@RequiredArgsConstructor
public class PeriodicSyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PeriodicSyncScheduler.class);

    private final GameSessionService gameSessionService;
    private final WebSocketController webSocketController;

    /**
     * Sends full game state synchronization every 5 seconds.
     *
     * This "checkpoint" ensures that even if clients miss some delta events,
     * they will eventually resynchronize with the server state.
     *
     * Trade-off: - Bandwidth: ~5KB every 5s = minimal impact - Accuracy:
     * Prevents indefinite drift accumulation
     */
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void sendPeriodicSync() {
        try {
            // Only sync sessions that are actively playing
            gameSessionService.getSessionsByStatus(GameStatus.IN_PROGRESS).forEach(session -> {
                try {
                    sendSyncForSession(session);
                } catch (Exception e) {
                    logger.error("❌ Error sending periodic sync for session {}: {}",
                            session.getSessionId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            logger.error("❌ Error in periodic sync scheduler: {}", e.getMessage());
        }
    }

    /**
     * Sends full state synchronization for a specific session.
     *
     * @param session the game session to synchronize
     */
    private void sendSyncForSession(GameSession session) {
        String sessionId = session.getSessionId().toString();

        // Get complete current state
        GameStateDTO fullState = session.getCurrentState();

        // Send to dedicated /sync topic (different from /state)
        webSocketController.broadcastPeriodicSync(sessionId, fullState);

        logger.debug("🔄 Periodic sync sent to session {} (players: {}, bombs: {}, powerUps: {})",
                sessionId,
                fullState.getPlayers().size(),
                fullState.getBombs().size(),
                fullState.getPowerUps().size());
    }
}
