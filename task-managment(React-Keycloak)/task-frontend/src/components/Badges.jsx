const STATUS_CLASS = {
  PENDING: "badge--pending",
  IN_PROGRESS: "badge--in-progress",
  COMPLETED: "badge--completed",
};

const STATUS_LABEL = {
  PENDING: "Pending",
  IN_PROGRESS: "In Progress",
  COMPLETED: "Completed",
};

const PRIORITY_CLASS = {
  LOW: "badge--low",
  MEDIUM: "badge--medium",
  HIGH: "badge--high",
};

export function StatusBadge({ status }) {
  const cls = STATUS_CLASS[status] || "badge--pending";
  const label = STATUS_LABEL[status] || status;
  return <span className={`badge ${cls}`}>{label}</span>;
}

export function PriorityBadge({ priority }) {
  const cls = PRIORITY_CLASS[priority] || "badge--medium";
  return <span className={`badge ${cls}`}>{priority}</span>;
}
