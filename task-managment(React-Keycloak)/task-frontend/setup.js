// src/__tests__/setup.js
import '@testing-library/jest-dom';

// ── Mock CSS imports (Vitest doesn't process CSS) ────────────────
// Prevents "Unknown file extension .css" errors
vi.mock('../index.css',       () => ({}));
vi.mock('../components/Navbar.css',        () => ({}));
vi.mock('../pages/TaskListPage.css',      () => ({}));
vi.mock('../pages/TaskDetailPage.css',    () => ({}));
vi.mock('../components/TaskForm.css',          () => ({}));
vi.mock('../pages/LoginPage.css',         () => ({}));
vi.mock('../pages/UnauthorizedPages.css',  () => ({}));
vi.mock('../components/Navbar.css',    () => ({}));
vi.mock('../components/TaskForm.css',  () => ({}));
vi.mock('../components/TaskFilterBar.css', () => ({}));
vi.mock('../pages/LoginPage.css',      () => ({}));
vi.mock('../pages/TaskDetailPage.css', () => ({}));
vi.mock('../pages/TaskListPage.css',   () => ({}));
vi.mock('../pages/UnauthorizedPage.css', () => ({}));

// ── Mock image imports (logo PNG) ────────────────────────────────
vi.mock('../assets/task-manager.png', () => ({ default: 'test-logo.png' }));

// ── Mock keycloak.js singleton ───────────────────────────────────
vi.mock('../keycloak.js', () => ({
  default: {
    init: vi.fn(() => Promise.resolve(true)),
    login: vi.fn(),
    logout: vi.fn(),
    updateToken: vi.fn(() => Promise.resolve(true)),
    token: 'mock-token',
    tokenParsed: null,
    authenticated: false,
  },
}));
