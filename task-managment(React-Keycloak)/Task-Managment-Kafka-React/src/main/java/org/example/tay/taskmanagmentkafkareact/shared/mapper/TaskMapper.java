package org.example.tay.taskmanagmentkafkareact.shared.mapper;

import org.example.tay.taskmanagmentkafkareact.domain.model.Task;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    TaskResponseDTO toResponseDTO(Task task);
}
