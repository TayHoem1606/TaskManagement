// src/__tests__/pages/TaskDetailPage.test.jsx
import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import TaskDetailPage from '../pages/TaskDetailPage';
import * as taskService from '../services/taskService';

// ── Mock useAuth ──────────────────────────────────────────────────
const mockUseAuth = vi.fn();
vi.mock('../hooks/useAuth', () => ({ useAuth: () => mockUseAuth() }));
vi.mock('../services/taskService');

const TASK_BY_USER1 = {
  id: 'TASK-0001',
  title: 'My Task',
  description: 'Task description',
  status: 'PENDING',
  priority: 'MEDIUM',
  dueDate: '2027-12-31',
  createdAt: '2026-01-01T10:00:00',
  updatedAt: '2026-01-01T10:00:00',
  createdBy: 'user1',
  updatedBy: 'user1',
};

const COMPLETED_TASK = { ...TASK_BY_USER1, status: 'COMPLETED' };

function renderDetailPage(taskId = 'TASK-0001') {
  return render(
    <MemoryRouter initialEntries={[`/tasks/${taskId}`]}>
      <Routes>
        <Route path="/tasks/:id" element={<TaskDetailPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('TaskDetailPage — role and ownership based rendering', () => {
  beforeEach(() => vi.clearAllMocks());

  // ── USER viewing their OWN task ───────────────────────────────

  it('USER sees Edit, Delete, and Advance Status buttons on their own PENDING task', async () => {
    mockUseAuth.mockReturnValue({
      initialized: true, authenticated: true,
      username: 'user1', isAdmin: false,
      token: 'tok', updateToken: vi.fn(() => Promise.resolve()),
    });
    taskService.getTaskById.mockResolvedValue(TASK_BY_USER1);

    renderDetailPage();

    await waitFor(() => {
      expect(screen.getByText('My Task')).toBeInTheDocument();
    });

    expect(screen.getByText(/mark in progress/i)).toBeInTheDocument();
    expect(screen.getByText(/edit task/i)).toBeInTheDocument();
    expect(screen.getByText(/delete task/i)).toBeInTheDocument();
  });

  // ── USER viewing ANOTHER user's task ─────────────────────────

  it("USER does NOT see Edit/Delete buttons on another user's task", async () => {
    mockUseAuth.mockReturnValue({
      initialized: true, authenticated: true,
      username: 'user2',   // different from createdBy: 'user1'
      isAdmin: false,
      token: 'tok', updateToken: vi.fn(() => Promise.resolve()),
    });
    taskService.getTaskById.mockResolvedValue(TASK_BY_USER1);

    renderDetailPage();

    await waitFor(() => {
      expect(screen.getByText('My Task')).toBeInTheDocument();
    });

    expect(screen.queryByText(/edit task/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/delete task/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/mark in progress/i)).not.toBeInTheDocument();
    expect(screen.getByText(/you can only modify tasks you created/i)).toBeInTheDocument();
  });

  // ── ADMIN viewing any task ────────────────────────────────────

  it('ADMIN sees Edit, Delete, and Advance Status on any task', async () => {
    mockUseAuth.mockReturnValue({
      initialized: true, authenticated: true,
      username: 'admin1',
      isAdmin: true,
      token: 'tok', updateToken: vi.fn(() => Promise.resolve()),
    });
    taskService.getTaskById.mockResolvedValue(TASK_BY_USER1);

    renderDetailPage();

    await waitFor(() => {
      expect(screen.getByText('My Task')).toBeInTheDocument();
    });

    expect(screen.getByText(/mark in progress/i)).toBeInTheDocument();
    expect(screen.getByText(/edit task/i)).toBeInTheDocument();
    expect(screen.getByText(/delete task/i)).toBeInTheDocument();
  });

  // ── COMPLETED task — edit and advance hidden ──────────────────

  it('hides Edit and Advance Status buttons for COMPLETED tasks (owner)', async () => {
    mockUseAuth.mockReturnValue({
      initialized: true, authenticated: true,
      username: 'user1', isAdmin: false,
      token: 'tok', updateToken: vi.fn(() => Promise.resolve()),
    });
    taskService.getTaskById.mockResolvedValue(COMPLETED_TASK);

    renderDetailPage();

    await waitFor(() => {
      expect(screen.getByText('My Task')).toBeInTheDocument();
    });

    expect(screen.queryByText(/edit task/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/mark/i)).not.toBeInTheDocument();
    // Delete is still allowed for COMPLETED tasks
    expect(screen.getByText(/delete task/i)).toBeInTheDocument();
  });

  it('hides Edit and Advance Status buttons for COMPLETED tasks (ADMIN)', async () => {
    mockUseAuth.mockReturnValue({
      initialized: true, authenticated: true,
      username: 'admin1', isAdmin: true,
      token: 'tok', updateToken: vi.fn(() => Promise.resolve()),
    });
    taskService.getTaskById.mockResolvedValue(COMPLETED_TASK);

    renderDetailPage();

    await waitFor(() => {
      expect(screen.getByText('My Task')).toBeInTheDocument();
    });

    expect(screen.queryByText(/edit task/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/mark/i)).not.toBeInTheDocument();
    expect(screen.getByText(/delete task/i)).toBeInTheDocument();
  });

  // ── Loading state ─────────────────────────────────────────────

  it('shows loading spinner while fetching task', () => {
    mockUseAuth.mockReturnValue({
      initialized: true, authenticated: true,
      username: 'user1', isAdmin: false,
      token: 'tok', updateToken: vi.fn(() => Promise.resolve()),
    });
    // Return a promise that never resolves to keep loading state
    taskService.getTaskById.mockReturnValue(new Promise(() => {}));

    renderDetailPage();

    expect(screen.getByText(/loading task/i)).toBeInTheDocument();
  });

  // ── Error state ───────────────────────────────────────────────

  it('shows error message when task fetch fails', async () => {
    mockUseAuth.mockReturnValue({
      initialized: true, authenticated: true,
      username: 'user1', isAdmin: false,
      token: 'tok', updateToken: vi.fn(() => Promise.resolve()),
    });
    taskService.getTaskById.mockRejectedValue(new Error('HTTP 404'));

    renderDetailPage();

    await waitFor(() => {
      expect(screen.getByText(/HTTP 404/i)).toBeInTheDocument();
    });
  });
});
