package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.model.enums.GameStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled service for periodic game state snapshots.
 * Logs complete state every 5 seconds for active sessions.
 *
 * @author Mapunix, Rivaceratops, Yisus-Rex
 * @version 1.0
 * @since 2025-12-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledGameStateLogger {

    private final GameSessionService gameSessionService;
    private final RedisGameStateLogger redisLogger;

    /**
     * Captures snapshots of all active game sessions every 5 seconds.
     * Only logs sessions in IN_PROGRESS status to avoid unnecessary writes.
     */
    @Scheduled(fixedRate = 5000, initialDelay = 5000)
    public void logActiveGameStates() {
        try {
            var activeSessions = gameSessionService.getSessionsByStatus(GameStatus.IN_PROGRESS);

            if (activeSessions.isEmpty()) {
                return;
            }

            log.debug("Logging {} active game session snapshots", activeSessions.size());

            activeSessions.forEach(session -> {
                String sessionId = session.getSessionId().toString();
                redisLogger.logGameState(sessionId, session, "SNAPSHOT");
            });

        } catch (Exception e) {
            log.error("Error during scheduled game state logging: {}", e.getMessage());
        }
    }
}
