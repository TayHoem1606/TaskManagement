package org.example.tay.taskmanagmentkafkareact.domain.repository;

import org.example.tay.taskmanagmentkafkareact.domain.model.Task;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskFilterDTO;
import reactor.core.publisher.Flux;

public interface TaskRepositoryCustom {
    Flux<Task> findByFilters(TaskFilterDTO filter);
}
