package org.example.tay.springbkafkamongodbdemo.shared.mapper;

import org.example.tay.springbkafkamongodbdemo.domain.model.Task;
import org.example.tay.springbkafkamongodbdemo.shared.dto.TaskResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    TaskResponseDTO toResponseDTO(Task task);
}
