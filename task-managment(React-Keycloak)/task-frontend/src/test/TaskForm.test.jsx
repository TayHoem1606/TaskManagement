// src/__tests__/components/TaskForm.test.jsx
import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import TaskForm from '../components/TaskForm';

describe('TaskForm — form validation', () => {
  const onSubmit = vi.fn();

  beforeEach(() => vi.clearAllMocks());

  function renderForm(props = {}) {
    render(<TaskForm onSubmit={onSubmit} submitLabel="Submit" {...props} />);
  }

  function fillValidForm() {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const dateStr = tomorrow.toISOString().split('T')[0];

    fireEvent.change(screen.getByPlaceholderText(/enter task title/i), {
      target: { value: 'Valid Title' },
    });
    fireEvent.change(screen.getByPlaceholderText(/enter task description/i), {
      target: { value: 'Valid description' },
    });
    fireEvent.change(screen.getByDisplayValue('PENDING') || screen.getAllByRole('combobox')[0], {
      target: { value: 'PENDING' },
    });
    fireEvent.change(screen.getByDisplayValue('MEDIUM') || screen.getAllByRole('combobox')[1], {
      target: { value: 'MEDIUM' },
    });
    // Date input
    const dateInput = screen.getByDisplayValue('') || document.querySelector('input[type="date"]');
    if (dateInput) fireEvent.change(dateInput, { target: { value: dateStr } });
  }

  it('renders all required form fields', () => {
    renderForm();
    expect(screen.getByPlaceholderText(/enter task title/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/enter task description/i)).toBeInTheDocument();
    expect(screen.getByText(/status/i)).toBeInTheDocument();
    expect(screen.getByText(/priority/i)).toBeInTheDocument();
    expect(screen.getByText(/due date/i)).toBeInTheDocument();
  });

  it('shows error when title is empty on submit', async () => {
    renderForm();
    fireEvent.click(screen.getByText('Submit'));
    await waitFor(() => {
      expect(screen.getByText(/title is required/i)).toBeInTheDocument();
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('shows error when title exceeds 50 characters', async () => {
    renderForm();
    fireEvent.change(screen.getByPlaceholderText(/enter task title/i), {
      target: { value: 'a'.repeat(51) },
    });
    fireEvent.click(screen.getByText('Submit'));
    await waitFor(() => {
      expect(screen.getByText(/50 characters/i)).toBeInTheDocument();
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('shows error when description is empty on submit', async () => {
    renderForm();
    fireEvent.change(screen.getByPlaceholderText(/enter task title/i), {
      target: { value: 'Valid Title' },
    });
    fireEvent.click(screen.getByText('Submit'));
    await waitFor(() => {
      expect(screen.getByText(/description is required/i)).toBeInTheDocument();
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('shows error when due date is in the past', async () => {
    renderForm();
    fireEvent.change(screen.getByPlaceholderText(/enter task title/i), {
      target: { value: 'Valid Title' },
    });
    fireEvent.change(screen.getByPlaceholderText(/enter task description/i), {
      target: { value: 'Valid description' },
    });

    const dateInputs = document.querySelectorAll('input[type="date"]');
    if (dateInputs.length > 0) {
      fireEvent.change(dateInputs[0], { target: { value: '2020-01-01' } });
    }

    fireEvent.click(screen.getByText('Submit'));
    await waitFor(() => {
      expect(screen.getByText(/cannot be in the past/i)).toBeInTheDocument();
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('shows character counter for title', () => {
    renderForm();
    fireEvent.change(screen.getByPlaceholderText(/enter task title/i), {
      target: { value: 'Hello' },
    });
    expect(screen.getByText('5/50')).toBeInTheDocument();
  });

  it('shows character counter for description', () => {
    renderForm();
    fireEvent.change(screen.getByPlaceholderText(/enter task description/i), {
      target: { value: 'Test desc' },
    });
    expect(screen.getByText('9/100')).toBeInTheDocument();
  });

  it('disables all fields when loading=true', () => {
    renderForm({ loading: true });
    const inputs = screen.getAllByRole('textbox');
    inputs.forEach((input) => {
      expect(input).toBeDisabled();
    });
    expect(screen.getByText('Saving…')).toBeInTheDocument();
  });

  it('pre-populates fields from initialValues', () => {
    renderForm({
      initialValues: {
        title: 'Existing Title',
        description: 'Existing Description',
        status: 'PENDING',
        priority: 'HIGH',
        dueDate: '2027-06-30',
      },
    });

    expect(screen.getByDisplayValue('Existing Title')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Existing Description')).toBeInTheDocument();
    expect(screen.getByDisplayValue('HIGH')).toBeInTheDocument();
  });

  it('in create mode, only shows PENDING status option', () => {
    renderForm({ isCreateMode: true });
    const statusSelect = screen.getAllByRole('combobox')[0];
    const options = Array.from(statusSelect.options).map((o) => o.value);
    expect(options).toContain('PENDING');
    expect(options).not.toContain('IN_PROGRESS');
    expect(options).not.toContain('COMPLETED');
  });
});
