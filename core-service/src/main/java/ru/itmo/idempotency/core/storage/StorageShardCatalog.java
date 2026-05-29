package ru.itmo.idempotency.core.storage;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.DisposableBean;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class StorageShardCatalog implements DisposableBean {

    private static final long HASH_SEED = 0x9E3779B97F4A7C15L;

    private final List<ShardDataSource> shards;
    private final StorageRoutingDataSource routingDataSource;
    private final AtomicInteger scanCursor = new AtomicInteger();

    public StorageShardCatalog(List<ShardDataSource> shards) {
        if (shards == null || shards.isEmpty()) {
            throw new IllegalArgumentException("At least one storage shard must be configured");
        }

        this.shards = List.copyOf(shards);
        this.routingDataSource = createRoutingDataSource(this.shards);
    }

    public List<ShardDataSource> shards() {
        return shards;
    }

    public String defaultShardId() {
        return shards.get(0).shardId();
    }

    public String shardIdForKey(String shardKey) {
        if (shardKey == null || shardKey.isBlank() || shards.size() == 1) {
            return defaultShardId();
        }

        String selectedShardId = defaultShardId();
        long selectedScore = Long.MIN_VALUE;
        for (ShardDataSource shard : shards) {
            long score = rendezvousScore(shardKey, shard.shardId());
            if (score > selectedScore) {
                selectedScore = score;
                selectedShardId = shard.shardId();
            }
        }
        return selectedShardId;
    }

    public List<String> shardIdsInScanOrder() {
        if (shards.size() == 1) {
            return List.of(defaultShardId());
        }

        int startIndex = Math.floorMod(scanCursor.getAndIncrement(), shards.size());
        List<String> orderedShardIds = new ArrayList<>(shards.size());
        for (int index = 0; index < shards.size(); index++) {
            orderedShardIds.add(shards.get((startIndex + index) % shards.size()).shardId());
        }
        return orderedShardIds;
    }

    public DataSource routingDataSource() {
        return routingDataSource;
    }

    @Override
    public void destroy() {
        for (ShardDataSource shard : shards) {
            if (shard.dataSource() instanceof HikariDataSource hikariDataSource) {
                hikariDataSource.close();
            }
        }
    }

    private StorageRoutingDataSource createRoutingDataSource(List<ShardDataSource> configuredShards) {
        Map<Object, Object> targetDataSources = new LinkedHashMap<>();
        for (ShardDataSource shard : configuredShards) {
            targetDataSources.put(shard.shardId(), shard.dataSource());
        }

        StorageRoutingDataSource dataSource = new StorageRoutingDataSource(configuredShards.get(0).shardId());
        dataSource.setDefaultTargetDataSource(configuredShards.get(0).dataSource());
        dataSource.setTargetDataSources(targetDataSources);
        dataSource.afterPropertiesSet();
        return dataSource;
    }

    private long rendezvousScore(String shardKey, String shardId) {
        long hash = HASH_SEED;
        String compositeKey = shardKey + '#' + shardId;
        for (int index = 0; index < compositeKey.length(); index++) {
            hash ^= compositeKey.charAt(index);
            hash *= 0xff51afd7ed558ccdL;
            hash = Long.rotateLeft(hash, 13);
        }
        return mix64(hash);
    }

    private long mix64(long value) {
        long mixed = value;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return mixed;
    }

    public record ShardDataSource(String shardId, DataSource dataSource) {
        public ShardDataSource {
            Objects.requireNonNull(shardId, "shardId");
            Objects.requireNonNull(dataSource, "dataSource");
        }
    }
}
