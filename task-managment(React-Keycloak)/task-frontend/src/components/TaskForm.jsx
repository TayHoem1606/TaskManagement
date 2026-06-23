import { useState } from 'react';
import './TaskForm.css';
import Button from './Button';

export default function TaskForm({
  initialValues = {},
  onSubmit,
  submitLabel = 'Submit',
  loading = false,
  isCreateMode = false
}) {
  const [form, setForm] = useState({
    title:       initialValues.title       || '',
    description: initialValues.description || '',
    status:      initialValues.status      || 'PENDING',
    priority:    initialValues.priority    || 'MEDIUM',
    dueDate:     initialValues.dueDate     || '',
  });
  const [errors, setErrors] = useState({});
  const today = new Date().toISOString().split('T')[0];

  const handle = (field) => (e) =>
    setForm((prev) => ({ ...prev, [field]: e.target.value }));

  function validate() {
    const e = {};
    if (!form.title.trim())            e.title = 'Title is required.';
    else if (form.title.length > 50)   e.title = 'Title cannot exceed 50 characters.';

    if (!form.description.trim())           e.description = 'Description is required.';
    else if (form.description.length > 100) e.description = 'Description cannot exceed 100 characters.';

    if (!form.status)   e.status   = 'Status is required.';
    if (!form.priority) e.priority = 'Priority is required.';

    if (!form.dueDate)
      e.dueDate = 'Due date is required.';
    else if (new Date(form.dueDate) < new Date(new Date().toDateString()))
      e.dueDate = 'Due date cannot be in the past.';

    return e;
  }

  function handleSubmit(e) {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) { setErrors(errs); return; }
    setErrors({});
    onSubmit(form);
  }

  return (
    <form className="task-form" onSubmit={handleSubmit}>

      {/* Title */}
      <div className="form-field">
        <label className="form-field__label">Title *</label>
        <input
          className={`form-field__input${errors.title ? ' form-field__input--error' : ''}`}
          value={form.title}
          onChange={handle('title')}
          placeholder="Enter task title (max 50 chars)"
          maxLength={50}
          disabled={loading}
        />
        <span className="form-field__hint">{form.title.length}/50</span>
        {errors.title && <span className="form-field__error">{errors.title}</span>}
      </div>

      {/* Description */}
      <div className="form-field">
        <label className="form-field__label">Description *</label>
        <textarea
          className={`form-field__input form-field__textarea${errors.description ? ' form-field__input--error' : ''}`}
          value={form.description}
          onChange={handle('description')}
          placeholder="Enter task description (max 100 chars)"
          maxLength={100}
          rows={3}
          disabled={loading}
        />
        <span className="form-field__hint">{form.description.length}/100</span>
        {errors.description && <span className="form-field__error">{errors.description}</span>}
      </div>

      {/* Status + Priority */}
      <div className="form-field form-field--row">
        <div className="form-field">
          <label className="form-field__label">Status *</label>
          <select
            className="form-field__input"
            value={form.status}
            onChange={handle('status')}
            disabled={loading}
          >
            <option value="PENDING">PENDING</option>
            {!isCreateMode && ( <>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="COMPLETED">COMPLETED</option>
              </>)}
          </select>
          {errors.status && <span className="form-field__error">{errors.status}</span>}
        </div>

        <div className="form-field">
          <label className="form-field__label">Priority *</label>
          <select
            className="form-field__input"
            value={form.priority}
            onChange={handle('priority')}
            disabled={loading}
          >
            <option value="LOW">LOW</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="HIGH">HIGH</option>
          </select>
          {errors.priority && <span className="form-field__error">{errors.priority}</span>}
        </div>
      </div>

      {/* Due Date */}
      <div className="form-field">
        <label className="form-field__label">Due Date *</label>
        <input
          type="date"
          className={`form-field__input${errors.dueDate ? ' form-field__input--error' : ''}`}
          value={form.dueDate}
          onChange={handle('dueDate')}
          disabled={loading}
          min={today}
        />
        {errors.dueDate && <span className="form-field__error">{errors.dueDate}</span>}
      </div>

      <Button type="submit" name="form-submit-btn" disabled={loading}>
        {loading ? 'Saving…' : submitLabel}
      </Button>
    </form>
  );
}
