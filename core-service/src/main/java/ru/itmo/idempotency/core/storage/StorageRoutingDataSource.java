package ru.itmo.idempotency.core.storage;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class StorageRoutingDataSource extends AbstractRoutingDataSource {

    private final String defaultShardId;

    public StorageRoutingDataSource(String defaultShardId) {
        this.defaultShardId = defaultShardId;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String currentShardId = StorageShardContext.currentShardId();
        return currentShardId != null ? currentShardId : defaultShardId;
    }
}
