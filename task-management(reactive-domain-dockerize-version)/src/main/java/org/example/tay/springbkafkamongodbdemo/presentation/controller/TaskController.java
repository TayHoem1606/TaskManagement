package org.example.tay.springbkafkamongodbdemo.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tay.springbkafkamongodbdemo.application.service.TaskService;
import org.example.tay.springbkafkamongodbdemo.domain.event.TaskCreatedEvent;
import org.example.tay.springbkafkamongodbdemo.domain.event.TaskDeletedEvent;
import org.example.tay.springbkafkamongodbdemo.domain.event.TaskUpdatedEvent;
import org.example.tay.springbkafkamongodbdemo.shared.dto.TaskRequestDTO;
import org.example.tay.springbkafkamongodbdemo.shared.dto.TaskResponseDTO;
import org.example.tay.springbkafkamongodbdemo.shared.dto.TaskStatusUpdateDTO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

 // PRESENTATION LAYER — REST API Controller
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final ApplicationEventPublisher eventPublisher;

    // WRITE operations — publish events → Kafka → Consumer → Service → MongoDB

     // Creates a new task. Publishes TaskCreatedEvent.
     // Returns 202 Accepted — task is saved asynchronously after Kafka delivers the event.
    @PostMapping
    public Mono<ResponseEntity<String>> createTask(
            @Valid @RequestBody TaskRequestDTO request) {

        String taskId = (request.getId() != null && !request.getId().isBlank())
                ? request.getId()
                : "TASK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("POST /api/tasks - Publishing TaskCreatedEvent for: {}", taskId);

        eventPublisher.publishEvent(new TaskCreatedEvent(
                taskId,
                request.getTitle(),
                request.getDescription(),
                request.getStatus(),
                request.getPriority(),
                request.getDueDate(),
                LocalDateTime.now()
        ));

        return Mono.just(ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body("Task creation event published. Task ID: " + taskId));
    }

     // Full update — all fields (title, description, priority, dueDate, status).
     // Only allowed when task is PENDING.
    @PutMapping("/{id}")
    public Mono<ResponseEntity<String>> updateTask(
            @NotBlank @PathVariable String id,
            @Valid @RequestBody TaskRequestDTO request) {

        log.info("PUT /api/tasks/{} - Publishing TaskUpdatedEvent (full update)", id);

        eventPublisher.publishEvent(new TaskUpdatedEvent(
                id,
                request.getTitle(),        // non-null title triggers MODE B (full update)
                request.getDescription(),
                request.getStatus(),
                request.getPriority(),
                request.getDueDate(),
                LocalDateTime.now()
        ));

        return Mono.just(ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body("Task update event published. Task ID: " + id));
    }

     // Status-only update — only changes the status field.
     // Works for both PENDING → IN_PROGRESS and IN_PROGRESS → COMPLETED.
    @PatchMapping("/{id}/status")
    public Mono<ResponseEntity<String>> updateTaskStatus(
            @NotBlank @PathVariable String id,
            @Valid @RequestBody TaskStatusUpdateDTO statusUpdate) {

        log.info("PATCH /api/tasks/{}/status - Publishing status-only update: {}", id, statusUpdate.getStatus());

        // Pass null for title/description/priority/dueDate to signal MODE A (status-only)
        eventPublisher.publishEvent(new TaskUpdatedEvent(
                id,
                null,                     // null title only update status
                null,
                statusUpdate.getStatus(),
                null,
                null,
                LocalDateTime.now()
        ));

        return Mono.just(ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body("Status update event published. Task ID: " + id +
                      ", New status: " + statusUpdate.getStatus()));
    }

     // Returns 202 Accepted — deletion is applied asynchronously.
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<String>> deleteTask(@NotBlank @PathVariable String id) {
        log.info("DELETE /api/tasks/{} - Publishing TaskDeletedEvent", id);

        eventPublisher.publishEvent(new TaskDeletedEvent(
                id,
                "Task with ID " + id + " deleted",
                LocalDateTime.now()
        ));

        return Mono.just(ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body("Task deletion event published. Task ID: " + id));
    }

    // READ operations — query directly → Service → MongoDB

     // Returns all tasks as a reactive stream.
    @GetMapping
    public Flux<TaskResponseDTO> getAllTasks() {
        log.info("GET /api/tasks - Fetching all tasks");
        return taskService.getAllTasks();
    }

     // Returns a single task. Returns 404 if not found.
    @GetMapping("/{id}")
    public Mono<ResponseEntity<TaskResponseDTO>> getTaskById(@PathVariable String id) {
        log.info("GET /api/tasks/{} - Fetching task by id", id);
        return taskService.getTaskById(id)
                .map(ResponseEntity::ok);
    }
}
