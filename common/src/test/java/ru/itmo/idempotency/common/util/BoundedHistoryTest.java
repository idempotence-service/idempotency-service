package ru.itmo.idempotency.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BoundedHistoryTest {

    @Test
    void shouldTrimOldestItemsWhenLimitExceeded() {
        BoundedHistory<String> history = new BoundedHistory<>(2);
        history.add("first");
        history.add("second");
        history.add("third");

        Assertions.assertEquals(2, history.size());
        Assertions.assertEquals(java.util.List.of("second", "third"), history.snapshot());
    }

    @Test
    void shouldReturnSnapshotCopy() {
        BoundedHistory<String> history = new BoundedHistory<>(3);
        history.add("one");
        var snapshot = history.snapshot();
        snapshot.clear();
        Assertions.assertEquals(1, history.size());
        Assertions.assertEquals("one", history.snapshot().getFirst());
    }

    @Test
    void shouldClearAllItems() {
        BoundedHistory<String> history = new BoundedHistory<>(3);
        history.add("one");
        history.add("two");
        history.clear();
        Assertions.assertEquals(0, history.size());
        Assertions.assertTrue(history.snapshot().isEmpty());
    }

    @Test
    void shouldRejectNonPositiveLimit() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BoundedHistory<>(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BoundedHistory<>(-1));
    }
}
