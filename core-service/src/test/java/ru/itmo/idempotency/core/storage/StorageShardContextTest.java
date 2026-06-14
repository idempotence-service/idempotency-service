package ru.itmo.idempotency.core.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StorageShardContextTest {

    @AfterEach
    void tearDown() {
        while (StorageShardContext.currentShardId() != null) {
            StorageShardContext.open(StorageShardContext.currentShardId()).close();
        }
    }

    @Test
    void shouldExposeCurrentShardId() {
        try (StorageShardContext.Scope ignored = StorageShardContext.open("shard-a")) {
            Assertions.assertEquals("shard-a", StorageShardContext.currentShardId());
        }
        Assertions.assertNull(StorageShardContext.currentShardId());
    }

    @Test
    void shouldSupportNestedScopes() {
        try (StorageShardContext.Scope outer = StorageShardContext.open("outer")) {
            Assertions.assertEquals("outer", StorageShardContext.currentShardId());
            try (StorageShardContext.Scope inner = StorageShardContext.open("inner")) {
                Assertions.assertEquals("inner", StorageShardContext.currentShardId());
            }
            Assertions.assertEquals("outer", StorageShardContext.currentShardId());
        }
        Assertions.assertNull(StorageShardContext.currentShardId());
    }

    @Test
    void shouldIgnoreBlankShardId() {
        try (StorageShardContext.Scope ignored = StorageShardContext.open("  ")) {
            Assertions.assertNull(StorageShardContext.currentShardId());
        }
        Assertions.assertNull(StorageShardContext.currentShardId());
    }
}
