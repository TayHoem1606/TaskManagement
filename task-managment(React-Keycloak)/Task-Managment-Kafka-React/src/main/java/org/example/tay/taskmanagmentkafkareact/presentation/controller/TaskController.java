package org.example.tay.taskmanagmentkafkareact.presentation.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tay.taskmanagmentkafkareact.application.service.TaskService;
import org.example.tay.taskmanagmentkafkareact.domain.event.TaskCreatedEvent;
import org.example.tay.taskmanagmentkafkareact.domain.event.TaskDeletedEvent;
import org.example.tay.taskmanagmentkafkareact.domain.event.TaskUpdatedEvent;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskPriority;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskStatus;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskFilterDTO;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskRequestDTO;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskResponseDTO;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskStatusUpdateDTO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
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
            @Valid @RequestBody TaskRequestDTO request, Authentication authentication) {
        String username = extractUsername(authentication);
            log.info("POST /api/tasks - Publishing TaskCreatedEvent for user: {}", username);

        return taskService.generateTaskId()
                 .flatMap(taskId -> {
                     log.info("POST /api/tasks - Publishing TaskCreatedEvent for: {}", taskId);
                     eventPublisher.publishEvent(new TaskCreatedEvent(
                             taskId,
                             request.getTitle(),
                             request.getDescription(),
                             request.getStatus(),
                             request.getPriority(),
                             request.getDueDate(),
                             LocalDateTime.now(),
                             username
                     ));
                     return Mono.just(ResponseEntity
                             .status(HttpStatus.CREATED)
                             .body("Task creation event published. Task ID: " + taskId));
                 });
    }

     // Full update — all fields (title, description, priority, dueDate, status).
     // Only allowed when task is PENDING.
    @PutMapping("/{id}")
    public Mono<ResponseEntity<String>> updateTask(
            @NotBlank @PathVariable String id,
            @Valid @RequestBody TaskRequestDTO request,
            Authentication authentication) {

        String username = extractUsername(authentication);

        log.info("PUT /api/tasks/{} - Publishing TaskUpdatedEvent (full update)", id);

        return taskService.getTaskById(id) // 1. Fetch task first
                .flatMap(taskService::validateOwnership)// 2. Validate while JWT is active
                .then(taskService.validateUpdate(id, request))
                .then(Mono.defer(() -> {
                    eventPublisher.publishEvent(new TaskUpdatedEvent(
                            id, request.getTitle(), request.getDescription(),
                            request.getStatus(), request.getPriority(), request.getDueDate(),
                            LocalDateTime.now(), username
                    ));
                    return Mono.just(ResponseEntity.status(HttpStatus.ACCEPTED)
                            .body("Update event published."));
                }));
    }

     // Status-only update — only changes the status field.
     // Works for both PENDING → IN_PROGRESS and IN_PROGRESS → COMPLETED.
    @PatchMapping("/{id}/status")
    public Mono<ResponseEntity<String>> updateTaskStatus(
            @NotBlank @PathVariable String id,
            @Valid @RequestBody TaskStatusUpdateDTO statusUpdate,
            Authentication authentication) {

        String username = extractUsername(authentication);
        log.info("PATCH /api/tasks/{}/status - Publishing status-only update: {}", id, statusUpdate.getStatus());

        return taskService.getTaskById(id) // 1. Fetch task first
                .flatMap(taskService::validateOwnership)// 2. Validate while JWT is active
                .flatMap(validTask -> {
                    // 3. Only publish if validation passed
                    // Pass null for title/description/priority/dueDate to signal MODE A (status-only)
                    eventPublisher.publishEvent(new TaskUpdatedEvent(
                            id,
                            null,                     // null title only update status
                            null,
                            statusUpdate.getStatus(),
                            null,
                            null,
                            LocalDateTime.now(),
                            username
                    ));
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.ACCEPTED)
                            .body("Status update event published. Task ID: " + id +
                                    ", New status: " + statusUpdate.getStatus()));
                });
    }

     @DeleteMapping("/{id}")
     public Mono<ResponseEntity<String>> deleteTask(
             @NotBlank @PathVariable String id,
             Authentication authentication) {

         String username = extractUsername(authentication);
         return taskService.getTaskById(id) // 1. Fetch task first
                 .flatMap(taskService::validateOwnership)// 2. Validate while JWT is active
                 .flatMap(validTask -> {
                     // 3. Only publish if validation passed
                     eventPublisher.publishEvent(new TaskDeletedEvent(id, validTask.getTitle(), LocalDateTime.now(),username));
                     return Mono.just(ResponseEntity.status(HttpStatus.ACCEPTED)
                             .body("Task deletion event published. Task ID: " + id));
                 });
     }

    // READ operations — query directly → Service → MongoDB

     // Returns all tasks as a reactive stream.
     @GetMapping
     public Flux<TaskResponseDTO> getAllTasks(
             Authentication authentication,

             // ── Filter params ─────────────────────────────────────
             @RequestParam(required = false) TaskStatus status,
             @RequestParam(required = false) TaskPriority priority,
             @RequestParam(required = false) String       titleSearch,
             @RequestParam(required = false) String       createdBy,
             @RequestParam(required = false)
             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore,

             @RequestParam(required = false)
             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueAfter,

             @RequestParam(required = false) Boolean overdueOnly,
             @RequestParam(required = false) Boolean myTasksOnly,

             // ── Sort params ───────────────────────────────────────
             @RequestParam(required = false) String sortBy,
             @RequestParam(required = false) String sortDir
     ) {
         String username = extractUsername(authentication);
         log.info("GET /api/tasks — user: {}, status: {}, priority: {}, titleSearch: {}, " +
                         "createdBy: {}, dueBefore: {}, dueAfter: {}, overdueOnly: {}, " +
                         "myTasksOnly: {}, sortBy: {}, sortDir: {}",
                 username, status, priority, titleSearch, createdBy,
                 dueBefore, dueAfter, overdueOnly, myTasksOnly, sortBy, sortDir);

         // Check if any filter or sort param was actually provided.
         // If not, use the fast getAllTasks() path (no Criteria overhead).
         boolean hasFilters = status != null || priority != null
                 || (titleSearch != null && !titleSearch.isBlank())
                 || (createdBy   != null && !createdBy.isBlank())
                 || dueBefore != null || dueAfter != null
                 || Boolean.TRUE.equals(overdueOnly)
                 || Boolean.TRUE.equals(myTasksOnly)
                 || (sortBy != null && !sortBy.isBlank())
                 || (sortDir != null && !sortDir.isBlank());

         if (!hasFilters) {
             return taskService.getAllTasks();
         }

         TaskFilterDTO filter = TaskFilterDTO.builder()
                 .status(status)
                 .priority(priority)
                 .titleSearch(titleSearch)
                 .createdBySearch(createdBy)
                 .dueBefore(dueBefore)
                 .dueAfter(dueAfter)
                 .overdueOnly(overdueOnly)
                 .myTasksOnly(myTasksOnly)
                 .currentUsername(username)    // injected from JWT — never from client
                 .sortBy(sortBy)
                 .sortDir(sortDir)
                 .build();

         return taskService.getFilteredTasks(filter);
     }

     // Returns a single task. Returns 404 if not found.
     @GetMapping("/{id}")
     public Mono<ResponseEntity<TaskResponseDTO>> getTaskById(
             @NotBlank @PathVariable String id,
             Authentication authentication) {
         log.info("GET /api/tasks/{} — user: {}", id, extractUsername(authentication));
         return taskService.getTaskById(id)
                 .map(ResponseEntity::ok);
     }

     // ── JWT extraction helpers ────────────────────────────────────

     /**
      * Extracts the username from the Authentication object.
      */
     private String extractUsername(Authentication authentication) {
         return authentication != null ? authentication.getName() : "anonymous";
     }
}
