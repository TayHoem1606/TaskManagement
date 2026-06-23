package org.example.tay.taskmanagmentkafkareact.domain.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.tay.taskmanagmentkafkareact.domain.model.Task;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskStatus;
import org.example.tay.taskmanagmentkafkareact.domain.repository.TaskRepositoryCustom;
import org.example.tay.taskmanagmentkafkareact.shared.dto.TaskFilterDTO;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class TaskRepositoryImpl implements TaskRepositoryCustom {

    private final ReactiveMongoTemplate mongoTemplate;

    @Override
    public Flux<Task> findByFilters(TaskFilterDTO filter) {
        List<Criteria> criteriaList = new ArrayList<>();

        //Status & Priority
        if (filter.getStatus() != null)
            criteriaList.add(Criteria.where("status").is(filter.getStatus()));
        if (filter.getPriority() != null)
            criteriaList.add(Criteria.where("priority").is(filter.getPriority()));

        // Title & CreatedBy Search (Regex)
        if (filter.getTitleSearch() != null && !filter.getTitleSearch().isBlank()) {
            criteriaList.add(Criteria.where("title").regex(filter.getTitleSearch(), "i"));
        }

        // User Ownership (myTasksOnly takes precedence)
        if (Boolean.TRUE.equals(filter.getMyTasksOnly()) && filter.getCurrentUsername() != null) {
            criteriaList.add(Criteria.where("createdBy").is(filter.getCurrentUsername()));
        } else if (filter.getCreatedBySearch() != null && !filter.getCreatedBySearch().isBlank()) {
            criteriaList.add(Criteria.where("createdBy").regex(filter.getCreatedBySearch(), "i"));
        }

        // 4. Date Logic (Overdue takes precedence)
        if (Boolean.TRUE.equals(filter.getOverdueOnly())) {
            criteriaList.add(Criteria.where("dueDate").lt(LocalDate.now()));
            criteriaList.add(Criteria.where("status").ne(TaskStatus.COMPLETED));
        } else {
            if (filter.getDueAfter() != null) criteriaList.add(Criteria.where("dueDate").gte(filter.getDueAfter()));
            if (filter.getDueBefore() != null) criteriaList.add(Criteria.where("dueDate").lte(filter.getDueBefore()));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        // 5. Sorting
        Sort.Direction dir = "asc".equalsIgnoreCase(filter.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = (filter.getSortBy() != null) ? filter.getSortBy() : "createdAt";
        query.with(Sort.by(dir, sortField));

        return mongoTemplate.find(query, Task.class);
    }
}
