package org.example.tay.taskmanagmentkafkareact.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskPriority;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskStatus;

import java.time.LocalDate;

//  SHARED LAYER — Incoming Request DTO
//
//  Carries validated input from the HTTP request.
//  Validation annotations ensure invalid data is rejected before reaching the service.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestDTO {

    private String id;

    @NotBlank(message = "Title must not be empty")
    @Size(max = 50, message = "Title cannot exceed 50 characters")
    private String title;

    @NotBlank(message = "Description must not be empty")
    @Size(max = 100, message = "Description cannot exceed 100 characters")
    private String description;

    @NotNull(message = "Status must not be null")
    private TaskStatus status;

    @NotNull(message = "Priority must not be null")
    private TaskPriority priority;

    @NotNull(message = "Due date must not be null")
    @FutureOrPresent(message = "Due date cannot be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;
}
