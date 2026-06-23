package org.example.tay.taskmanagmentkafkareact.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tay.taskmanagmentkafkareact.application.service.TaskService;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskEventDTO;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;


//  INFRASTRUCTURE LAYER — Kafka Consumer
//
//  Thin consumer — receives events from Kafka topic and
//  delegates ALL business logic to TaskService (command methods).
//
//  Consumer only decides: "Which method to call based on event type?"
//  TaskServiceImpl decides: "How to validate and persist the task."
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final TaskService taskService;

    @KafkaListener(
            topics = "${kafka.topic.name}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeEvent(
            TaskEventDTO event,
            // Inject Kafka headers so we know exactly which message caused errors
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET)             long offset
    ) {
        // ── Guard: null event = deserialization failure ────────────────
        if (event == null) {
            log.warn(
                    "CONSUMER WARNING — received null event (deserialization failure). " +
                            "Message at partition={}, offset={} could not be parsed into TaskEventDTO. " +
                            "Check the raw message with: " +
                            "docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh " +
                            "--topic task-events --bootstrap-server localhost:9092 " +
                            "--partition {} --offset {} --max-messages 1",
                    partition, offset, partition, offset
            );
            // Return cleanly — the DefaultErrorHandler in KafkaConfig will
            // route this to the DLT after retries.
            return;
        }

        log.info("Received Kafka Event: eventType={}, taskId={}, partition={}, offset={}",
                event.getEventType(), event.getTaskId(), partition, offset);

        switch (event.getEventType()) {
            case TASK_CREATED -> taskService.handleCreate(event).block();
            case TASK_UPDATED -> taskService.handleUpdate(event).block();
            case TASK_DELETED -> taskService.handleDelete(event).block();
            default -> log.warn("Unknown event type received: {}", event.getEventType());
        }
    }

}
