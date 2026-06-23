package org.example.tay.springbkafkamongodbdemo.domain.repository;

import org.example.tay.springbkafkamongodbdemo.domain.model.Task;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface TaskRepository extends ReactiveMongoRepository<Task, String> {
}
