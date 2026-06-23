package org.example.tay.springbkafkamongodbdemo.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.tay.springbkafkamongodbdemo.domain.model.TaskPriority;
import org.example.tay.springbkafkamongodbdemo.domain.model.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@AllArgsConstructor
public class TaskUpdatedEvent {
    private final String taskId;
    private final String title;
    private final String description;
    private final TaskStatus status;
    private final TaskPriority priority;
    private final LocalDate dueDate;
    private final LocalDateTime occurredAt;
}
