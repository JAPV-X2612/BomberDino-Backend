package com.arsw.bomberdino.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SequenceNumberManager {

    private final ConcurrentHashMap<String, AtomicLong> sequenceCounters =
            new ConcurrentHashMap<>();

    public long getNextSequenceNumber(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID cannot be null or blank");
        }

        return sequenceCounters.computeIfAbsent(sessionId, k -> new AtomicLong(0))
                .incrementAndGet();
    }

    public long getCurrentSequenceNumber(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID cannot be null or blank");
        }

        AtomicLong counter = sequenceCounters.get(sessionId);
        return counter != null ? counter.get() : 0;
    }

    public void resetSequence(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID cannot be null or blank");
        }

        sequenceCounters.remove(sessionId);
    }

    public void clearAll() {
        sequenceCounters.clear();
    }
}
