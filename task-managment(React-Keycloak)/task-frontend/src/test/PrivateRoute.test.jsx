// src/__tests__/components/PrivateRoute.test.jsx
import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import PrivateRoute from '../components/PrivateRoute';

// ── Mock useAuth ──────────────────────────────────────────────────
const useKeycloakMock = vi.fn();
 
vi.mock('@react-keycloak/web', () => ({
  useKeycloak: () => useKeycloakMock(),
  ReactKeycloakProvider: ({ children }) => <>{children}</>,
}));
 
const mockLogin = vi.fn();
 
function setupKeycloak({ initialized, authenticated }) {
  useKeycloakMock.mockReturnValue({
    initialized,
    keycloak: {
      authenticated,
      token: authenticated ? 'mock-token' : '',
      tokenParsed: authenticated
        ? { preferred_username: 'user1', realm_access: { roles: ['USER'] } }
        : null,
      login: mockLogin,
      logout: vi.fn(),
      updateToken: vi.fn(() => Promise.resolve(true)),
    },
  });
}
 
describe('PrivateRoute', () => {
  const ProtectedContent = () => <div>Protected Content</div>;
 
  beforeEach(() => {
    vi.clearAllMocks();
  });
 
  it('shows loading spinner while Keycloak is not initialized', () => {
    setupKeycloak({ initialized: false, authenticated: false });
 
    render(
      <MemoryRouter>
        <PrivateRoute><ProtectedContent /></PrivateRoute>
      </MemoryRouter>
    );
 
    expect(screen.getByText(/loading authentication/i)).toBeInTheDocument();
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });
 
  it('calls login() and shows redirect message when not authenticated', () => {
    setupKeycloak({ initialized: true, authenticated: false });
 
    render(
      <MemoryRouter>
        <PrivateRoute><ProtectedContent /></PrivateRoute>
      </MemoryRouter>
    );
 
    expect(mockLogin).toHaveBeenCalledOnce();
    expect(screen.getByText(/redirecting to login/i)).toBeInTheDocument();
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });
 
  it('renders children when authenticated', () => {
    setupKeycloak({ initialized: true, authenticated: true });
 
    render(
      <MemoryRouter>
        <PrivateRoute><ProtectedContent /></PrivateRoute>
      </MemoryRouter>
    );
 
    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });
});
