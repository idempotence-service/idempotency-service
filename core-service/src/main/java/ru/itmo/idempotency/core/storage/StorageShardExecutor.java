package ru.itmo.idempotency.core.storage;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ru.itmo.idempotency.common.config.RouteModels;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

@Component
public class StorageShardExecutor {

    private final StorageShardCatalog shardCatalog;
    private final TransactionTemplate readWriteTransactionTemplate;
    private final TransactionTemplate readOnlyTransactionTemplate;

    public StorageShardExecutor(StorageShardCatalog shardCatalog,
                                PlatformTransactionManager transactionManager) {
        this.shardCatalog = shardCatalog;
        this.readWriteTransactionTemplate = new TransactionTemplate(transactionManager);
        this.readOnlyTransactionTemplate = new TransactionTemplate(transactionManager);
        this.readWriteTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.readOnlyTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.readOnlyTransactionTemplate.setReadOnly(true);
    }

    public String shardIdForKey(String shardKey) {
        return shardCatalog.shardIdForKey(shardKey);
    }

    public List<String> shardIdsInScanOrder() {
        return shardCatalog.shardIdsInScanOrder();
    }

    public <T> T runOnKey(String shardKey, Supplier<T> action) {
        return runOnShard(shardCatalog.shardIdForKey(shardKey), action);
    }

    public void runOnKey(String shardKey, Runnable action) {
        runOnKey(shardKey, () -> {
            action.run();
            return null;
        });
    }

    public <T> T runOnRoute(RouteModels.RouteSnapshot route, Supplier<T> action) {
        return runOnKey(routeKey(route), action);
    }

    public void runOnRoute(RouteModels.RouteSnapshot route, Runnable action) {
        runOnRoute(route, () -> {
            action.run();
            return null;
        });
    }

    public <T> T runReadOnlyOnKey(String shardKey, Supplier<T> action) {
        return readOnlyTransactionTemplate.execute(status -> runOnKey(shardKey, action));
    }

    public <T> T runReadOnlyOnShard(String shardId, Supplier<T> action) {
        return readOnlyTransactionTemplate.execute(status -> runOnShard(shardId, action));
    }

    public <T> Optional<T> firstPresentInTransaction(Function<String, Optional<T>> action) {
        for (String shardId : shardCatalog.shardIdsInScanOrder()) {
            Optional<T> result = readWriteTransactionTemplate.execute(status -> runOnShard(shardId, () -> action.apply(shardId)));
            if (result != null && result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    public boolean firstTrueInTransaction(Function<String, Boolean> action) {
        for (String shardId : shardCatalog.shardIdsInScanOrder()) {
            Boolean result = readWriteTransactionTemplate.execute(status -> runOnShard(shardId, () -> action.apply(shardId)));
            if (Boolean.TRUE.equals(result)) {
                return true;
            }
        }
        return false;
    }

    public <T> List<T> collectFromAllReadOnly(Function<String, List<T>> action) {
        List<T> result = new ArrayList<>();
        for (String shardId : shardCatalog.shardIdsInScanOrder()) {
            List<T> items = readOnlyTransactionTemplate.execute(status -> runOnShard(shardId, () -> action.apply(shardId)));
            if (items != null && !items.isEmpty()) {
                result.addAll(items);
            }
        }
        return result;
    }

    public long sumLongReadOnly(ToLongFunction<String> action) {
        long result = 0;
        for (String shardId : shardCatalog.shardIdsInScanOrder()) {
            Long partial = readOnlyTransactionTemplate.execute(status -> runOnShard(shardId, () -> action.applyAsLong(shardId)));
            result += partial != null ? partial : 0;
        }
        return result;
    }

    public <T> T runInTransactionOnKey(String shardKey, Supplier<T> action) {
        return readWriteTransactionTemplate.execute(status -> runOnKey(shardKey, action));
    }

    public <T> T runInTransactionOnShard(String shardId, Supplier<T> action) {
        return readWriteTransactionTemplate.execute(status -> runOnShard(shardId, action));
    }

    public <T> T runInTransactionOnRoute(RouteModels.RouteSnapshot route, Supplier<T> action) {
        return readWriteTransactionTemplate.execute(status -> runOnRoute(route, action));
    }

    public String routeKey(RouteModels.RouteSnapshot route) {
        if (route == null) {
            return null;
        }
        return route.service() + ":" + route.integration();
    }

    public <T> T runOnShard(String shardId, Supplier<T> action) {
        try (StorageShardContext.Scope ignored = StorageShardContext.open(shardId)) {
            return action.get();
        }
    }
}
