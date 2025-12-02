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
     * ==================== LIGHTWEIGHT EVENTS ==================== These send
     * ONLY what changed (delta updates)
     */
    /**
     * Handles PlayerMovedEvent by broadcasting lightweight movement delta.
     *
     * Payload size: ~100 bytes (vs ~5KB for full state) Frequency: High (every
     * player input)
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
     *
     * Payload size: ~150 bytes (vs ~5KB for full state) Frequency: Medium (when
     * players place bombs)
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
     * ==================== FULL STATE EVENTS ==================== These send
     * complete game state because many things change at once
     */
    /**
     * Handles BombExplodedEvent with FULL STATE synchronization.
     *
     * Why full state? Because explosions affect: - Destroyed blocks (tiles
     * change) - Damaged players (lives, position, status change) - Spawned
     * power-ups (new entities appear) - Removed bomb (entity disappears)
     *
     * Sending individual deltas would require 10+ messages. Full state is
     * actually MORE efficient here.
     *
     * @param event BombExplodedEvent containing explosion details
     */
    @EventListener
    @Async
    public void onBombExploded(BombExplodedEvent event) {
        try {
            String sessionId = event.getSessionId();

            // Send explosion event for visual effects (fireball animation, sound)
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

            // Send FULL STATE for synchronization (many entities changed)
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
     * Why full state? Because deaths affect: - Player lives (decremented) -
     * Player position (respawn point) - Player status (ALIVE/DEAD/SPECTATING) -
     * Killer's score (kills incremented) - Game status (might trigger end
     * condition)
     *
     * @param event PlayerKilledEvent containing killer and victim IDs
     */
    @EventListener
    @Async
    public void onPlayerKilled(PlayerKilledEvent event) {
        try {
            String sessionId = event.getSessionId();

            // Send kill event for UI notifications (kill feed)
            PlayerKilledDTO dto = PlayerKilledDTO.builder()
                    .sessionId(sessionId)
                    .killerId(event.getKillerId())
                    .victimId(event.getVictimId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            webSocketController.broadcastPlayerKilled(sessionId, dto);

            // Send FULL STATE for synchronization (player states changed)
            GameStateDTO fullState = gameSessionService.getSession(sessionId).getCurrentState();
            webSocketController.broadcastGameState(sessionId, fullState);

            logger.info("💀 Player killed - sent kill event + full state (~5KB)");

        } catch (Exception e) {
            logger.error("❌ Error broadcasting player death: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles PowerUpCollectedEvent with FULL STATE synchronization.
     *
     * @param event PowerUpCollectedEvent containing player and power-up data
     */
    @EventListener
    @Async
    public void onPowerUpCollected(PowerUpCollectedEvent event) {
        try {
            String sessionId = event.getSessionId();

            // Send power-up event for visual feedback
            webSocketController.broadcastToSession(sessionId, "/powerup", event);

            // Send FULL STATE (power-up removed, player stats changed)
            GameStateDTO fullState = gameSessionService.getSession(sessionId).getCurrentState();
            webSocketController.broadcastGameState(sessionId, fullState);

            logger.debug("⭐ Power-up collected - sent event + full state");

        } catch (Exception e) {
            logger.error("❌ Error broadcasting power-up collection: {}", e.getMessage(), e);
        }
    }

    /**
     * ==================== IGNORED EVENTS ==================== These are now
     * handled by periodic sync instead of per-action
     */
    /**
     * GameStateChangedEvent is NO LONGER broadcasted immediately.
     *
     * Why? Because: 1. Specific events (PlayerMovedEvent, BombPlacedEvent)
     * handle deltas 2. Critical events (BombExplodedEvent, PlayerKilledEvent)
     * send full state 3. PeriodicSyncScheduler sends full state every 5s as
     * checkpoint
     *
     * Commenting this out ELIMINATES 90% of redundant broadcasts.
     */
    // @EventListener
    // @Async
    // public void onGameStateChanged(GameStateChangedEvent event) {
    //     // DISABLED - Replaced by hybrid delta/sync strategy
    // }
}
