// src/__tests__/components/Navbar.test.jsx
import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import Navbar from '../components/Navbar';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, useNavigate: () => mockNavigate };
});

const mockUseAuth = vi.fn();
vi.mock('../hooks/useAuth', () => ({ useAuth: () => mockUseAuth() }));

describe('Navbar', () => {
  beforeEach(() => vi.clearAllMocks());

  it('renders nothing while Keycloak is not initialized', () => {
    mockUseAuth.mockReturnValue({ initialized: false, authenticated: false });
    const { container } = render(<MemoryRouter><Navbar /></MemoryRouter>);
    expect(container.firstChild).toBeNull();
  });

  it('shows ADMIN badge when user has ADMIN role', () => {
    mockUseAuth.mockReturnValue({
      initialized: true,
      authenticated: true,
      username: 'admin1',
      isAdmin: true,
      logout: vi.fn(),
    });

    render(<MemoryRouter><Navbar /></MemoryRouter>);

    expect(screen.getByText('ADMIN')).toBeInTheDocument();
    expect(screen.queryByText('USER')).not.toBeInTheDocument();
  });

  it('shows USER badge when user has USER role', () => {
    mockUseAuth.mockReturnValue({
      initialized: true,
      authenticated: true,
      username: 'user1',
      isAdmin: false,
      logout: vi.fn(),
    });

    render(<MemoryRouter><Navbar /></MemoryRouter>);

    expect(screen.getByText('USER')).toBeInTheDocument();
    expect(screen.queryByText('ADMIN')).not.toBeInTheDocument();
  });

  it('displays the logged-in username', () => {
    mockUseAuth.mockReturnValue({
      initialized: true,
      authenticated: true,
      username: 'alice',
      isAdmin: false,
      logout: vi.fn(),
    });

    render(<MemoryRouter><Navbar /></MemoryRouter>);
    expect(screen.getByText(/alice/i)).toBeInTheDocument();
  });

  it('calls logout when Logout button is clicked', () => {
    const logout = vi.fn();
    mockUseAuth.mockReturnValue({
      initialized: true,
      authenticated: true,
      username: 'user1',
      isAdmin: false,
      logout,
    });

    render(<MemoryRouter><Navbar /></MemoryRouter>);
    fireEvent.click(screen.getByText('Logout'));
    expect(logout).toHaveBeenCalledOnce();
  });

  it('does not show user info or logout when not authenticated', () => {
    mockUseAuth.mockReturnValue({
      initialized: true,
      authenticated: false,
      username: '',
      isAdmin: false,
      logout: vi.fn(),
    });

    render(<MemoryRouter><Navbar /></MemoryRouter>);

    expect(screen.queryByText('Logout')).not.toBeInTheDocument();
    expect(screen.queryByText('ADMIN')).not.toBeInTheDocument();
    expect(screen.queryByText('USER')).not.toBeInTheDocument();
  });

  it('navigates to /tasks when brand is clicked', () => {
    mockUseAuth.mockReturnValue({
      initialized: true,
      authenticated: true,
      username: 'user1',
      isAdmin: false,
      logout: vi.fn(),
    });

    render(<MemoryRouter><Navbar /></MemoryRouter>);
    fireEvent.click(screen.getByText(/task management/i));
    expect(mockNavigate).toHaveBeenCalledWith('/tasks');
  });
});
