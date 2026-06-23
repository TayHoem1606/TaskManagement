package org.example.tay.springbkafkamongodbdemo.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tay.springbkafkamongodbdemo.application.service.TaskService;
import org.example.tay.springbkafkamongodbdemo.domain.model.Task;
import org.example.tay.springbkafkamongodbdemo.domain.model.TaskStatus;
import org.example.tay.springbkafkamongodbdemo.domain.repository.TaskRepository;
import org.example.tay.springbkafkamongodbdemo.shared.dto.TaskEventDTO;
import org.example.tay.springbkafkamongodbdemo.shared.dto.TaskResponseDTO;
import org.example.tay.springbkafkamongodbdemo.shared.exception.TaskNotFoundException;
import org.example.tay.springbkafkamongodbdemo.shared.mapper.TaskMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;

// APPLICATION LAYER — Task Service Implementation
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    // QUERY OPERATIONS — called directly by TaskController

    @Override
    public Flux<TaskResponseDTO> getAllTasks() {
        log.info("Fetching all tasks from MongoDB");
        return taskRepository.findAll()
                .map(taskMapper::toResponseDTO);
    }

    @Override
    public Mono<TaskResponseDTO> getTaskById(String id) {
        log.info("Fetching task by id: {}", id);
        return taskRepository.findById(id)
                .switchIfEmpty(Mono.error(new TaskNotFoundException(id)))
                .map(taskMapper::toResponseDTO);
    }


//        Handles TASK_CREATED event from Kafka.
//        1. Task ID must not already exist
//        2. Due date must not be in the past
//        3. Due date must not be more than 1 year ahead
//        4. Status must be PENDING (enforced here — cannot create as IN_PROGRESS or COMPLETED)

    @Override
    public Mono<Void> handleCreate(TaskEventDTO event) {
        log.info("Processing TASK_CREATED for: {}", event.getTaskId());

        // Validation: Duplicate ID check
        return taskRepository.existsById(event.getTaskId())
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Task ID already exists: {}", event.getTaskId());
                        return Mono.error(new IllegalArgumentException(
                                "Task ID already exists: " + event.getTaskId()));
                    }

                    // Validation: Due date must not be null
                    if (event.getDueDate() == null) {
                        return Mono.error(new IllegalArgumentException(
                                "Due date must not be null"));
                    }

                    // Validation: Due date must not be in the past
                    if (event.getDueDate().isBefore(LocalDate.now())) {
                        return Mono.error(new IllegalArgumentException(
                                "Due date cannot be in the past. Provided: " + event.getDueDate()));
                    }

                    // Validation: Due date must not be more than 1 year from today
                    if (event.getDueDate().isAfter(LocalDate.now().plusYears(1))) {
                        return Mono.error(new IllegalArgumentException(
                                "Due date cannot be more than 1 year from today. Provided: " + event.getDueDate()));
                    }

                    // Validation: New tasks must always start as PENDING
                    if (event.getStatus() != TaskStatus.PENDING) {
                        return Mono.error(new IllegalArgumentException(
                                "New tasks must start with status PENDING. Provided: " + event.getStatus()));
                    }

                    // All validations passed — build and save
                    Task task = Task.builder()
                            .id(event.getTaskId())
                            .title(event.getTitle())
                            .description(event.getDescription())
                            .status(TaskStatus.PENDING)   // always force PENDING on create
                            .priority(event.getPriority())
                            .dueDate(event.getDueDate())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return taskRepository.save(task)
                            .doOnSuccess(saved ->
                                    log.info("Task saved to MongoDB: {}", saved.getId()))
                            .doOnError(err ->
                                    log.error("Failed to save task {}: {}", event.getTaskId(), err.getMessage()));
                })
                .then();
    }
    
     // Handles TASK_UPDATED event from Kafka.
     //
     // TWO update modes supported:
     // A. Status-only update:
     //   If the event only provides a new status (title/description/priority/dueDate are null),
     //   only the status is updated. This allows changing status without touching other fields.
     //   Example: move PENDING -> IN_PROGRESS without changing title or description.
     //
     // B. Full update (PENDING tasks only):
     //   All fields can be changed only when the task is still PENDING.
     //   Once IN_PROGRESS, only the status can be changed (MODE A).
     //   Once COMPLETED, nothing can be changed at all.
     //
     // Status transition rules: 
     //   PENDING     -> IN_PROGRESS  allowed
     //   PENDING     -> COMPLETED    must go through IN_PROGRESS first
     //   IN_PROGRESS -> COMPLETED    allowed
     //   IN_PROGRESS -> PENDING      cannot go backwards
     //   COMPLETED   -> anything     completed tasks are fully locked
    @Override
    public Mono<Void> handleUpdate(TaskEventDTO event) {
        log.info("Processing TASK_UPDATED for: {}", event.getTaskId());

        return taskRepository.findById(event.getTaskId())
                .switchIfEmpty(Mono.error(new TaskNotFoundException(event.getTaskId())))
                .flatMap(existingTask -> {

                    // COMPLETED tasks are fully locked — nothing can change
                    if (existingTask.getStatus() == TaskStatus.COMPLETED) {
                        return Mono.error(new IllegalStateException(
                                "Task " + event.getTaskId() + " is COMPLETED and cannot be modified."));
                    }

                    // Detect status-only update
                    // If title is null/blank in the event, treat as status-only update
                    boolean isStatusOnlyUpdate = (event.getTitle() == null || event.getTitle().isBlank());

                    if (isStatusOnlyUpdate) {
                        return handleStatusOnlyUpdate(existingTask, event);
                    } else {
                        return handleFullUpdate(existingTask, event);
                    }
                })
                .doOnSuccess(updated ->
                        log.info("Task updated in MongoDB: {}", updated.getId()))
                .doOnError(err ->
                        log.error("Failed to update task {}: {}", event.getTaskId(), err.getMessage()))
                .then();
    }

    //Status-only update.
    private Mono<Task> handleStatusOnlyUpdate(Task existingTask, TaskEventDTO event) {
        if (event.getStatus() == null) {
            return Mono.error(new IllegalArgumentException(
                    "Status must be provided for status-only update."));
        }

        // Validate status transition
        Mono<Void> transitionCheck = validateStatusTransition(
                existingTask.getStatus(), event.getStatus(), event.getTaskId());

        return transitionCheck.then(Mono.defer(() -> {
            existingTask.setStatus(event.getStatus());
            existingTask.setUpdatedAt(LocalDateTime.now());
            return taskRepository.save(existingTask);
        }));
    }
    
     // Full update.
     // All fields can be updated, but only when task is PENDING.
     // Once IN_PROGRESS, full updates are blocked — use status-only update instead.
    private Mono<Task> handleFullUpdate(Task existingTask, TaskEventDTO event) {

        // Validation: IN_PROGRESS tasks can only have status changed
        if (existingTask.getStatus() == TaskStatus.IN_PROGRESS) {
            return Mono.error(new IllegalStateException(
                    "Task " + event.getTaskId() + " is IN_PROGRESS. " +
                    "Only status can be changed. " +
                    "Send a status-only update (leave title blank) to advance to COMPLETED."));
        }

        // From here, task must be PENDING — full update allowed

        // Validation: Status transition check (even for full updates)
        if (event.getStatus() != null &&
                event.getStatus() != existingTask.getStatus()) {

            if (existingTask.getStatus() == TaskStatus.PENDING &&
                    event.getStatus() == TaskStatus.COMPLETED) {
                return Mono.error(new IllegalArgumentException(
                        "Cannot jump from PENDING to COMPLETED directly. " +
                        "Task must go through IN_PROGRESS first."));
            }
        }

        // Validation : Due date must not be moved to the past
        if (event.getDueDate() != null &&
                event.getDueDate().isBefore(LocalDate.now())) {
            return Mono.error(new IllegalArgumentException(
                    "Due date cannot be set to a past date. Provided: " + event.getDueDate()));
        }

        // Validation: Due date cannot be moved earlier than the original due date
        // (cannot shrink the deadline once set)
        if (event.getDueDate() != null &&
                existingTask.getDueDate() != null &&
                event.getDueDate().isBefore(existingTask.getDueDate())) {
            return Mono.error(new IllegalArgumentException(
                    "Due date cannot be moved earlier than the original due date. " +
                    "Original: " + existingTask.getDueDate() +
                    ", Provided: " + event.getDueDate()));
        }

        // All validations passed — apply all field updates
        existingTask.setTitle(event.getTitle());
        existingTask.setDescription(event.getDescription());
        existingTask.setPriority(event.getPriority());

        if (event.getDueDate() != null) {
            existingTask.setDueDate(event.getDueDate());
        }
        if (event.getStatus() != null) {
            existingTask.setStatus(event.getStatus());
        }

        existingTask.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(existingTask);
    }

