package br.com.loomi.orders.config;

import br.com.loomi.orders.domain.event.OrderEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for the order processing service.
 * Configures producers, consumers, and topics for order events.
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.order-events-topic:order-events}")
    private String orderEventsTopic;

    /**
     * Creates the Kafka topic for order events.
     *
     * @return the configured Kafka topic
     */
    @Bean
    public NewTopic orderEventsTopic() {
        return new NewTopic(orderEventsTopic, 3, (short) 1);
    }

    /**
     * Creates the producer factory for order events.
     *
     * @return the configured producer factory
     */
    @Bean
    public ProducerFactory<String, OrderEvent> orderEventProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Creates the Kafka template for publishing order events.
     *
     * @return the configured Kafka template
     */
    @Bean
    public KafkaTemplate<String, OrderEvent> orderEventKafkaTemplate() {
        return new KafkaTemplate<>(orderEventProducerFactory());
    }

    /**
     * Creates the consumer factory for order events.
     *
     * @return the configured consumer factory
     */
    @Bean
    public ConsumerFactory<String, OrderEvent> orderEventConsumerFactory() {
        JsonDeserializer<OrderEvent> deserializer = new JsonDeserializer<>(OrderEvent.class);
        deserializer.addTrustedPackages("*");

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "order-processing-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    /**
     * Creates error handler with retry logic for consumer.
     * Retries 3 times with 2 second interval before giving up.
     *
     * @return the configured error handler
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(
                (record, exception) -> {
                    log.error("Failed to process message after retries. Topic: {}, Key: {}, Error: {}",
                            record.topic(), record.key(), exception.getMessage());
                },
                new FixedBackOff(2000L, 3L)
        );
    }

    /**
     * Creates the Kafka listener container factory for order events.
     *
     * @return the configured listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> orderEventListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderEventConsumerFactory());
        factory.setCommonErrorHandler(kafkaErrorHandler());
        return factory;
    }
}
