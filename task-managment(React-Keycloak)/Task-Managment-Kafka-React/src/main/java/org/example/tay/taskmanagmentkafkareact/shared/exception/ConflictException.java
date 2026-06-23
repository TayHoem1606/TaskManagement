package org.example.tay.taskmanagmentkafkareact.shared.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
