package ru.itmo.idempotency.core.config;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import ru.itmo.idempotency.core.storage.StorageShardCatalog;

import javax.sql.DataSource;
import java.util.List;

@Configuration
public class StorageDataSourceConfiguration {

    @Bean
    public StorageShardCatalog storageShardCatalog(CoreProperties coreProperties,
                                                   DataSourceProperties dataSourceProperties) {
        List<StorageShardCatalog.ShardDataSource> shards = resolveShardConfigurations(coreProperties, dataSourceProperties)
                .stream()
                .map(this::createShardDataSource)
                .toList();
        return new StorageShardCatalog(shards);
    }

    @Bean
    @Primary
    public DataSource dataSource(StorageShardCatalog storageShardCatalog) {
        return new LazyConnectionDataSourceProxy(storageShardCatalog.routingDataSource());
    }

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(StorageShardCatalog storageShardCatalog) {
        return flyway -> {
            for (StorageShardCatalog.ShardDataSource shard : storageShardCatalog.shards()) {
                Flyway.configure()
                        .configuration(flyway.getConfiguration())
                        .dataSource(shard.dataSource())
                        .load()
                        .migrate();
            }
        };
    }

    private List<ShardConfiguration> resolveShardConfigurations(CoreProperties coreProperties,
                                                                DataSourceProperties dataSourceProperties) {
        if (coreProperties.getStorage().getShards().isEmpty()) {
            return List.of(new ShardConfiguration(
                    "default",
                    dataSourceProperties.getUrl(),
                    dataSourceProperties.getUsername(),
                    dataSourceProperties.getPassword(),
                    dataSourceProperties.getDriverClassName(),
                    10
            ));
        }

        return coreProperties.getStorage().getShards().stream()
                .map(shard -> new ShardConfiguration(
                        shard.getShardId(),
                        shard.getUrl(),
                        shard.getUsername(),
                        shard.getPassword(),
                        shard.getDriverClassName(),
                        shard.getMaximumPoolSize()
                ))
                .toList();
    }

    private StorageShardCatalog.ShardDataSource createShardDataSource(ShardConfiguration configuration) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName("core-" + configuration.shardId());
        dataSource.setJdbcUrl(configuration.url());
        dataSource.setUsername(configuration.username());
        dataSource.setPassword(configuration.password());
        if (configuration.driverClassName() != null && !configuration.driverClassName().isBlank()) {
            dataSource.setDriverClassName(configuration.driverClassName());
        }
        dataSource.setMaximumPoolSize(configuration.maximumPoolSize());
        dataSource.setMinimumIdle(Math.min(1, configuration.maximumPoolSize()));
        return new StorageShardCatalog.ShardDataSource(configuration.shardId(), dataSource);
    }

    private record ShardConfiguration(String shardId,
                                      String url,
                                      String username,
                                      String password,
                                      String driverClassName,
                                      int maximumPoolSize) {
    }
}
