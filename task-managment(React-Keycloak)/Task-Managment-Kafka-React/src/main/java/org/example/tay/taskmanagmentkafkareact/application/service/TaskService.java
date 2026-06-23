package org.example.tay.taskmanagmentkafkareact.application.service;

import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskEventDTO;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskFilterDTO;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskRequestDTO;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskResponseDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface TaskService {

    // ── QUERY operations (Read) ──────────────────────────────
    Flux<TaskResponseDTO> getAllTasks();
    Mono<TaskResponseDTO> getTaskById(String id);

    /**
     * Returns tasks matching all non-null fields in the filter,
     * sorted by filter.sortBy / filter.sortDir.
     * When all filter fields are null this behaves identically to getAllTasks().
     */
    Flux<TaskResponseDTO> getFilteredTasks(TaskFilterDTO filter);

    // ── VALIDATION operation (New) ───────────────────────────
    Mono<TaskResponseDTO> validateOwnership(TaskResponseDTO task);
    Mono<Void> validateUpdate(String taskId, TaskRequestDTO request);
    Mono<String> generateTaskId();

    // ── COMMAND operations (Write) — triggered by Kafka Consumer ──
    Mono<Void> handleCreate(TaskEventDTO event);
    Mono<Void> handleUpdate(TaskEventDTO event);
    Mono<Void> handleDelete(TaskEventDTO event);
}
