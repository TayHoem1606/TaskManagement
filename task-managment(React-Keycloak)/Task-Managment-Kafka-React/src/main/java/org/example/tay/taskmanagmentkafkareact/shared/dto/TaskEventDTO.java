package org.example.tay.taskmanagmentkafkareact.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskEventType;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskPriority;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;


//  SHARED LAYER — Kafka Event DTO
//
//  Carries the FULL task payload inside every Kafka message.
//  "Event-Carried State Transfer":
//  the event itself carries all state, so the consumer is self-sufficient
//  nd does not need to call back to any other service.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskEventDTO {

    //Event metadata
    private TaskEventType eventType;       // TASK_CREATED | TASK_UPDATED | TASK_DELETED
    private LocalDateTime timestamp;

    //Full task payload (used by consumer to save/update/delete)
    private String taskId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;

    private LocalDate dueDate;
    private String createdBy;
    private String updatedBy;
}
