import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { getTaskById, deleteTask, updateTaskStatus } from '../services/taskService';
import { StatusBadge, PriorityBadge } from '../components/Badges';
import './TaskDetailPage.css';
import Button from '../components/Button';

export default function TaskDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { token, username, isAdmin, updateToken } = useAuth();

  const [task,    setTask]    = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState(null);
  const [toast,   setToast]   = useState(null);
  const [busy,    setBusy]    = useState(false);

  // ── Fetch ────────────────────────────────────────────────────
  useEffect(() => {
    async function load() {
      try {
        setLoading(true);
        await updateToken(30);
        const data = await getTaskById(token, id);
        setTask(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id, token]);

  function showToast(msg, type = 'success') {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3500);
  }

  // ── Delete ───────────────────────────────────────────────────
  async function handleDelete() {
    if (!window.confirm(`Delete "${task.title}"? This cannot be undone.`)) return;
    try {
      setBusy(true);
      await updateToken(30);
      await deleteTask(token, task.id);
      showToast('Task deletion event sent. Returning to list…');
      setTimeout(() => navigate('/tasks'), 1500);
    } catch (err) {
      showToast(
        err.message.includes('403') ? 'You can only delete tasks you created.' : err.message,
        'error'
      );
      setBusy(false);
    }
  }

  // ── Advance status ───────────────────────────────────────────
  async function handleAdvanceStatus() {
    const next = task.status === 'PENDING' ? 'IN_PROGRESS' : 'COMPLETED';
    try {
      setBusy(true);
      await updateToken(30);
      await updateTaskStatus(token, task.id, next);
      showToast(`Status → ${next}. Refreshing…`);
      setTimeout(async () => {
        const updated = await getTaskById(token, task.id);
        setTask(updated);
        setBusy(false);
      }, 1500);
    } catch (err) {
      showToast(err.message, 'error');
      setBusy(false);
    }
  }

  const canModify = task && (isAdmin || task.createdBy === username);

  // ── Render ───────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="page page--narrow">
        <div className="spinner-wrap">
          <div className="spinner" />
          <p>Loading task…</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page page--narrow">
        <div className="alert alert--error">{error}</div>
        <Button name="back-btn" onClick={() => navigate('/tasks')}>← Back to Tasks</Button>
      </div>
    );
  }

  if (!task) return null;

  return (
    <div className="page page--narrow">
      {toast && (
        <div className={`toast toast--${toast.type}`}>{toast.msg}</div>
      )}

      <Button name="back-btn" onClick={() => navigate('/tasks')}>← Back to Tasks</Button>

      <div className="card card--padded">
        {/* Title + badges */}
        <div className="task-detail__title-row">
          <h2 className="task-detail__task-title">{task.title}</h2>
          <div className="task-detail__badges">
            <StatusBadge status={task.status} />
            <PriorityBadge priority={task.priority} />
          </div>
        </div>

        {/* Details grid */}
        <div className="task-detail__grid">
          <DetailField label="Task ID">
            <span className="task-detail__field-value task-detail__field-value--mono">
              {task.id}
            </span>
          </DetailField>
          <DetailField label="Due Date">
            <span className="task-detail__field-value">{task.dueDate}</span>
          </DetailField>
          <DetailField label="Created By">
            <span className={`task-detail__field-value${task.createdBy === username ? ' task-detail__field-value--own' : ''}`}>
              {task.createdBy || '—'}{task.createdBy === username ? ' (you)' : ''}
            </span>
          </DetailField>
          <DetailField label="Updated By">
            <span className="task-detail__field-value">{task.updatedBy || '—'}</span>
          </DetailField>
          <DetailField label="Created At">
            <span className="task-detail__field-value">
              {task.createdAt ? task.createdAt.replace('T', ' ').slice(0, 19) : '—'}
            </span>
          </DetailField>
          <DetailField label="Updated At">
            <span className="task-detail__field-value">
              {task.updatedAt ? task.updatedAt.replace('T', ' ').slice(0, 19) : '—'}
            </span>
          </DetailField>
        </div>

        {/* Description */}
        <div className="task-detail__desc-box">
          <p className="task-detail__desc-label">Description</p>
          <p className="task-detail__desc-text">{task.description}</p>
        </div>

        {/* Actions */}
        <div className="task-detail__actions">
          {canModify && task.status !== 'COMPLETED' && (
            <Button
              name="btn btn--outline-purple"
              onClick={handleAdvanceStatus}
              disabled={busy}
            >
              {task.status === 'PENDING' ? '▶ Mark In Progress' : '✓ Mark Completed'}
            </Button>
          )}

          {canModify && task.status !== 'COMPLETED' && (
            <Button
              name="btn btn--outline-green"
              onClick={() => navigate(`/tasks/${task.id}/edit`)}
            >
              ✏ Edit Task
            </Button>
          )}

          {canModify && (
            <Button name="btn btn--outline-red" onClick={handleDelete} disabled={busy}>
              🗑 Delete Task
            </Button>
          )}

          {!canModify && (
            <p className="task-detail__no-action">
              You can only modify tasks you created.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

function DetailField({ label, children }) {
  return (
    <div>
      <p className="task-detail__field-label">{label}</p>
      {children}
    </div>
  );
}
