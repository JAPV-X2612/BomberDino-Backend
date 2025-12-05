package com.arsw.bomberdino.service.impl;

import com.arsw.bomberdino.exception.ValidationException;
import com.arsw.bomberdino.model.entity.Bomb;
import com.arsw.bomberdino.model.enums.BombState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.awt.Point;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BombServiceTest {

    @Mock
    private CollisionService collisionService;

    @InjectMocks
    private BombService service;

    private final Point position = new Point(2, 3);

    @BeforeEach
    void setup() {
        when(collisionService.isValidPosition(any(), any())).thenReturn(true);
    }

    @Test
    void placeBombCreatesPlacedBombWhenPositionValid() {
        Bomb bomb = service.placeBomb("session-1", "player-1", position, 3);

        assertNotNull(bomb);
        assertEquals(BombState.PLACED, bomb.getState());
        assertEquals(position.x, bomb.getPosX());
        assertEquals(position.y, bomb.getPosY());
        assertEquals(2, bomb.getRange()); // hardcoded in service

        List<Bomb> active = service.getActiveBombs("session-1");
        assertTrue(active.stream().anyMatch(b -> b.getId().equals(bomb.getId())));
    }

    @Test
    void placeBombReturnsNullWhenPositionInvalid() {
        when(collisionService.isValidPosition(eq("session-1"), eq(position))).thenReturn(false);

        Bomb bomb = service.placeBomb("session-1", "player-1", position, 3);

        assertNull(bomb);
    }

    @Test
    void placeBombValidatesInputs() {
        assertThrows(ValidationException.class,
                () -> service.placeBomb(null, "player-1", position, 3));
        assertThrows(ValidationException.class,
                () -> service.placeBomb("session-1", " ", position, 3));
        assertThrows(ValidationException.class,
                () -> service.placeBomb("session-1", "player-1", null, 3));
    }

    @Test
    void explodeBombReturnsCrossPatternAndUpdatesState() {
        Bomb bomb = service.placeBomb("session-1", "player-1", position, 3);
        String bombId = bomb.getId().toString();

        List<Point> affected = service.explodeBomb(bombId);

        assertEquals(1 + 4 * bomb.getRange(), affected.size());
        assertTrue(affected.contains(new Point(position.x, position.y)));
        assertEquals(BombState.EXPLODED, bomb.getState());
    }

    @Test
    void explodeBombValidatesStateAndExistence() {
        assertThrows(ValidationException.class, () -> service.explodeBomb(null));
        assertThrows(IllegalStateException.class, () -> service.explodeBomb("missing"));

        Bomb bomb = service.placeBomb("session-1", "player-1", position, 3);
        bomb.setState(BombState.EXPLODED);

        assertThrows(IllegalStateException.class, () -> service.explodeBomb(bomb.getId().toString()));
        assertThrows(ValidationException.class, () -> service.explodeBomb(" "));
    }

    @Test
    void getActiveBombsValidatesSessionId() {
        assertThrows(ValidationException.class, () -> service.getActiveBombs(null));
        assertThrows(ValidationException.class, () -> service.getActiveBombs(" "));
    }

    @Test
    void isReadyToExplodeReflectsCountdownAndMissingBomb() {
        Bomb bomb = service.placeBomb("session-1", "player-1", position, 3);
        String bombId = bomb.getId().toString();

        bomb.setPlacedTime(System.currentTimeMillis() - 5000);
        bomb.setExplosionDelay(1000);

        assertTrue(service.isReadyToExplode(bombId));
        assertFalse(service.isReadyToExplode("missing"));
        assertThrows(ValidationException.class, () -> service.isReadyToExplode(" "));
    }

    @Test
    void removeBombDeletesFromStorage() {
        Bomb bomb = service.placeBomb("session-1", "player-1", position, 3);
        String bombId = bomb.getId().toString();

        service.removeBomb(bombId);

        assertFalse(service.isReadyToExplode(bombId));
        assertTrue(service.getActiveBombs("session-1").isEmpty());
        assertThrows(ValidationException.class, () -> service.removeBomb(""));
    }

    @Test
    void scheduleBombExplosionRejectsTooShortDelayOrInvalidId() {
        assertThrows(ValidationException.class, () -> service.scheduleBombExplosion(null, 1000));
        Bomb bomb = service.placeBomb("session-1", "player-1", position, 3);
        assertThrows(IllegalArgumentException.class,
                () -> service.scheduleBombExplosion(bomb.getId().toString(), 500));
    }

    @Test
    void scheduleBombExplosionRunsWhenReadyAndRemovesBomb() throws Exception {
        Bomb bomb = service.placeBomb("session-1", "player-1", position, 3);
        bomb.setPlacedTime(System.currentTimeMillis() - 5_000);
        bomb.setExplosionDelay(1_000);

        ImmediateScheduler scheduler = new ImmediateScheduler();
        setScheduler(scheduler);

        service.scheduleBombExplosion(bomb.getId().toString(), 1_000);

        assertTrue(scheduler.ranTask);
        assertFalse(service.isReadyToExplode(bomb.getId().toString()));
    }

    @Test
    void scheduleBombExplosionSkipsWhenNotReady() throws Exception {
        Bomb bomb = service.placeBomb("session-1", "player-1", position, 3);
        bomb.setExplosionDelay(10_000_000); // far in future

        ImmediateScheduler scheduler = new ImmediateScheduler();
        setScheduler(scheduler);

        service.scheduleBombExplosion(bomb.getId().toString(), 1_000);

        assertTrue(scheduler.ranTask);
        assertFalse(service.isReadyToExplode(bomb.getId().toString())); // not ready -> not removed
        assertEquals(BombState.PLACED, bomb.getState());
    }

    @Test
    void shutdownTriggersShutdownNowWhenNotTerminated() throws Exception {
        ImmediateScheduler scheduler = new ImmediateScheduler();
        scheduler.awaitReturn = false; // force shutdownNow branch
        setScheduler(scheduler);

        service.shutdown();

        assertTrue(scheduler.shutdownCalled);
        assertTrue(scheduler.shutdownNowCalled);
    }

    private void setScheduler(ScheduledExecutorService scheduler) throws Exception {
        Field field = BombService.class.getDeclaredField("explosionScheduler");
        field.setAccessible(true);
        field.set(service, scheduler);
    }

    private static class ImmediateScheduler implements ScheduledExecutorService {
        boolean shutdownCalled;
        boolean shutdownNowCalled;
        boolean ranTask;
        boolean awaitReturn = true;

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            ranTask = true;
            command.run();
            return new ImmediateScheduledFuture();
        }

        @Override
        public void shutdown() {
            shutdownCalled = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdownNowCalled = true;
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdownCalled;
        }

        @Override
        public boolean isTerminated() {
            return shutdownCalled && awaitReturn;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return awaitReturn;
        }

        // Unused methods for these tests
        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
                TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<?> submit(Runnable task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void execute(Runnable command) {
            throw new UnsupportedOperationException();
        }
    }

    private static class ImmediateScheduledFuture implements ScheduledFuture<Object> {
        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(java.util.concurrent.Delayed o) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            return null;
        }
    }

}
