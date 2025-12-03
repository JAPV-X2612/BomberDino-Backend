package com.arsw.bomberdino.listener;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.arsw.bomberdino.controller.websocket.WebSocketController;
import com.arsw.bomberdino.model.dto.response.BombExplodedDTO;
import com.arsw.bomberdino.model.dto.response.BombPlacedEventDTO;
import com.arsw.bomberdino.model.dto.response.GameStateDTO;
import com.arsw.bomberdino.model.dto.response.PlayerKilledDTO;
import com.arsw.bomberdino.model.dto.response.PlayerMovedEventDTO;
import com.arsw.bomberdino.model.dto.response.PointDTO;
import com.arsw.bomberdino.model.entity.Bomb;
import com.arsw.bomberdino.model.entity.GameSession;
import com.arsw.bomberdino.model.entity.Player;
import com.arsw.bomberdino.model.event.BombExplodedEvent;
import com.arsw.bomberdino.model.event.BombPlacedEvent;
import com.arsw.bomberdino.model.event.PlayerKilledEvent;
import com.arsw.bomberdino.model.event.PlayerMovedEvent;
import com.arsw.bomberdino.model.event.PowerUpCollectedEvent;
import com.arsw.bomberdino.service.impl.GameSessionService;
import com.arsw.bomberdino.util.SequenceNumberManager;

/**
 * Event listener for domain events that require WebSocket broadcasting. Listens
 * to game events and broadcasts updates to connected clients. All methods are
 * async to prevent blocking the event publisher.
 *
 * @author Mapunix, Rivaceratops, Yisus-Rex
 * @version 1.0
 * @since 2025-10-26
 */
