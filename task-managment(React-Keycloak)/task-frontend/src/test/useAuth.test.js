// src/__tests__/hooks/useAuth.test.js
import { renderHook } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useAuth } from '../hooks/useAuth';

// ── Mock @react-keycloak/web ──────────────────────────────────────
const mockKeycloak = {
  authenticated: false,
  token: '',
  tokenParsed: null,
  login: vi.fn(),
  logout: vi.fn(),
  updateToken: vi.fn(() => Promise.resolve(true)),
};

vi.mock('@react-keycloak/web', () => ({
  useKeycloak: () => ({
    keycloak: mockKeycloak,
    initialized: true,
  }),
}));

describe('useAuth hook', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockKeycloak.authenticated = false;
    mockKeycloak.token = '';
    mockKeycloak.tokenParsed = null;
  });

  it('returns initialized=true when Keycloak is ready', () => {
    const { result } = renderHook(() => useAuth());
    expect(result.current.initialized).toBe(true);
  });

  it('returns authenticated=false when not logged in', () => {
    const { result } = renderHook(() => useAuth());
    expect(result.current.authenticated).toBe(false);
  });

  it('returns username from tokenParsed preferred_username', () => {
    mockKeycloak.authenticated = true;
    mockKeycloak.tokenParsed = { preferred_username: 'admin1', realm_access: { roles: ['ADMIN'] } };

    const { result } = renderHook(() => useAuth());
    expect(result.current.username).toBe('admin1');
  });

  it('returns isAdmin=true when ADMIN role is present', () => {
    mockKeycloak.authenticated = true;
    mockKeycloak.tokenParsed = {
      preferred_username: 'admin1',
      realm_access: { roles: ['ADMIN', 'offline_access'] },
    };

    const { result } = renderHook(() => useAuth());
    expect(result.current.isAdmin).toBe(true);
    expect(result.current.isUser).toBe(false);
  });

  it('returns isUser=true when USER role is present', () => {
    mockKeycloak.authenticated = true;
    mockKeycloak.tokenParsed = {
      preferred_username: 'user1',
      realm_access: { roles: ['USER', 'offline_access'] },
    };

    const { result } = renderHook(() => useAuth());
    expect(result.current.isAdmin).toBe(false);
    expect(result.current.isUser).toBe(true);
  });

  it('returns empty roles array when tokenParsed is null', () => {
    mockKeycloak.authenticated = false;
    mockKeycloak.tokenParsed = null;

    const { result } = renderHook(() => useAuth());
    expect(result.current.roles).toEqual([]);
    expect(result.current.isAdmin).toBe(false);
    expect(result.current.isUser).toBe(false);
  });

  it('calls keycloak.login() when login() is invoked', () => {
    const { result } = renderHook(() => useAuth());
    result.current.login();
    expect(mockKeycloak.login).toHaveBeenCalledOnce();
  });

  it('calls keycloak.logout() with origin when logout() is invoked', () => {
    const { result } = renderHook(() => useAuth());
    result.current.logout();
    expect(mockKeycloak.logout).toHaveBeenCalledWith({
      redirectUri: window.location.origin,
    });
  });
});
