package org.example.tay.springbkafkamongodbdemo.shared.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(String id) {
        super("Task not found with id: " + id);
    }
}
