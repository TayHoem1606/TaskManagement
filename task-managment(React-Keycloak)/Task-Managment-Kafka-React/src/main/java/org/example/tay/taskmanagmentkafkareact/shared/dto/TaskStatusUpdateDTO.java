package org.example.tay.taskmanagmentkafkareact.shared.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskStatus;

// SHARED LAYER — Status-Only Update DTO

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusUpdateDTO {

    @NotNull(message = "Status must not be null")
    private TaskStatus status;
}
