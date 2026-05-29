package ru.itmo.idempotency.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.itmo.idempotency.common.kafka.KafkaJsonProducerRegistry;
import ru.itmo.idempotency.common.messaging.MessageModels;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;
import ru.itmo.idempotency.core.service.IdempotencySearchService;
import ru.itmo.idempotency.core.service.ReceiverDispatchProcessor;
import ru.itmo.idempotency.core.service.RequestDispatchProcessor;
import ru.itmo.idempotency.core.storage.StorageShardCatalog;
import ru.itmo.idempotency.core.storage.StorageShardExecutor;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.kafka.test.EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
        "sender.events.inbound",
        "sender.events.request-out",
        "receiver.events.unique",
        "receiver.events.reply"
})
class CoreShardedStorageIntegrationTest {

    private static final String INBOUND_TOPIC = "sender.events.inbound";
    private static final String REQUEST_OUT_TOPIC = "sender.events.request-out";
    private static final String RECEIVER_UNIQUE_TOPIC = "receiver.events.unique";
    private static final String H2_BASE_URL = "jdbc:h2:mem:%s;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON\\;CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE";
    private static final String SHARD_A_URL = H2_BASE_URL.formatted("core-shard-a");
    private static final String SHARD_B_URL = H2_BASE_URL.formatted("core-shard-b");

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaJsonProducerRegistry kafkaJsonProducerRegistry;

    @Autowired
    private RequestDispatchProcessor requestDispatchProcessor;

    @Autowired
    private ReceiverDispatchProcessor receiverDispatchProcessor;

    @Autowired
    private IdempotencySearchService idempotencySearchService;

    @Autowired
    private StorageShardCatalog storageShardCatalog;

    @Autowired
    private StorageShardExecutor storageShardExecutor;

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<String, JdbcTemplate> shardJdbcTemplates = new LinkedHashMap<>();

