package org.example.tay.springbkafkamongodbdemo.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;


@Getter
@AllArgsConstructor
public class TaskDeletedEvent {
    private final String taskId;
    private final String title;
    private final LocalDateTime occurredAt;
}
