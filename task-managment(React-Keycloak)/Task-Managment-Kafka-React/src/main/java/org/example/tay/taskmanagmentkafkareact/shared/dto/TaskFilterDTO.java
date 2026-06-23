package org.example.tay.taskmanagmentkafkareact.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskPriority;
import org.example.tay.taskmanagmentkafkareact.domain.model.TaskStatus;

import java.time.LocalDate;


 //SHARED LAYER — Filter + Sort parameters for GET /api/tasks.

 //All fields are optional. When a field is null, that filter is skipped.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskFilterDTO {

    private TaskStatus status;

    private TaskPriority priority;

    private String titleSearch;

    private String createdBySearch;

    private LocalDate dueBefore;

    private LocalDate dueAfter;

    private Boolean overdueOnly;

    /**
     * When true, returns only tasks where createdBy == current user's username.
     * Set from the JWT in TaskController — the client never sends their own username.
     * Takes precedence over createdBySearch when set.
     */
    private Boolean myTasksOnly;

    /** The authenticated user's username — injected by controller from JWT for myTasksOnly */
    private String currentUsername;

    // ── Sorting ──────────────────────────────────────────────────
    /**
     * Field to sort by. Allowed values (validated in TaskServiceImpl):
     *   dueDate | priority | status | createdBy | title | createdAt
     * Default: createdAt (newest first)
     */
    private String sortBy;

    /**
     * Sort direction: "asc" or "desc".
     * Default: desc
     */
    private String sortDir;
}