//    Validates that a status transition is allowed.
//
//    Allowed transitions:
//        PENDING     -> IN_PROGRESS
//        IN_PROGRESS -> COMPLETED
//
//    Blocked transitions:
//        PENDING     -> COMPLETED    must pass through IN_PROGRESS
//        IN_PROGRESS -> PENDING      cannot go backwards
//        COMPLETED   -> anything     final state

    private Mono<Void> validateStatusTransition(
            TaskStatus current, TaskStatus next, String taskId) {

        if (current == next) {
            // Same status — no change needed, not an error
            return Mono.empty();
        }

        boolean allowed = switch (current) {
            case PENDING -> next == TaskStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == TaskStatus.COMPLETED;
            case COMPLETED -> false; // completed is always final
        };

        if (!allowed) {
            return Mono.error(new IllegalStateException(
                    "Invalid status transition for task " + taskId +
                    ": " + current + " -> " + next + ". " +
                    getAllowedTransitionMessage(current)));
        }

        return Mono.empty();
    }

    private String getAllowedTransitionMessage(TaskStatus current) {
        return switch (current) {
            case PENDING -> "PENDING tasks can only move to IN_PROGRESS.";
            case IN_PROGRESS -> "IN_PROGRESS tasks can only move to COMPLETED.";
            case COMPLETED -> "COMPLETED tasks cannot be changed.";
        };
    }


//  Handles TASK_DELETED event from Kafka.
//  Validation:
//     - Task must exist before deletion (404 check via findById)
//     - COMPLETED tasks CAN be deleted (deletion is always allowed)

    @Override
    public Mono<Void> handleDelete(TaskEventDTO event) {
        log.info("Processing TASK_DELETED for: {}", event.getTaskId());

        return taskRepository.findById(event.getTaskId())
                .switchIfEmpty(Mono.error(new TaskNotFoundException(event.getTaskId())))
                .flatMap(task -> taskRepository.deleteById(task.getId()))
                .doOnSuccess(v ->
                        log.info("Task deleted from MongoDB: {}", event.getTaskId()))
                .doOnError(err ->
                        log.error("Failed to delete task {}: {}", event.getTaskId(), err.getMessage()));
    }
}
