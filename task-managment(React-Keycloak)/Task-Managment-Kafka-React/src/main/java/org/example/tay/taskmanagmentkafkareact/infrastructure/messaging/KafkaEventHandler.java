package org.example.tay.taskmanagmentkafkareact.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tay.taskmanagmentkafkareact.domain.event.TaskCreatedEvent;
import org.example.tay.taskmanagmentkafkareact.domain.event.TaskDeletedEvent;
import org.example.tay.taskmanagmentkafkareact.domain.event.TaskUpdatedEvent;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskEventType;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskEventDTO;
import org.example.tay.taskmanagmentkafkareact.shared.exception.KafkaPublishException;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


//  INFRASTRUCTURE LAYER — Kafka Event Handler
//
//  Listens to domain events published by TaskController via
//  ApplicationEventPublisher, then forwards them to Kafka.
//
//    TaskController
//        │ publishes TaskCreatedEvent (via ApplicationEventPublisher)
//    KafkaEventHandler
//        │ converts domain event → TaskEventDTO
//        │ calls KafkaProducerService
//   Kafka Topic: task-events
//   KafkaConsumerService (saves to MongoDB)

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventHandler {

    private final KafkaProducerService kafkaProducerService;

    @EventListener
    public void handleTaskCreated(TaskCreatedEvent event) {
        log.info("Handling TaskCreatedEvent for task: {} by user: {}",
                event.getTaskId(), event.getCreatedBy());
        try {
            kafkaProducerService.publishEvent(
                    TaskEventDTO.builder()
                            .eventType(TaskEventType.TASK_CREATED)
                            .taskId(event.getTaskId())
                            .title(event.getTitle())
                            .description(event.getDescription())
                            .status(event.getStatus())
                            .priority(event.getPriority())
                            .dueDate(event.getDueDate())
                            .createdBy(event.getCreatedBy())
                            .timestamp(LocalDateTime.now())
                            .build()
            );
        } catch (KafkaPublishException e) {
            // This catches the exception thrown from the CompletableFuture callback.
            // Log at ERROR level with full context so it is visible in docker logs.
            log.error("KAFKA HANDLER ERROR — Failed to publish TASK_CREATED " +
                    "for task: {}. The task will NOT be saved to MongoDB. " +
                    "Error: {}", event.getTaskId(), e.getMessage(), e);
            // In production: write to outbox table or alert here
        }
    }

    @EventListener
    public void handleTaskUpdated(TaskUpdatedEvent event) {
        log.info("Handling TaskUpdatedEvent for task: {} by user: {}",
                event.getTaskId(), event.getUpdatedBy());
        try {
            kafkaProducerService.publishEvent(
                    TaskEventDTO.builder()
                            .eventType(TaskEventType.TASK_UPDATED)
                            .taskId(event.getTaskId())
                            .title(event.getTitle())
                            .description(event.getDescription())
                            .status(event.getStatus())
                            .priority(event.getPriority())
                            .dueDate(event.getDueDate())
                            .updatedBy(event.getUpdatedBy())
                            .timestamp(LocalDateTime.now())
                            .build()
            );
        } catch (KafkaPublishException e) {
            log.error("KAFKA HANDLER ERROR — Failed to publish TASK_UPDATED " +
                    "for task: {}. Error: {}", event.getTaskId(), e.getMessage(), e);
        }
    }

    @EventListener
    public void handleTaskDeleted(TaskDeletedEvent event) {
        log.info("Handling TaskDeletedEvent for task: {} by user: {}",
                event.getTaskId(), event.getDeletedBy());
        try {
            kafkaProducerService.publishEvent(
                    TaskEventDTO.builder()
                            .eventType(TaskEventType.TASK_DELETED)
                            .taskId(event.getTaskId())
                            .title(event.getTitle())
                            .updatedBy(event.getDeletedBy())
                            .timestamp(LocalDateTime.now())
                            .build()
            );
        } catch (KafkaPublishException e) {
            log.error("KAFKA HANDLER ERROR — Failed to publish TASK_DELETED " +
                    "for task: {}. Error: {}", event.getTaskId(), e.getMessage(), e);
        }
    }
}
