package org.example.tay.springbkafkamongodbdemo.application.service;

import org.example.tay.springbkafkamongodbdemo.shared.dto.TaskEventDTO;
import org.example.tay.springbkafkamongodbdemo.shared.dto.TaskResponseDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


//  Separation of concerns is maintained through the flow:
//    Controller -> event -> Kafka -> Consumer -> TaskService (command)
//    Controller -> TaskService (query) -> MongoDB

public interface TaskService {

    // ── QUERY operations (Read) ──────────────────────────────
    Flux<TaskResponseDTO> getAllTasks();
    Mono<TaskResponseDTO> getTaskById(String id);

    // ── COMMAND operations (Write) — triggered by Kafka Consumer ──
    Mono<Void> handleCreate(TaskEventDTO event);
    Mono<Void> handleUpdate(TaskEventDTO event);
    Mono<Void> handleDelete(TaskEventDTO event);
}
