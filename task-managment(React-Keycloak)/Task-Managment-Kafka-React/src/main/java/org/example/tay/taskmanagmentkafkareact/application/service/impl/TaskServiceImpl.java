package org.example.tay.taskmanagmentkafkareact.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tay.taskmanagmentkafkareact.application.service.TaskService;
import org.example.tay.taskmanagmentkafkareact.domain.model.Task;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskStatus;
import org.example.tay.taskmanagmentkafkareact.domain.repository.TaskRepository;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskEventDTO;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskFilterDTO;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskRequestDTO;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskResponseDTO;
import org.example.tay.taskmanagmentkafkareact.shared.exception.ConflictException;
import org.example.tay.taskmanagmentkafkareact.shared.exception.TaskNotFoundException;
import org.example.tay.taskmanagmentkafkareact.shared.mapper.TaskMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

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

    @Override
    public Flux<TaskResponseDTO> getFilteredTasks(TaskFilterDTO filter) {
        log.info("Delegating filter query to TaskRepository: {}", filter);
        // Call the repository method directly
        return taskRepository.findByFilters(filter)
                .map(taskMapper::toResponseDTO);
    }


    @Override
    public Mono<Void> handleCreate(TaskEventDTO event) {
        log.info("Processing TASK_CREATED for: {} by user: {}", event.getTaskId(), event.getCreatedBy());

        return taskRepository.existsById(event.getTaskId())
                .flatMap(exists -> {
                    // Validation: Duplicate ID Check
                    if (exists) {
                        return Mono.error(new ConflictException("Task ID already exists: " + event.getTaskId()));
                    }

                    //Validation: Due Date Bounds (Past and 1-Year Limit)
                    LocalDate now = LocalDate.now();
                    if (event.getDueDate().isAfter(now.plusYears(1))) {
                        return Mono.error(new ConflictException("Due date cannot be more than 1 year away"));
                    }

                    //Audit Metadata: Ensure creator information is present
                    if (event.getCreatedBy() == null || event.getCreatedBy().isBlank()) {
                        return Mono.error(new ConflictException("Creator username (createdBy) is missing in event"));
                    }

                    LocalDateTime timestamp = LocalDateTime.now();

                    // 5. Build and Persist
                    Task task = Task.builder()
                            .id(event.getTaskId())
                            .title(event.getTitle())
                            .description(event.getDescription())
                            .status(TaskStatus.PENDING)
                            .priority(event.getPriority())
                            .dueDate(event.getDueDate())
                            .createdAt(timestamp)
                            .updatedAt(timestamp)
                            .createdBy(event.getCreatedBy())
                            .updatedBy(event.getCreatedBy())
                            .build();

                    return taskRepository.save(task)
                            .doOnSuccess(saved -> log.info("Task {} successfully created by {}", saved.getId(), saved.getCreatedBy()))
                            .doOnError(err -> log.error("Failed to save task {}: {}", event.getTaskId(), err.getMessage()));
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
         return taskRepository.findById(event.getTaskId())
                 .switchIfEmpty(Mono.error(new TaskNotFoundException(event.getTaskId())))
                 .flatMap(existingTask -> {
                     // Validation: COMPLETED tasks are locked
                     if (existingTask.getStatus() == TaskStatus.COMPLETED) {
                         return Mono.error(new ConflictException("COMPLETED tasks cannot be modified."));
                     }

                     boolean isStatusOnly = (event.getTitle() == null || event.getTitle().isBlank());
                     return isStatusOnly ?
                             handleStatusOnlyUpdate(existingTask, event) :
                             handleFullUpdate(existingTask, event);
                 })
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
            existingTask.setUpdatedBy(event.getUpdatedBy());
            return taskRepository.save(existingTask);
        }));
    }
    
     // Full update.
     // All fields can be updated, but only when task is PENDING.
     // Once IN_PROGRESS, full updates are blocked — use status-only update instead.
    private Mono<Task> handleFullUpdate(Task existingTask, TaskEventDTO event) {

        // Validation: IN_PROGRESS tasks can only have status changed
        if (existingTask.getStatus() == TaskStatus.IN_PROGRESS) {
            return Mono.error(new ConflictException(
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
                return Mono.error(new ConflictException(
                        "Cannot jump from PENDING to COMPLETED directly. " +
                        "Task must go through IN_PROGRESS first."));
            }
        }

        // Validation: Due date cannot be moved earlier than the original due date
        // (cannot shrink the deadline once set)
        if (event.getDueDate() != null &&
                existingTask.getDueDate() != null &&
                event.getDueDate().isBefore(existingTask.getDueDate())) {
            return Mono.error(new ConflictException(
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
        existingTask.setUpdatedBy(event.getUpdatedBy());
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
        return taskRepository.findById(event.getTaskId())
                .flatMap(task -> taskRepository.deleteById(task.getId()))
                .switchIfEmpty(Mono.fromRunnable(() ->
                        log.warn("Delete event for non-existent task: {} — treating as success",
                                event.getTaskId())))
                .doOnSuccess(v -> log.info("Task deleted: {}", event.getTaskId()))
                .then();
    }

    //Helper Method
    //get current username from ReactiveSecurityContext
    private Mono<String> getCurrentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .defaultIfEmpty("anonymous");
    }

    //checks if current user has ADMIN role
    private Mono<Boolean> isAdmin() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .flatMap(auth -> Flux.fromIterable(auth.getAuthorities())
                        .map(GrantedAuthority::getAuthority)
                        .filter(role -> role.equals("ROLE_ADMIN"))
                        .hasElements());
    }

    @Override
    public Mono<TaskResponseDTO> validateOwnership(TaskResponseDTO existingTask) {
        return isAdmin().flatMap(admin ->
                getCurrentUsername().flatMap(username -> {
                    // ADMIN can do anything; USER must be the creator
                    if (!admin && !existingTask.getCreatedBy().equals(username)) {
                        log.warn("Access denied for user {} on task {}", username, existingTask.getId());
                        return Mono.error(new AccessDeniedException(
                                "You do not have permission to modify task: " + existingTask.getId()));
                    }
                    return Mono.just(existingTask);
                })
        );
    }

    @Override
    public Mono<Void> validateUpdate(String taskId, TaskRequestDTO request) {
        return taskRepository.findById(taskId)
                .switchIfEmpty(Mono.error(new TaskNotFoundException(taskId)))
                .flatMap(existingTask -> {
                    if (existingTask.getStatus() == TaskStatus.COMPLETED) {
                        return Mono.error(new ConflictException("COMPLETED tasks are locked."));
                    }

                    // 2. 状态转换校验 (重用 handleUpdate 中的逻辑)
                    if (request.getStatus() != null && request.getStatus() != existingTask.getStatus()) {
                        return validateStatusTransition(existingTask.getStatus(), request.getStatus(), taskId);
                    }

                    return Mono.empty();
                });
    }

    @Override
    public Mono<String> generateTaskId() {
        String newId = "TASK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return taskRepository.existsById(newId)
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Task ID collision detected for {}, regenerating...", newId);
                        return generateTaskId();
                    }
                    return Mono.just(newId);
                });
    }
}
