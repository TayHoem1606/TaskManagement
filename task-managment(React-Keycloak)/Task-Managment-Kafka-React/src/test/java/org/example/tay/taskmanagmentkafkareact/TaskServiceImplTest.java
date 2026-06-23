package org.example.tay.taskmanagmentkafkareact;

import org.example.tay.taskmanagmentkafkareact.application.service.impl.TaskServiceImpl;
import org.example.tay.taskmanagmentkafkareact.domain.model.Task;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskEventType;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskPriority;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskStatus;
import org.example.tay.taskmanagmentkafkareact.domain.repository.TaskRepository;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskEventDTO;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskResponseDTO;
import org.example.tay.taskmanagmentkafkareact.shared.exception.ConflictException;
import org.example.tay.taskmanagmentkafkareact.shared.mapper.TaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task pendingTask;
    private TaskResponseDTO pendingTaskDTO;

    @BeforeEach
    void setUp() {
        pendingTask = Task.builder()
                .id("TASK-0001")
                .title("Test Task")
                .description("Test description")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.of(2027, 12, 31))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy("user1")
                .updatedBy("user1")
                .build();

        pendingTaskDTO = TaskResponseDTO.builder()
                .id("TASK-0001")
                .title("Test Task")
                .description("Test description")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.of(2027, 12, 31))
                .createdAt(pendingTask.getCreatedAt())
                .updatedAt(pendingTask.getUpdatedAt())
                .createdBy("user1")
                .updatedBy("user1")
                .build();
    }

    // ── Helper: build a Spring Security context ───────────────────
    // FIX: Use SecurityContextImpl (not a bare context holder) so
    // ReactiveSecurityContextHolder.withSecurityContext() has the right type.

    private Mono<org.springframework.security.core.context.SecurityContext>
    securityContextFor(String username, String... roles) {
        var authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
        var ctx  = new SecurityContextImpl(auth);
        return Mono.just(ctx);
    }

    // ── validateOwnership ─────────────────────────────────────────

    @Test
    @DisplayName("ADMIN can modify any task regardless of createdBy")
    void validateOwnership_admin_alwaysPasses() {
        StepVerifier.create(
                        taskService.validateOwnership(pendingTaskDTO)
                                .contextWrite(ReactiveSecurityContextHolder
                                        .withSecurityContext(securityContextFor("admin1", "ROLE_ADMIN")))
                )
                .expectNext(pendingTaskDTO)
                .verifyComplete();
    }

    @Test
    @DisplayName("USER can modify their own task")
    void validateOwnership_userOwnsTask_passes() {
        StepVerifier.create(
                        taskService.validateOwnership(pendingTaskDTO)
                                .contextWrite(ReactiveSecurityContextHolder
                                        .withSecurityContext(securityContextFor("user1", "ROLE_USER")))
                )
                .expectNext(pendingTaskDTO)
                .verifyComplete();
    }

    @Test
    @DisplayName("USER cannot modify another user's task — AccessDeniedException thrown")
    void validateOwnership_userNotOwner_throwsAccessDeniedException() {
        // FIX: Use expectErrorSatisfies instead of expectErrorMatches with instanceof cast.
        // expectErrorMatches with (ex -> ex instanceof X && ...) causes
        // "Inconvertible types" compile error because the lambda parameter is Throwable.
        // expectErrorSatisfies receives a Throwable and lets you assert on it safely.
        StepVerifier.create(
                        taskService.validateOwnership(pendingTaskDTO)
                                .contextWrite(ReactiveSecurityContextHolder
                                        .withSecurityContext(securityContextFor("user2", "ROLE_USER")))
                )
                .expectErrorSatisfies(ex -> {
                    org.assertj.core.api.Assertions.assertThat(ex)
                            .isInstanceOf(AccessDeniedException.class)
                            .hasMessageContaining("TASK-0001");
                })
                .verify();
    }

    // ── handleCreate — validation ─────────────────────────────────

    @Test
    @DisplayName("handleCreate fails with duplicate task ID → ConflictException")
    void handleCreate_duplicateId_throwsConflictException() {
        // ambiguous method call between existsById(String) and existsById(Publisher<String>)
        when(taskRepository.existsById(anyString())).thenReturn(Mono.just(true));

        var event = buildCreateEvent("TASK-0001", "Duplicate",
                LocalDate.now().plusMonths(3), "user1");

        StepVerifier.create(taskService.handleCreate(event))
                .expectErrorSatisfies(ex ->
                        org.assertj.core.api.Assertions.assertThat(ex)
                                .isInstanceOf(ConflictException.class)
                                .hasMessageContaining("already exists"))
                .verify();
    }

    @Test
    @DisplayName("handleCreate fails when due date is more than 1 year away → ConflictException")
    void handleCreate_dueDateTooFar_throwsConflictException() {
        // FIX: anyString() avoids the existsById overload ambiguity
        when(taskRepository.existsById(anyString())).thenReturn(Mono.just(false));

        var event = buildCreateEvent("TASK-0099", "Far Future",
                LocalDate.now().plusYears(2), "user1");

        StepVerifier.create(taskService.handleCreate(event))
                .expectErrorSatisfies(ex ->
                        org.assertj.core.api.Assertions.assertThat(ex)
                                .isInstanceOf(ConflictException.class)
                                .hasMessageContaining("1 year"))
                .verify();
    }

    @Test
    @DisplayName("handleCreate fails when createdBy is null → ConflictException")
    void handleCreate_missingCreatedBy_throwsConflictException() {
        when(taskRepository.existsById(anyString())).thenReturn(Mono.just(false));

        var event = buildCreateEvent("TASK-0099", "Missing Creator",
                LocalDate.now().plusMonths(3), null);

        StepVerifier.create(taskService.handleCreate(event))
                .expectErrorSatisfies(ex ->
                        org.assertj.core.api.Assertions.assertThat(ex)
                                .isInstanceOf(ConflictException.class)
                                .hasMessageContaining("createdBy"))
                .verify();
    }

    @Test
    @DisplayName("handleCreate saves task successfully with valid data")
    void handleCreate_validData_savesTask() {
        when(taskRepository.existsById(anyString())).thenReturn(Mono.just(false));
        // The previous error was caused by incorrect generic inference in older mockito versions.
        when(taskRepository.save(any(Task.class))).thenReturn(Mono.just(pendingTask));

        var event = buildCreateEvent("TASK-0099", "New Task",
                LocalDate.now().plusMonths(3), "user1");

        StepVerifier.create(taskService.handleCreate(event))
                .verifyComplete();
    }

    // ── handleDelete — idempotent ─────────────────────────────────

    @Test
    @DisplayName("handleDelete on non-existent task completes without error (idempotent)")
    void handleDelete_taskNotFound_completesSuccessfully() {
        when(taskRepository.findById(anyString())).thenReturn(Mono.empty());

        var event = TaskEventDTO.builder()
                .eventType(TaskEventType.TASK_DELETED)
                .taskId("TASK-XXXX")
                .build();

        StepVerifier.create(taskService.handleDelete(event))
                .verifyComplete();
    }

    // ── handleUpdate — status transitions ────────────────────────

    @Test
    @DisplayName("handleUpdate on COMPLETED task → ConflictException")
    void handleUpdate_completedTask_throwsConflictException() {
        Task completedTask = Task.builder()
                .id("TASK-0002")
                .status(TaskStatus.COMPLETED)
                .build();

        when(taskRepository.findById(anyString())).thenReturn(Mono.just(completedTask));

        var event = TaskEventDTO.builder()
                .eventType(TaskEventType.TASK_UPDATED)
                .taskId("TASK-0002")
                .title("New Title")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.now().plusMonths(1))
                .build();

        StepVerifier.create(taskService.handleUpdate(event))
                .expectErrorSatisfies(ex ->
                        org.assertj.core.api.Assertions.assertThat(ex)
                                .isInstanceOf(ConflictException.class)
                                .hasMessageContaining("COMPLETED"))
                .verify();
    }

    @Test
    @DisplayName("PENDING → COMPLETED directly is rejected → ConflictException")
    void handleUpdate_pendingToCompleted_throwsConflictException() {
        when(taskRepository.findById(anyString())).thenReturn(Mono.just(pendingTask));

        var event = TaskEventDTO.builder()
                .eventType(TaskEventType.TASK_UPDATED)
                .taskId("TASK-0001")
                .title("Updated")
                .description("Desc")
                .status(TaskStatus.COMPLETED)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.of(2027, 12, 31))
                .build();

        StepVerifier.create(taskService.handleUpdate(event))
                .expectErrorSatisfies(ex ->
                        org.assertj.core.api.Assertions.assertThat(ex)
                                .isInstanceOf(ConflictException.class)
                                .hasMessageContaining("IN_PROGRESS"))
                .verify();
    }

    @Test
    @DisplayName("Status-only update PENDING → IN_PROGRESS is allowed")
    void handleUpdate_statusOnly_pendingToInProgress_succeeds() {
        when(taskRepository.findById(anyString())).thenReturn(Mono.just(pendingTask));
        when(taskRepository.save(any(Task.class))).thenReturn(Mono.just(pendingTask));

        var event = TaskEventDTO.builder()
                .eventType(TaskEventType.TASK_UPDATED)
                .taskId("TASK-0001")
                .title(null)                    // null = status-only mode
                .status(TaskStatus.IN_PROGRESS)
                .updatedBy("user1")
                .build();

        StepVerifier.create(taskService.handleUpdate(event))
                .verifyComplete();
    }

    // ── Helper builders ───────────────────────────────────────────

    private TaskEventDTO buildCreateEvent(String taskId, String title,
                                          LocalDate dueDate, String createdBy) {
        return TaskEventDTO.builder()
                .eventType(TaskEventType.TASK_CREATED)
                .taskId(taskId)
                .title(title)
                .description("Description")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.MEDIUM)
                .dueDate(dueDate)
                .createdBy(createdBy)
                .build();
    }
}

