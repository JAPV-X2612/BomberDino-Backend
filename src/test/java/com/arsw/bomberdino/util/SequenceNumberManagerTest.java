package com.arsw.bomberdino.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SequenceNumberManagerTest {

    private final SequenceNumberManager manager = new SequenceNumberManager();

    @Test
    void getNextSequenceNumberStartsAtOneAndIncrementsPerSession() {
        long first = manager.getNextSequenceNumber("session-1");
        long second = manager.getNextSequenceNumber("session-1");
        long otherSession = manager.getNextSequenceNumber("session-2");

        assertEquals(1L, first);
        assertEquals(2L, second);
        assertEquals(1L, otherSession);
    }

    @Test
    void getCurrentSequenceNumberReturnsZeroWhenSessionUnknown() {
        assertEquals(0L, manager.getCurrentSequenceNumber("missing"));
    }

    @Test
    void getCurrentSequenceNumberReturnsLatestValue() {
        manager.getNextSequenceNumber("session-1");
        manager.getNextSequenceNumber("session-1");

        assertEquals(2L, manager.getCurrentSequenceNumber("session-1"));
    }

    @Test
    void resetSequenceClearsCounter() {
        manager.getNextSequenceNumber("session-1");
        manager.getNextSequenceNumber("session-1");

        manager.resetSequence("session-1");

        assertEquals(0L, manager.getCurrentSequenceNumber("session-1"));
        assertEquals(1L, manager.getNextSequenceNumber("session-1"));
    }

    @Test
    void clearAllRemovesAllSessions() {
        manager.getNextSequenceNumber("session-1");
        manager.getNextSequenceNumber("session-2");

        manager.clearAll();

        assertEquals(0L, manager.getCurrentSequenceNumber("session-1"));
        assertEquals(0L, manager.getCurrentSequenceNumber("session-2"));
    }

    @Test
    void operationsRejectNullOrBlankSessionIds() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.getNextSequenceNumber(null));
        assertThrows(IllegalArgumentException.class,
                () -> manager.getNextSequenceNumber("  "));
        assertThrows(IllegalArgumentException.class,
                () -> manager.getCurrentSequenceNumber(""));
        assertThrows(IllegalArgumentException.class,
                () -> manager.resetSequence(null));
    }
}
