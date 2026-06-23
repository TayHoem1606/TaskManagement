package org.example.tay.taskmanagmentkafkareact.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskEventDTO;
import org.example.tay.taskmanagmentkafkareact.shared.exception.KafkaPublishException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;


//  INFRASTRUCTURE LAYER — Kafka Producer
//
//  Responsible only for sending messages to the Kafka topic.
//  This class is only called by KafkaEventHandler — NOT by TaskService.
//  This keeps Kafka concerns fully inside the infrastructure layer.

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, TaskEventDTO> kafkaTemplate;

    @Value("${kafka.topic.name}")
    private String taskEventsTopic;

    public void publishEvent(TaskEventDTO event) {
        log.info("Attempting to publish Kafka event: {} for task: {}",
                event.getEventType(), event.getTaskId());

        CompletableFuture<SendResult<String, TaskEventDTO>> future =
                kafkaTemplate.send(taskEventsTopic, event.getTaskId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                // ── SUCCESS: message durably written to Kafka broker ──────────
                log.info(
                        "Kafka event PUBLISHED successfully: eventType={}, taskId={}, " +
                                "topic={}, partition={}, offset={}",
                        event.getEventType(),
                        event.getTaskId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                );
            } else {
                // ── FAILURE: Kafka rejected the message after all producer retries ──
                // This fires after the configured RETRIES_CONFIG attempts are exhausted.
                // With RETRIES=10 and backoff=2s, this happens ~20 seconds after the error.
                log.error(
                        "Kafka event FAILED to publish: eventType={}, taskId={}, " +
                                "topic={}, error={}",
                        event.getEventType(),
                        event.getTaskId(),
                        taskEventsTopic,
                        ex.getMessage(),
                        ex  // full stack trace
                );

                throw new KafkaPublishException(
                        "Failed to publish " + event.getEventType() +
                                " for task " + event.getTaskId() + ": " + ex.getMessage(),
                        ex
                );
            }
        });
    }

}
