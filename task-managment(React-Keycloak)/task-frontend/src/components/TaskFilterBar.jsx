import { useState } from 'react';
import './TaskFilterBar.css';
import Button from './Button';

/**
 * TaskFilterBar — filter and sort controls for the task list.
 * Layout:
 *   Row 1: Title search | Status | Priority | CreatedBy search
 *   Row 2: Due After | Due Before | [Overdue] [My Tasks] quick-toggle buttons
 *   Row 3: Sort by | Sort direction | [Clear All] (only shown when filters active)
 */
export default function TaskFilterBar({ filters, onFilterChange, onReset, activeCount }) {
  const [expanded, setExpanded] = useState(true);

  // Update a single filter field and notify the parent immediately
  const set = (field) => (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    onFilterChange({ ...filters, [field]: value || null });
  };

  // Toggle boolean quick-filter (overdueOnly / myTasksOnly)
  const toggle = (field) => () => {
    onFilterChange({ ...filters, [field]: !filters[field] || null });
  };

  return (
    <div className="filter-bar">
      {/* ── Header ─────────────────────────────────────────────── */}
      <div className="filter-bar__header">
        <Button
          name="filter-bar__toggle"
          onClick={() => setExpanded((v) => !v)}
          title={expanded ? 'Collapse filters' : 'Expand filters'}
        >
          Filters &amp; Sort
          {activeCount > 0 && (
            <span className="filter-bar__active-badge">{activeCount}</span>
          )}
          <span className="filter-bar__chevron">{expanded ? '▲' : '▼'}</span>
        </Button>

        {activeCount > 0 && (
          <Button name="filter-bar__clear-btn" onClick={onReset}>
            ✕ Clear all ({activeCount})
          </Button>
        )}
      </div>

      {/* ── Filter rows (collapsible) ───────────────────────────── */}
      {expanded && (
        <div className="filter-bar__body">

          {/* Row 1: Text searches + enum dropdowns */}
          <div className="filter-bar__row">
            {/* Title search */}
            <div className="filter-bar__field filter-bar__field--grow">
              <label className="filter-bar__label">Title</label>
              <input
                className="filter-bar__input"
                type="text"
                placeholder="Search title…"
                value={filters.titleSearch || ''}
                onChange={(e) => onFilterChange({
                  ...filters,
                  titleSearch: e.target.value || null,
                })}
              />
            </div>

            {/* Status */}
            <div className="filter-bar__field">
              <label className="filter-bar__label">Status</label>
              <select
                className="filter-bar__select"
                value={filters.status || ''}
                onChange={(e) => onFilterChange({
                  ...filters,
                  status: e.target.value || null,
                  // clear overdueOnly when user manually picks COMPLETED status
                  overdueOnly: e.target.value === 'COMPLETED' ? null : filters.overdueOnly,
                })}
              >
                <option value="">All statuses</option>
                <option value="PENDING">PENDING</option>
                <option value="IN_PROGRESS">IN PROGRESS</option>
                <option value="COMPLETED">COMPLETED</option>
              </select>
            </div>

            {/* Priority */}
            <div className="filter-bar__field">
              <label className="filter-bar__label">Priority</label>
              <select
                className="filter-bar__select"
                value={filters.priority || ''}
                onChange={(e) => onFilterChange({
                  ...filters,
                  priority: e.target.value || null,
                })}
              >
                <option value="">All priorities</option>
                <option value="HIGH">HIGH</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="LOW">LOW</option>
              </select>
            </div>

            {/* Created by */}
            <div className="filter-bar__field filter-bar__field--grow">
              <label className="filter-bar__label">Created by</label>
              <input
                className="filter-bar__input"
                type="text"
                placeholder="Search username…"
                value={filters.createdBy || ''}
                // Disable text input when myTasksOnly is on (it overrides this)
                disabled={!!filters.myTasksOnly}
                onChange={(e) => onFilterChange({
                  ...filters,
                  createdBy: e.target.value || null,
                })}
              />
            </div>
          </div>

          {/* Row 2: Date range + quick toggles */}
          <div className="filter-bar__row">
            {/* Due after */}
            <div className="filter-bar__field">
              <label className="filter-bar__label">Due after</label>
              <input
                className="filter-bar__input"
                type="date"
                value={filters.dueAfter || ''}
                disabled={!!filters.overdueOnly}
                onChange={(e) => onFilterChange({
                  ...filters,
                  dueAfter: e.target.value || null,
                })}
              />
            </div>

            {/* Due before */}
            <div className="filter-bar__field">
              <label className="filter-bar__label">Due before</label>
              <input
                className="filter-bar__input"
                type="date"
                value={filters.dueBefore || ''}
                disabled={!!filters.overdueOnly}
                onChange={(e) => onFilterChange({
                  ...filters,
                  dueBefore: e.target.value || null,
                })}
              />
            </div>

            {/* Quick toggles */}
            <div className="filter-bar__field filter-bar__field--toggles">
              <label className="filter-bar__label">Quick filters</label>
              <div className="filter-bar__toggles">
                {/* Overdue button */}
                <Button
                  name={`filter-bar__toggle-btn ${filters.overdueOnly ? 'filter-bar__toggle-btn--active-red' : ''}`}
                  onClick={() => onFilterChange({
                    ...filters,
                    overdueOnly: filters.overdueOnly ? null : true,
                    // Clear manual date range when overdue is toggled on
                    dueAfter:  filters.overdueOnly ? filters.dueAfter  : null,
                    dueBefore: filters.overdueOnly ? filters.dueBefore : null,
                    // Overdue means non-completed, so clear status filter
                    status:    filters.overdueOnly ? filters.status : null,
                  })}
                  title="Show tasks where due date is before today and status is not COMPLETED"
                >
                  🔴 Overdue
                </Button>

                {/* My Tasks button */}
                <Button
                  name={`filter-bar__toggle-btn ${filters.myTasksOnly ? 'filter-bar__toggle-btn--active-blue' : ''}`}
                  onClick={() => onFilterChange({
                    ...filters,
                    myTasksOnly: filters.myTasksOnly ? null : true,
                    // Clear createdBy search when My Tasks is toggled on
                    createdBy: filters.myTasksOnly ? filters.createdBy : null,
                  })}
                  title="Show only your own tasks (tasks you created)"
                >
                  👤 My Tasks
                </Button>
              </div>
            </div>
          </div>

          {/* Row 3: Sort controls */}
          <div className="filter-bar__row filter-bar__row--sort">
            <div className="filter-bar__field">
              <label className="filter-bar__label">Sort by</label>
              <select
                className="filter-bar__select"
                value={filters.sortBy || ''}
                onChange={(e) => onFilterChange({
                  ...filters,
                  sortBy: e.target.value || null,
                })}
              >
                <option value="">Default (newest first)</option>
                <option value="dueDate">Due Date</option>
                <option value="priority">Priority</option>
                <option value="status">Status</option>
                <option value="title">Title</option>
                <option value="createdBy">Created By</option>
                <option value="createdAt">Created At</option>
              </select>
            </div>

            <div className="filter-bar__field">
              <label className="filter-bar__label">Direction</label>
              <select
                className="filter-bar__select"
                value={filters.sortDir || 'desc'}
                onChange={(e) => onFilterChange({
                  ...filters,
                  sortDir: e.target.value,
                })}
              >
                <option value="desc">Descending ↓</option>
                <option value="asc">Ascending ↑</option>
              </select>
            </div>

            {/* Spacer so sort sits left-aligned */}
            <div className="filter-bar__field filter-bar__field--grow" />
          </div>
        </div>
      )}
    </div>
  );
}
