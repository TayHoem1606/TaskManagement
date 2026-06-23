package org.example.tay.taskmanagmentkafkareact.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskPriority;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

//  SHARED LAYER — Outgoing Response DTO
//  Carries task data back to the HTTP client.
//  Decouples the domain model from the API contract.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponseDTO {

    private String id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
