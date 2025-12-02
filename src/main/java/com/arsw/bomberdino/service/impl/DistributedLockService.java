package com.arsw.bomberdino.service.impl;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Service for distributed locking using Redisson.
 * Prevents race conditions in multi-instance deployments.
 *
 * @author Yisus-Rex
 * @version 1.0
 * @since 2025-12-01
 */
@Service
public class DistributedLockService {

    private static final Logger logger = LoggerFactory.getLogger(DistributedLockService.class);
    private static final String LOCK_KEY_PREFIX = "game:";
    private static final String LOCK_KEY_SUFFIX = ":lock";
    private static final long WAIT_TIME_SECONDS = 5;
    private static final long LEASE_TIME_SECONDS = 30;

    private final RedissonClient redissonClient;

    public DistributedLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * Executes operation with distributed lock protection.
     *
     * @param sessionId unique session identifier
     * @param operation operation to execute under lock
     * @param <T> return type of operation
     * @return operation result
     * @throws RuntimeException if lock acquisition fails or operation throws
     */
    public <T> T executeWithLock(UUID sessionId, Supplier<T> operation) {
        String lockKey = buildLockKey(sessionId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(WAIT_TIME_SECONDS, LEASE_TIME_SECONDS, TimeUnit.SECONDS);

            if (!acquired) {
                logger.warn("Failed to acquire lock for session {} after {}s", sessionId, WAIT_TIME_SECONDS);
                throw new RuntimeException("Could not acquire distributed lock");
            }

            logger.debug("Lock acquired for session: {}", sessionId);
            return operation.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Lock acquisition interrupted for session {}", sessionId, e);
            throw new RuntimeException("Lock acquisition interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                logger.debug("Lock released for session: {}", sessionId);
            }
        }
    }

    /**
     * Builds Redis key for distributed lock.
     *
     * @param sessionId unique session identifier
     * @return formatted lock key
     */
    private String buildLockKey(UUID sessionId) {
        return LOCK_KEY_PREFIX + sessionId + LOCK_KEY_SUFFIX;
    }
}
