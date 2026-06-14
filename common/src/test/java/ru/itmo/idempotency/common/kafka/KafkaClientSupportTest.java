package ru.itmo.idempotency.common.kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class KafkaClientSupportTest {

    @Test
    void shouldBuildProducerProperties() {
        var properties = KafkaClientSupport.producerProperties("localhost:9092");

        Assertions.assertEquals("localhost:9092", properties.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        Assertions.assertEquals(StringSerializer.class, properties.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
        Assertions.assertEquals(StringSerializer.class, properties.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
        Assertions.assertEquals("all", properties.get(ProducerConfig.ACKS_CONFIG));
        Assertions.assertEquals(true, properties.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG));
        Assertions.assertEquals(1, properties.get(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION));
        Assertions.assertEquals(10, properties.get(ProducerConfig.RETRIES_CONFIG));
    }

    @Test
    void shouldBuildConsumerProperties() {
        var properties = KafkaClientSupport.consumerProperties("localhost:9092", "test-group", "test-client");

        Assertions.assertEquals("localhost:9092", properties.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        Assertions.assertEquals(StringDeserializer.class, properties.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        Assertions.assertEquals(StringDeserializer.class, properties.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
        Assertions.assertEquals("test-group", properties.get(ConsumerConfig.GROUP_ID_CONFIG));
        Assertions.assertTrue(properties.get(ConsumerConfig.CLIENT_ID_CONFIG).toString().startsWith("test-client-"));
        Assertions.assertEquals("earliest", properties.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
        Assertions.assertEquals(false, properties.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
    }

    @Test
    void shouldBuildAdminProperties() {
        var properties = KafkaClientSupport.adminProperties("localhost:9092");

        Assertions.assertEquals("localhost:9092", properties.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG));
    }
}