@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final WebSocketController webSocketController;
    private final GameSessionService gameSessionService;
    private final SequenceNumberManager sequenceNumberManager;

    private static final String GAME_TOPIC_PREFIX = "/topic/game/";

    public WebSocketEventListener(
            WebSocketController webSocketController,
            GameSessionService gameSessionService,
            SequenceNumberManager sequenceNumberManager) {
        this.webSocketController = webSocketController;
        this.gameSessionService = gameSessionService;
        this.sequenceNumberManager = sequenceNumberManager;
    }

    /**
     * Handles PlayerMovedEvent by broadcasting lightweight movement delta.
     * Payload size: ~100 bytes (vs ~5KB for full state) Frequency: High (every player input)
     *
     * @param event PlayerMovedEvent containing player ID and new position
     */
    @EventListener
    @Async
    public void onPlayerMoved(PlayerMovedEvent event) {
        try {
            String sessionId = event.getSessionId();
            GameSession session = gameSessionService.getSession(sessionId);

            Player player = session.getPlayers().stream()
                    .filter(p -> p.getId().toString().equals(event.getPlayerId()))
                    .findFirst()
                    .orElse(null);

            if (player == null) {
                logger.warn("⚠️ Player {} not found for movement event", event.getPlayerId());
                return;
            }

            PlayerMovedEventDTO dto = PlayerMovedEventDTO.builder()
                    .playerId((String) event.getPlayerId())
                    .newX(player.getPosX())
                    .newY(player.getPosY())
                    .direction(event.getDirection())
                    .sequenceNumber(sequenceNumberManager.getNextSequenceNumber(sessionId))
                    .timestamp(System.currentTimeMillis())
                    .build();

            webSocketController.broadcastPlayerMoved(sessionId, dto);

            logger.debug("📤 Player {} moved to ({}, {}) - sent delta (~100 bytes)",
                    event.getPlayerId(), player.getPosX(), player.getPosY());

        } catch (Exception e) {
            logger.error("❌ Error broadcasting player movement: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles BombPlacedEvent by broadcasting lightweight bomb creation delta.
     * Payload size: ~150 bytes (vs ~5KB for full state) Frequency: Medium (when players place bombs)
     *
     * @param event BombPlacedEvent containing bomb details
     */
    @EventListener
    @Async
    public void onBombPlaced(BombPlacedEvent event) {
        try {
            String sessionId = event.getSessionId();
            GameSession session = gameSessionService.getSession(sessionId);

            Bomb bomb = session.getActiveBombs().stream()
                    .filter(b -> b.getId().toString().equals(event.getBombId()))
                    .findFirst()
                    .orElse(null);

            if (bomb == null) {
                logger.warn("⚠️ Bomb {} not found for placement event", event.getBombId());
                return;
            }

            BombPlacedEventDTO dto = BombPlacedEventDTO.builder()
                    .bombId((String) event.getBombId())
                    .playerId(event.getPlayerId())
                    .x(bomb.getPosX())
                    .y(bomb.getPosY())
                    .range(bomb.getRange())
                    .timeToExplode(bomb.getTimeUntilExplosion())
                    .sequenceNumber(sequenceNumberManager.getNextSequenceNumber(sessionId))
                    .timestamp(System.currentTimeMillis())
                    .build();

            webSocketController.broadcastBombPlaced(sessionId, dto);

            logger.debug("📤 Bomb {} placed at ({}, {}) - sent delta (~150 bytes)",
                    event.getBombId(), bomb.getPosX(), bomb.getPosY());

        } catch (Exception e) {
            logger.error("❌ Error broadcasting bomb placement: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles BombExplodedEvent with FULL STATE synchronization.
     * @param event BombExplodedEvent containing explosion details
     */
    @EventListener
    @Async
    public void onBombExploded(BombExplodedEvent event) {
        try {
            String sessionId = event.getSessionId();

            BombExplodedDTO explosionDto = BombExplodedDTO.builder()
                    .sessionId(sessionId)
                    .bombId(event.getBombId())
                    .affectedTiles(event.getAffectedTiles().stream()
                            .map(point -> PointDTO.builder()
                            .x(point.x)
                            .y(point.y)
                            .build())
                            .collect(Collectors.toList()))
                    .affectedPlayers(event.getAffectedPlayers())
                    .timestamp(System.currentTimeMillis())
                    .build();

            webSocketController.broadcastBombExploded(sessionId, explosionDto);

            GameStateDTO fullState = gameSessionService.getSession(sessionId).getCurrentState();
            webSocketController.broadcastGameState(sessionId, fullState);

            logger.info("💥 Bomb exploded - sent explosion event + full state (~5KB)");

        } catch (Exception e) {
            logger.error("❌ Error broadcasting bomb explosion: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles PlayerKilledEvent with FULL STATE synchronization.
     *
     * @param event PlayerKilledEvent containing killer and victim IDs
     */
    @EventListener
    @Async
    public void onPlayerKilled(PlayerKilledEvent event) {
        try {
            String sessionId = event.getSessionId();

            PlayerKilledDTO dto = PlayerKilledDTO.builder()
                    .sessionId(sessionId)
                    .killerId(event.getKillerId())
                    .victimId(event.getVictimId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            webSocketController.broadcastPlayerKilled(sessionId, dto);

            GameStateDTO fullState = gameSessionService.getSession(sessionId).getCurrentState();
            webSocketController.broadcastGameState(sessionId, fullState);

            logger.info("💀 Player killed - sent kill event + full state (~5KB)");

        } catch (Exception e) {
            logger.error("❌ Error broadcasting player death: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles PowerUpCollectedEvent with FULL STATE synchronization.
     * @param event PowerUpCollectedEvent containing player and power-up data
     */
    @EventListener
    @Async
    public void onPowerUpCollected(PowerUpCollectedEvent event) {
        try {
            String sessionId = event.getSessionId();

            webSocketController.broadcastToSession(sessionId, "/powerup", event);

            GameStateDTO fullState = gameSessionService.getSession(sessionId).getCurrentState();
            webSocketController.broadcastGameState(sessionId, fullState);

            logger.debug("⭐ Power-up collected - sent event + full state");

        } catch (Exception e) {
            logger.error("❌ Error broadcasting power-up collection: {}", e.getMessage(), e);
        }
    }
}
