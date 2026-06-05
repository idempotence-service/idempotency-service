package ru.itmo.idempotency.common.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class BoundedHistory<T> {

    private final int limit;
    private final Deque<T> items = new ArrayDeque<>();

    public BoundedHistory(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("History limit must be positive");
        }
        this.limit = limit;
    }

    public synchronized void add(T item) {
        if (items.size() >= limit) {
            items.removeFirst();
        }
        items.addLast(item);
    }

    public synchronized List<T> snapshot() {
        return new ArrayList<>(items);
    }

    public synchronized int size() {
        return items.size();
    }

    public synchronized void clear() {
        items.clear();
    }
}
