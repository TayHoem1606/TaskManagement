import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { createTask } from "../services/taskService";
import TaskForm from "../components/TaskForm";
import Button from "../components/Button";

export default function CreateTaskPage() {
  const navigate = useNavigate();
  const { token, updateToken } = useAuth();

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  async function handleSubmit(formData) {
    try {
      setLoading(true);
      setError(null);
      await updateToken(30);
      await createTask(token, formData);
      setSuccess(true);
      setTimeout(() => navigate("/tasks"), 1500);
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  }

  return (
    <div className="page page--narrow">
      <Button name="back-btn" onClick={() => navigate("/tasks")}>
        ← Back to Tasks
      </Button>

      <div className="card card--padded">
        <h2 className="form-page__title">Create New Task</h2>
        <p className="form-page__subtitle">
          Fill in the details below. Status must start as PENDING.
        </p>

        {error && (
          <div className="alert alert--error">
            <strong>Error:</strong> {error}
          </div>
        )}

        {success && (
          <div className="alert alert--success">
            ✅ Task creation event sent! Redirecting to task list…
          </div>
        )}

        {!success && (
          <TaskForm
            onSubmit={handleSubmit}
            submitLabel="Create Task"
            loading={loading}
            initialValues={{ status: "PENDING" }}
            isCreateMode ={true}
          />
        )}
      </div>
    </div>
  );
}