    private Consumer<String, String> requestOutConsumer;
    private Consumer<String, String> receiverUniqueConsumer;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.routes-file", () -> createRoutesFile(System.getProperty(SPRING_EMBEDDED_KAFKA_BROKERS)));
        registry.add("app.storage.shards[0].shard-id", () -> "shard-a");
        registry.add("app.storage.shards[0].url", () -> SHARD_A_URL);
        registry.add("app.storage.shards[0].username", () -> "sa");
        registry.add("app.storage.shards[0].password", () -> "");
        registry.add("app.storage.shards[1].shard-id", () -> "shard-b");
        registry.add("app.storage.shards[1].url", () -> SHARD_B_URL);
        registry.add("app.storage.shards[1].username", () -> "sa");
        registry.add("app.storage.shards[1].password", () -> "");
    }

    @BeforeEach
    void setUp() {
        requestOutConsumer = createConsumer("sharded-request-out", REQUEST_OUT_TOPIC);
        receiverUniqueConsumer = createConsumer("sharded-receiver-unique", RECEIVER_UNIQUE_TOPIC);
        initializeShardJdbcTemplates();
        initializeNonDefaultShards();
        truncateAllShards();
    }

    @AfterEach
    void tearDown() {
        requestOutConsumer.close();
        receiverUniqueConsumer.close();
        truncateAllShards();
    }

    @Test
    void shouldRouteEventsToDifferentShardsAndProcessThem() throws Exception {
        Map<String, String> shardToUid = locateDifferentShardAssignments();
        Assertions.assertEquals(2, shardToUid.size());

        List<Map.Entry<String, String>> assignments = new ArrayList<>(shardToUid.entrySet());
        String shardA = assignments.get(0).getKey();
        String uidA = assignments.get(0).getValue();
        String shardB = assignments.get(1).getKey();
        String uidB = assignments.get(1).getValue();

        kafkaJsonProducerRegistry.send(
                embeddedKafkaBroker.getBrokersAsString(),
                INBOUND_TOPIC,
                uidA,
                new MessageModels.MessageEnvelope(Map.of("uid", uidA), objectMapper.readTree("{\"amount\":100}"))
        );
        kafkaJsonProducerRegistry.send(
                embeddedKafkaBroker.getBrokersAsString(),
                INBOUND_TOPIC,
                uidB,
                new MessageModels.MessageEnvelope(Map.of("uid", uidB), objectMapper.readTree("{\"amount\":200}"))
        );

        String globalKeyA = globalKey(uidA);
        String globalKeyB = globalKey(uidB);
        waitForCondition(() -> idempotencySearchService.findByGlobalKey(globalKeyA).isPresent());
        waitForCondition(() -> idempotencySearchService.findByGlobalKey(globalKeyB).isPresent());

        Assertions.assertEquals(1L, countRows(shardA, "idempotency"));
        Assertions.assertEquals(1L, countRows(shardB, "idempotency"));
        Assertions.assertEquals(1L, countRows(shardA, "kafka_event_outbox"));
        Assertions.assertEquals(1L, countRows(shardB, "kafka_event_outbox"));

        Assertions.assertEquals(2, requestDispatchProcessor.processBatch(10));
        Assertions.assertEquals(2, receiverDispatchProcessor.processBatch(10));

        List<MessageModels.MessageEnvelope> technicalResponses = consumeMessages(requestOutConsumer, REQUEST_OUT_TOPIC, 2, Duration.ofSeconds(10));
        Assertions.assertEquals(2, technicalResponses.size());

        List<MessageModels.MessageEnvelope> receiverMessages = consumeMessages(receiverUniqueConsumer, RECEIVER_UNIQUE_TOPIC, 2, Duration.ofSeconds(10));
        Assertions.assertEquals(2, receiverMessages.size());

        Assertions.assertEquals(IdempotencyStatus.WAITING_ASYNC_RESPONSE,
                idempotencySearchService.findByGlobalKey(globalKeyA).orElseThrow().getStatus());
        Assertions.assertEquals(IdempotencyStatus.WAITING_ASYNC_RESPONSE,
                idempotencySearchService.findByGlobalKey(globalKeyB).orElseThrow().getStatus());
    }

    private void initializeShardJdbcTemplates() {
        shardJdbcTemplates.clear();
        for (StorageShardCatalog.ShardDataSource shard : storageShardCatalog.shards()) {
            shardJdbcTemplates.put(shard.shardId(), new JdbcTemplate((DataSource) shard.dataSource()));
        }
    }

    private void initializeNonDefaultShards() {
        List<StorageShardCatalog.ShardDataSource> shards = storageShardCatalog.shards();
        for (int index = 1; index < shards.size(); index++) {
            JdbcTemplate jdbcTemplate = shardJdbcTemplates.get(shards.get(index).shardId());
            for (String statement : SHARD_SCHEMA_SQL) {
                jdbcTemplate.execute(statement);
            }
        }
    }

    private void truncateAllShards() {
        for (JdbcTemplate jdbcTemplate : shardJdbcTemplates.values()) {
            jdbcTemplate.execute("DELETE FROM kafka_event_outbox");
            jdbcTemplate.execute("DELETE FROM event_audit");
            jdbcTemplate.execute("DELETE FROM idempotency");
        }
    }

    private Map<String, String> locateDifferentShardAssignments() {
        Map<String, String> assignments = new LinkedHashMap<>();
        for (int index = 1; index <= 1_000 && assignments.size() < 2; index++) {
            String uid = "shard-uid-" + index;
            assignments.putIfAbsent(storageShardExecutor.shardIdForKey(globalKey(uid)), uid);
        }
        return assignments;
    }

    private long countRows(String shardId, String tableName) {
        return shardJdbcTemplates.get(shardId).queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
    }

    private String globalKey(String uid) {
        return "sender-service:system1-to-system2:" + uid;
    }

    private Consumer<String, String> createConsumer(String groupId, String topic) {
        Map<String, Object> properties = KafkaTestUtils.consumerProps(groupId, "false", embeddedKafkaBroker);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                new StringDeserializer()
        );
        Consumer<String, String> consumer = consumerFactory.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, true, topic);
        return consumer;
    }

    private List<MessageModels.MessageEnvelope> consumeMessages(Consumer<String, String> consumer,
                                                                 String topic,
                                                                 int expectedCount,
                                                                 Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        List<MessageModels.MessageEnvelope> result = new ArrayList<>();
        while (System.currentTimeMillis() < deadline && result.size() < expectedCount) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records.records(topic)) {
                result.add(objectMapper.readValue(record.value(), MessageModels.MessageEnvelope.class));
            }
        }
        if (result.size() < expectedCount) {
            Assertions.fail("Expected " + expectedCount + " messages but got " + result.size());
        }
        return result;
    }

    private void waitForCondition(Check check) throws Exception {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (check.ok()) {
                return;
            }
            Thread.sleep(100);
        }
        Assertions.fail("Condition was not met within timeout");
    }

    private static String createRoutesFile(String bootstrapServers) {
        try {
            Path file = Files.createTempFile("routes-sharded-test-", ".yaml");
            Files.writeString(file, """
                    service:
                      name: sender-service

                    kafka:
                      routes:
                        system1-to-system2:
                          sender:
                            producer:
                              host: %s
                              topic: sender.events.inbound
                            consumer:
                              host: %s
                              topic: sender.events.request-out
                              group: sender-test-replies
                          receiver:
                            producer:
                              host: %s
                              topic: receiver.events.unique
                            consumer:
                              host: %s
                              topic: receiver.events.reply
                              group: core-test-replies
                          idempotency:
                            enabled: true
                    """.formatted(bootstrapServers, bootstrapServers, bootstrapServers, bootstrapServers));
            return file.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @FunctionalInterface
    private interface Check {
        boolean ok() throws Exception;
    }

    private static final List<String> SHARD_SCHEMA_SQL = List.of(
            """
            CREATE TABLE IF NOT EXISTS idempotency (
                global_key VARCHAR(512) PRIMARY KEY,
                source_uid VARCHAR(255) NOT NULL,
                service_name VARCHAR(255) NOT NULL,
                integration_name VARCHAR(255) NOT NULL,
                yaml_snapshot JSONB NOT NULL,
                headers JSONB NOT NULL,
                payload JSONB NOT NULL,
                status VARCHAR(24) NOT NULL,
                status_description VARCHAR(255),
                retry_count INTEGER NOT NULL DEFAULT 0,
                next_attempt_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                owner_id VARCHAR(128),
                lease_until TIMESTAMPTZ,
                last_claim_date TIMESTAMPTZ,
                create_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                update_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_idempotency_status_create_date ON idempotency (status, create_date)",
            "CREATE INDEX IF NOT EXISTS idx_idempotency_status_update_date ON idempotency (status, update_date)",
            "CREATE INDEX IF NOT EXISTS idx_idempotency_status_next_attempt_date ON idempotency (status, next_attempt_date, create_date)",
            "CREATE INDEX IF NOT EXISTS idx_idempotency_status_claim_window ON idempotency (status, next_attempt_date, lease_until, create_date)",
            "CREATE INDEX IF NOT EXISTS idx_idempotency_expired_leases ON idempotency (lease_until)",
            """
            CREATE TABLE IF NOT EXISTS kafka_event_outbox (
                id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                global_key VARCHAR(512),
                service_name VARCHAR(255),
                integration_name VARCHAR(255),
                yaml_snapshot JSONB NOT NULL,
                status VARCHAR(8) NOT NULL,
                status_description VARCHAR(255),
                result VARCHAR(8) NOT NULL,
                result_description VARCHAR(255),
                retry_count INTEGER NOT NULL DEFAULT 0,
                next_attempt_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                owner_id VARCHAR(128),
                lease_until TIMESTAMPTZ,
                last_claim_date TIMESTAMPTZ,
                create_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                update_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_kafka_event_outbox_status_create_date ON kafka_event_outbox (status, create_date)",
            "CREATE INDEX IF NOT EXISTS idx_kafka_event_outbox_status_next_attempt_date ON kafka_event_outbox (status, next_attempt_date, create_date)",
            "CREATE INDEX IF NOT EXISTS idx_kafka_event_outbox_claim_window ON kafka_event_outbox (status, next_attempt_date, lease_until, create_date)",
            "CREATE INDEX IF NOT EXISTS idx_kafka_event_outbox_expired_leases ON kafka_event_outbox (lease_until)",
            """
            CREATE TABLE IF NOT EXISTS event_audit (
                id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                global_key VARCHAR(512),
                service_name VARCHAR(255),
                integration_name VARCHAR(255),
                reason VARCHAR(255) NOT NULL,
                headers JSONB,
                payload JSONB,
                yaml_snapshot JSONB,
                create_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_event_audit_create_date ON event_audit (create_date)"
    );
}
