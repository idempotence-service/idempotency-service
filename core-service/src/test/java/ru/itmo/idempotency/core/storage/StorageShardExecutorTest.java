package ru.itmo.idempotency.core.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.itmo.idempotency.common.config.RouteModels;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

class StorageShardExecutorTest {

    private StorageShardCatalog shardCatalog;
    private StorageShardExecutor executor;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource shardA = dataSource("shard-a-mem");
        DriverManagerDataSource shardB = dataSource("shard-b-mem");
        shardCatalog = new StorageShardCatalog(List.of(
                new StorageShardCatalog.ShardDataSource("shard-a", shardA),
                new StorageShardCatalog.ShardDataSource("shard-b", shardB)
        ));
        PlatformTransactionManager transactionManager = new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                shardCatalog.routingDataSource()
        );
        executor = new StorageShardExecutor(shardCatalog, transactionManager);
    }

    @AfterEach
    void tearDown() throws Exception {
        shardCatalog.destroy();
        while (StorageShardContext.currentShardId() != null) {
            StorageShardContext.open(StorageShardContext.currentShardId()).close();
        }
    }

    @Test
    void shouldResolveShardIdForKey() {
        Assertions.assertEquals("shard-a", executor.shardIdForKey("gk-1"));
        Assertions.assertEquals(List.of("shard-a", "shard-b"), executor.shardIdsInScanOrder().subList(0, 2));
    }

    @Test
    void shouldRunOnKeyAndRoute() {
        RouteModels.RouteSnapshot route = new RouteModels.RouteSnapshot("svc", "int", null, null, null, null, true);

        String shardFromKey = executor.runOnKey("gk-1", () -> StorageShardContext.currentShardId());
        String shardFromRoute = executor.runOnRoute(route, () -> StorageShardContext.currentShardId());

        Assertions.assertNotNull(shardFromKey);
        Assertions.assertNotNull(shardFromRoute);
        Assertions.assertEquals(executor.routeKey(route), "svc:int");
    }

    @Test
    void shouldReturnNullRouteKeyForNullRoute() {
        Assertions.assertNull(executor.routeKey(null));
    }

    @Test
    void shouldFindFirstPresentInTransaction() {
        AtomicInteger calls = new AtomicInteger();
        Optional<String> found = executor.firstPresentInTransaction(shardId -> {
            calls.incrementAndGet();
            return "shard-b".equals(shardId) ? Optional.of("hit") : Optional.empty();
        });

        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals("hit", found.get());
        Assertions.assertTrue(calls.get() >= 2);
    }

    @Test
    void shouldReturnFirstTrueInTransaction() {
        boolean result = executor.firstTrueInTransaction(shardId -> "shard-a".equals(shardId));

        Assertions.assertTrue(result);
    }

    @Test
    void shouldCollectFromAllReadOnly() {
        List<String> collected = executor.collectFromAllReadOnly(shardId -> List.of(shardId));

        Assertions.assertEquals(2, collected.size());
        Assertions.assertTrue(collected.contains("shard-a"));
        Assertions.assertTrue(collected.contains("shard-b"));
    }

    @Test
    void shouldSumLongReadOnly() {
        long sum = executor.sumLongReadOnly(shardId -> shardId.length());

        Assertions.assertEquals("shard-a".length() + "shard-b".length(), sum);
    }

    @Test
    void shouldRunReadOnlyOnKey() {
        String shardId = executor.runReadOnlyOnKey("gk-1", () -> StorageShardContext.currentShardId());

        Assertions.assertNotNull(shardId);
    }

    @Test
    void shouldRunInTransactionOnKeyShardAndRoute() {
        RouteModels.RouteSnapshot route = new RouteModels.RouteSnapshot("svc", "int", null, null, null, null, true);

        Assertions.assertNotNull(executor.runInTransactionOnKey("gk-1", () -> StorageShardContext.currentShardId()));
        Assertions.assertNotNull(executor.runInTransactionOnShard("shard-a", () -> StorageShardContext.currentShardId()));
        Assertions.assertNotNull(executor.runInTransactionOnRoute(route, () -> StorageShardContext.currentShardId()));
    }

    @Test
    void shouldExecuteRunnableOnKey() {
        List<String> executed = new ArrayList<>();
        executor.runOnKey("gk-1", () -> executed.add(StorageShardContext.currentShardId()));

        Assertions.assertEquals(1, executed.size());
    }

    private static DriverManagerDataSource dataSource(String dbName) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
