package org.example.tay.taskmanagmentkafkareact.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;


//  DOMAIN LAYER — Aggregate Root
//
//  This is the core domain entity. It represents a Task in the business domain.
//  It does NOT depend on any framework-specific logic (no Spring MVC, no Kafka).
//  MongoDB annotations are allowed because they describe how the domain is persisted.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tasks")
public class Task {

    @Id
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
