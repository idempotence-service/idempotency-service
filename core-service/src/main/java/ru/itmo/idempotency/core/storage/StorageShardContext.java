package ru.itmo.idempotency.core.storage;

import java.util.ArrayDeque;
import java.util.Deque;

public final class StorageShardContext {

    private static final ThreadLocal<Deque<String>> SHARD_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private StorageShardContext() {
    }

    public static Scope open(String shardId) {
        if (shardId == null || shardId.isBlank()) {
            return Scope.NOOP;
        }

        Deque<String> stack = SHARD_STACK.get();
        stack.push(shardId);
        return new Scope(true);
    }

    public static String currentShardId() {
        Deque<String> stack = SHARD_STACK.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    public static final class Scope implements AutoCloseable {

        private static final Scope NOOP = new Scope(false);

        private final boolean active;

        private Scope(boolean active) {
            this.active = active;
        }

        @Override
        public void close() {
            if (!active) {
                return;
            }

            Deque<String> stack = SHARD_STACK.get();
            if (!stack.isEmpty()) {
                stack.pop();
            }
            if (stack.isEmpty()) {
                SHARD_STACK.remove();
            }
        }
    }
}
