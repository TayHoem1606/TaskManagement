import { useEffect, useState, useCallback, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import {
  getAllTasks,
  deleteTask,
  updateTaskStatus,
} from "../services/taskService";
import { StatusBadge, PriorityBadge } from "../components/Badges";
import TaskFilterBar from "../components/TaskFilterBar";
import "./TaskListPage.css";
import Button from "../components/Button";

const EMPTY_FILTERS = {
  status: null,
  priority: null,
  titleSearch: null,
  createdBy: null,
  dueBefore: null,
  dueAfter: null,
  overdueOnly: null,
  myTasksOnly: null,
  sortBy: null,
  sortDir: "desc",
};

export default function TaskListPage() {
  const navigate = useNavigate();
  const { token, username, isAdmin, updateToken } = useAuth();

  const updateTokenRef = useRef(updateToken);

  useEffect(() =>{
    updateTokenRef.current = updateToken;
  }, [updateToken])

  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [toast, setToast] = useState(null);
  const [deleting, setDeleting] = useState(null);

  // ── Filter state ─────────────────────────────────────────────
  const [filters, setFilters] = useState(EMPTY_FILTERS);

  // Count active filters for the badge on the filter bar header.
  // sortDir alone ("desc") is not counted as an "active" filter.
  const activeCount = Object.entries(filters).filter(([key, val]) => {
    if (key === "sortDir") return false;
    return val !== null && val !== "" && val !== false;
  }).length;

  const fetchTasks = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      
      await updateTokenRef.current(30); 
      
      const data = await getAllTasks(token, filters);
      setTasks(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [token, filters]); 

  useEffect(() => { fetchTasks(); }, [fetchTasks]);

  // ── Filter handlers ───────────────────────────────────────────
  function handleFilterChange(newFilters) {
    setFilters(newFilters);
    // fetchTasks re-runs automatically because filters is in its dependency array
  }

  function handleReset() {
    setFilters(EMPTY_FILTERS);
  }

  // ── Toast ─────────────────────────────────────────────────────
  function showToast(msg, type = "success") {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3500);
  }

  // ── Delete ────────────────────────────────────────────────────
  async function handleDelete(task) {
    if (!window.confirm(`Delete "${task.title}"? This cannot be undone.`))
      return;
    try {
      setDeleting(task.id);
      await updateToken(30);
      await deleteTask(token, task.id);
      showToast(`"${task.title}" deletion event sent. Refreshing…`);
      setTimeout(fetchTasks, 1500);
    } catch (err) {
      showToast(
        err.message.includes("403")
          ? "You can only delete tasks you created."
          : err.message,
        "error",
      );
    } finally {
      setDeleting(null);
    }
  }

  // ── Status advance ────────────────────────────────────────────
  async function handleAdvanceStatus(task) {
    const next = task.status === "PENDING" ? "IN_PROGRESS" : "COMPLETED";
    try {
      await updateToken(30);
      await updateTaskStatus(token, task.id, next);
      showToast(`Status updated to ${next}. Refreshing…`);
      setTimeout(fetchTasks, 1500);
    } catch (err) {
      showToast(err.message, "error");
    }
  }

  // ── Column sort — clicking a header toggles sort direction ────
  function handleColumnSort(field) {
    const isSameField = filters.sortBy === field;
    setFilters((prev) => ({
      ...prev,
      sortBy: field,
      sortDir: isSameField && prev.sortDir === "asc" ? "desc" : "asc",
    }));
  }

  // ── Ownership check ───────────────────────────────────────────
  const canModify = (task) => isAdmin || task.createdBy === username;

  // ── Sort indicator for column headers ─────────────────────────
  function sortIcon(field) {
    if (filters.sortBy !== field)
      return <span className="sort-icon sort-icon--inactive">↕</span>;
    return (
      <span className="sort-icon sort-icon--active">
        {filters.sortDir === "asc" ? "↑" : "↓"}
      </span>
    );
  }

  // ── Render ────────────────────────────────────────────────────
  return (
    <div className="page">
      {/* Toast */}
      {toast && <div className={`toast toast--${toast.type}`}>{toast.msg}</div>}

      {/* Page header */}
      <div className="task-list__header">
        <div>
          <h1 className="task-list__title">All Tasks</h1>
          {!loading && !error && (
            <p className="task-list__subtitle">
              {tasks.length} task{tasks.length !== 1 ? "s" : ""}
              {activeCount > 0
                ? ` matching ${activeCount} filter${activeCount !== 1 ? "s" : ""}`
                : " total"}
            </p>
          )}
        </div>
        <Button
          name="btn btn--primary"
          onClick={() => navigate("/tasks/new")}
        >
          + New Task
        </Button>
      </div>

      {/* ── Filter bar ──────────────────────────────────────── */}
      <TaskFilterBar
        filters={filters}
        onFilterChange={handleFilterChange}
        onReset={handleReset}
        activeCount={activeCount}
      />

      {/* Loading */}
      {loading && (
        <div className="spinner-wrap">
          <div className="spinner" />
          <p>Loading tasks…</p>
        </div>
      )}

      {/* Error */}
      {!loading && error && (
        <div className="task-list__error">
          <strong>Error:</strong> {error}
          <Button name="task-list__retry-btn" onClick={fetchTasks}>
            Retry
          </Button>
        </div>
      )}

      {/* Empty — no tasks at all */}
      {!loading && !error && tasks.length === 0 && activeCount === 0 && (
        <div className="empty-state">
          <div className="empty-state__icon">📭</div>
          <p className="empty-state__title">No tasks yet</p>
          <p className="empty-state__subtitle">
            Create your first task to get started.
          </p>
          <Button
            name="btn btn--primary"
            onClick={() => navigate("/tasks/new")}
          >
            Create Task
          </Button>
        </div>
      )}

      {/* Empty — filters returned no results */}
      {!loading && !error && tasks.length === 0 && activeCount > 0 && (
        <div className="empty-state">
          <div className="empty-state__icon">🔍</div>
          <p className="empty-state__title">No tasks match your filters</p>
          <p className="empty-state__subtitle">
            Try adjusting or clearing the filters above.
          </p>
          <Button name="btn btn--primary" onClick={handleReset}>
            Clear filters
          </Button>
        </div>
      )}

      {/* Task table */}
      {!loading && !error && tasks.length > 0 && (
        <div className="table-wrap">
          <table className="task-table">
            <thead>
              <tr>
                <th>ID</th>

                {/* Sortable columns — click header to sort */}
                <th
                  className="task-table__th--sortable"
                  onClick={() => handleColumnSort("title")}
                  title="Sort by title"
                >
                  Title {sortIcon("title")}
                </th>

                <th
                  className="task-table__th--sortable"
                  onClick={() => handleColumnSort("status")}
                  title="Sort by status"
                >
                  Status {sortIcon("status")}
                </th>

                <th
                  className="task-table__th--sortable"
                  onClick={() => handleColumnSort("priority")}
                  title="Sort by priority"
                >
                  Priority {sortIcon("priority")}
                </th>

                <th
                  className="task-table__th--sortable"
                  onClick={() => handleColumnSort("dueDate")}
                  title="Sort by due date"
                >
                  Due Date {sortIcon("dueDate")}
                </th>

                <th
                  className="task-table__th--sortable"
                  onClick={() => handleColumnSort("createdBy")}
                  title="Sort by creator"
                >
                  Created By {sortIcon("createdBy")}
                </th>

                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {tasks.map((task) => {
                const isOverdue =
                  task.status !== "COMPLETED" &&
                  task.dueDate &&
                  new Date(task.dueDate) < new Date(new Date().toDateString());

                return (
                  <tr
                    key={task.id}
                    className={isOverdue ? "task-table__row--overdue" : ""}
                  >
                    <td>
                      <span
                        className="task-table__id-link"
                        onClick={() => navigate(`/tasks/${task.id}`)}
                      >
                        {task.id}
                      </span>
                    </td>
                    <td>
                      <span className="task-table__title">
                        {task.title}
                        {isOverdue && (
                          <span
                            className="task-table__overdue-tag"
                            title="Overdue"
                          >
                            ⚠
                          </span>
                        )}
                      </span>
                    </td>
                    <td>
                      <StatusBadge status={task.status} />
                    </td>
                    <td>
                      <PriorityBadge priority={task.priority} />
                    </td>
                    <td>
                      <span
                        className={isOverdue ? "task-table__date--overdue" : ""}
                      >
                        {task.dueDate}
                      </span>
                    </td>
                    <td>
                      <span
                        className={
                          task.createdBy === username
                            ? "task-table__own-tag"
                            : ""
                        }
                      >
                        {task.createdBy || "—"}
                        {task.createdBy === username && " (you)"}
                      </span>
                    </td>
                    <td>
                      <div className="task-table__actions">
                        <Button name="btn btn--sm btn--outline-blue" onClick={() => navigate(`/tasks/${task.id}`)}>View</Button>

                        {canModify(task) && task.status !== "COMPLETED" && (
                          <Button name="btn btn--sm btn--outline-green" onClick={() => navigate(`/tasks/${task.id}/edit`)}>
                            Edit
                          </Button>
                        )}

                        {canModify(task) && task.status !== "COMPLETED" && (
                          <Button name="btn btn--sm btn--outline-purple" onClick={() => handleAdvanceStatus(task)}>
                            {task.status === "PENDING" ? "▶ Start" : "✓ Done"}
                          </Button>
                        )}

                        {canModify(task) && (
                          <Button name="btn btn--sm btn--outline-red" onClick={() => handleDelete(task)} disabled={deleting === task.id}>
                            {deleting === task.id ? "…" : "Delete"}
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
