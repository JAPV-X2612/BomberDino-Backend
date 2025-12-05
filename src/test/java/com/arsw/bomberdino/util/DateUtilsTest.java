package com.arsw.bomberdino.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void formatAndSecondsBetweenHandleNulls() {
        assertNull(DateUtils.format(null));
        assertEquals(0, DateUtils.secondsBetween(null, LocalDateTime.now()));
        assertEquals(0, DateUtils.secondsBetween(LocalDateTime.now(), null));
    }

    @Test
    void formatProducesExpectedPattern() {
        LocalDateTime date = LocalDateTime.of(2025, 1, 2, 3, 4, 5);
        assertEquals("2025-01-02 03:04:05", DateUtils.format(date));
    }

    @Test
    void conversionsBetweenMillisAndSeconds() {
        assertEquals(2, DateUtils.millisToSeconds(2500));
        assertEquals(5000, DateUtils.secondsToMillis(5));
    }

    @Test
    void expirationAndRemainingTime() {
        long start = System.currentTimeMillis() - 10;
        assertTrue(DateUtils.hasExpired(start, 1));
        long notExpiredStart = System.currentTimeMillis() + 1000;
        assertFalse(DateUtils.hasExpired(notExpiredStart, 10));

        long remaining = DateUtils.remainingTime(System.currentTimeMillis(), 50);
        assertTrue(remaining <= 50 && remaining >= 0);
        assertEquals(0, DateUtils.remainingTime(System.currentTimeMillis() - 100, 10));
    }
}
