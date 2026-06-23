package org.example.tay.springbkafkamongodbdemo.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskEventDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {


    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${kafka.topic.name}")
    private String taskEventsTopic;

    // Topics
    @Bean
    public NewTopic taskEventsTopic() {
        return TopicBuilder.name(taskEventsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Dead Letter Topic: task-events.DLT
     *
     * Spring Kafka's DeadLetterPublishingRecoverer automatically appends ".DLT"
     * to the original topic name. We pre-create it here so it exists before
     * any messages need to go there.
     *
     * Without this bean, Kafka will auto-create the DLT on first failure,
     * which is fine for development but risky for production.
     */
    @Bean
    public NewTopic taskEventsDltTopic() {
        return TopicBuilder.name(taskEventsTopic + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    // ── Producer ─────────────────────────────────────────────────────
    @Bean
    public ProducerFactory<String, TaskEventDTO> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        props.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        // Producer resilience settings
        // Retry up to 10 times on transient send failures
        props.put(ProducerConfig.RETRIES_CONFIG, 10);
        // Wait 2 seconds between retries (prevents hammering a down broker)
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 2000);
        // Give up on metadata fetch after 10 seconds (fail fast when Kafka is down)
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10_000);
        // Total time budget for one send including all retries = 60 seconds
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 60_000);
        // Per-request broker timeout = 10 seconds
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10_000);

        // ── Exactly-once on producer side
        // Prevents duplicate messages when a send is retried after timeout
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        // Required by idempotence: all in-sync replicas must ack
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, TaskEventDTO> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ── Consumer ─────────────────────────────────────────────────────
    @Bean
    public ConsumerFactory<String, TaskEventDTO> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,  bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,           groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,  "earliest");

        // Manual offset commit: only commit after successful processing
        // AUTO_COMMIT = true (default) commits offsets on a timer regardless
        // of processing success. This means a failed message is "consumed"
        // even if the handler threw an exception.
        // Setting to false + ENABLE_AUTO_COMMIT=false gives the error handler
        // control over when to commit (after DLT write or successful processing).
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JacksonJsonDeserializer<TaskEventDTO> deserializer =
                new JacksonJsonDeserializer<>(TaskEventDTO.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    /**
     * Listener container factory with Dead Letter Topic and retry backoff.
     *
     * DefaultErrorHandler replaces the old SeekToCurrentErrorHandler.
     *
     * FixedBackOff(2000L, 3):
     *   - intervalMillis = 2000 → wait 2 seconds between retries
     *   - maxAttempts    = 3    → retry 3 times before giving up
     *   After 3 failures: DeadLetterPublishingRecoverer moves message to DLT.
     *
     * DeadLetterPublishingRecoverer(kafkaTemplate()):
     *   Writes the failed message to topic "task-events.DLT".
     *   The DLT message includes:
     *     - Original message bytes
     *     - Exception class name header
     *     - Exception message header
     *     - Original topic/partition/offset headers
     *
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TaskEventDTO>
    kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TaskEventDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Dead Letter Topic recoverer: after retries exhausted, write to DLT
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate());

        // Retry 3 times with 2-second intervals, then DLT
        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 3));

        // Deserialization errors are unrecoverable — skip straight to DLT
        // (no point retrying a broken message 3 times)
        errorHandler.addNotRetryableExceptions(
                org.apache.kafka.common.errors.SerializationException.class
        );

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}