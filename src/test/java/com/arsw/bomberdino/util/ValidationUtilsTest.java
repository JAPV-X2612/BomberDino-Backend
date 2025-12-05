package com.arsw.bomberdino.util;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    void requireNonBlankAndNonNull() {
        ValidationUtils.requireNonBlank("ok", "field");
        ValidationUtils.requireNonNull(new Object(), "obj");
        assertThrows(IllegalArgumentException.class, () -> ValidationUtils.requireNonBlank(" ", "id"));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtils.requireNonNull(null, "obj"));
    }

    @Test
    void pointValidations() {
        ValidationUtils.requireValidPoint(new Point(1, 1), "pos");
        assertThrows(IllegalArgumentException.class, () -> ValidationUtils.requireValidPoint(null, "pos"));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtils.requireInMapBounds(new Point(-1, 0), 5, 5));
        ValidationUtils.requireInMapBounds(new Point(4, 4), 5, 5);
    }

    @Test
    void numberValidations() {
        ValidationUtils.requirePositive(1, "value");
        ValidationUtils.requireNonNegative(0, "value");
        assertThrows(IllegalArgumentException.class, () -> ValidationUtils.requirePositive(0, "value"));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtils.requireNonNegative(-1, "value"));
        ValidationUtils.requireInRange(5, 1, 10, "range");
        assertThrows(IllegalArgumentException.class, () -> ValidationUtils.requireInRange(0, 1, 10, "range"));
    }

    @Test
    void validUuidOrThrows() {
        UUID uuid = ValidationUtils.requireValidUUID(UUID.randomUUID().toString(), "id");
        assertNotNull(uuid);
        assertThrows(IllegalArgumentException.class, () -> ValidationUtils.requireValidUUID("not-uuid", "id"));
    }
}
