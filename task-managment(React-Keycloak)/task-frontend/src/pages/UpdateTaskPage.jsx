import { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { getTaskById, updateTask } from '../services/taskService';
import TaskForm from '../components/TaskForm';
import Button from '../components/Button';

export default function UpdateTaskPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { token, username, isAdmin, updateToken, initialized } = useAuth();

  const updateTokenRef = useRef(updateToken);

  useEffect(() =>{
    updateTokenRef.current = updateToken;
  }, [updateToken])

  const [task,    setTask]    = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving,  setSaving]  = useState(false);
  const [error,   setError]   = useState(null);
  const [success, setSuccess] = useState(false);

  // ── Load task ────────────────────────────────────────────────
  useEffect(() => {
    async function load() {

      if (!initialized || !token) return;
      try {
        await updateTokenRef.current(30); 
        const data = await getTaskById(token, id);
        setTask(data);

        // Guard: COMPLETED → redirect away
        if (data.status === 'COMPLETED') {
          navigate(`/tasks/${id}`, { replace: true });
          return;
        }
        if (!isAdmin && data.createdBy !== username) {
          navigate('/unauthorized', { replace: true });
          return;
        }
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id, token, username, isAdmin, navigate]);

  // ── Submit ───────────────────────────────────────────────────
  async function handleSubmit(formData) {
    try {
      setSaving(true);
      setError(null);
      await updateTokenRef.current(30); 

      await updateTask(token, id, formData); 
      
      setSuccess(true);
      setTimeout(() => navigate(`/tasks/${id}`), 1500);
    } catch (err) {
      setError(err.message); 
      setSaving(false);
    }
  }

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

  if (!task && error) {
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
      <Button name="back-btn" onClick={() => navigate(`/tasks/${id}`)}>
        ← Back to Task
      </Button>

      <div className="card card--padded">
        <h2 className="form-page__title">Edit Task</h2>
        <p className="form-page__subtitle">
          Editing: <strong>{task.id}</strong>
          {task.status === 'IN_PROGRESS' && (
            <span className="form-page__subtitle--warn">
              {' '}— Task is IN_PROGRESS. Only status changes are accepted.
            </span>
          )}
        </p>

        {error && (
          <div className="alert alert--error">
            <strong>Error:</strong> {error}
          </div>
        )}

        {success && (
          <div className="alert alert--success">
            ✅ Update event sent! Redirecting…
          </div>
        )}

        {!success && (
          <TaskForm
            initialValues={{
              title:       task.title,
              description: task.description,
              status:      task.status,
              priority:    task.priority,
              dueDate:     task.dueDate,
            }}
            onSubmit={handleSubmit}
            submitLabel="Save Changes"
            loading={saving}
          />
        )}
      </div>
    </div>
  );
}
