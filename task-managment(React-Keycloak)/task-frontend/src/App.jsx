import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import PrivateRoute from './components/PrivateRoute';
import Navbar from './components/Navbar';
import LoginPage from './pages/LoginPage';
import TaskListPage from './pages/TaskListPage';
import TaskDetailPage from './pages/TaskDetailPage';
import CreateTaskPage from './pages/CreateTaskPage';
import UpdateTaskPage from './pages/UpdateTaskPage';
import UnauthorizedPage from './pages/UnauthorizedPage';

/**
 * App — root component.
 *
 * Routing strategy (React Router v6):
 *   /              → redirect to /tasks
 *   /login         → LoginPage (public — shown before authentication)
 *   /unauthorized  → UnauthorizedPage (shown on 403)
 *   /tasks         → TaskListPage       (protected — requires login)
 *   /tasks/new     → CreateTaskPage     (protected — requires login)
 *   /tasks/:id     → TaskDetailPage     (protected — requires login)
 *   /tasks/:id/edit → UpdateTaskPage    (protected — requires login)
 *
 * PrivateRoute wraps every protected route.
 * If the user is not authenticated it calls keycloak.login(),
 * which redirects to the Keycloak login page.
 * After login Keycloak redirects back to the original URL.
 *
 * Navbar is rendered outside <Routes> so it appears on every page.
 * It handles null auth state gracefully (hides itself when not initialized).
 */
export default function App() {
  return (
    <BrowserRouter>
      <Navbar/>
      <Routes>
        <Route path="/login"   element={<LoginPage/>}/>
        <Route path="/unauthorized" element={<UnauthorizedPage/>} />

        <Route path="/tasks"         element={<PrivateRoute><TaskListPage /></PrivateRoute>} />
        <Route path="/tasks/new"     element={<PrivateRoute><CreateTaskPage /></PrivateRoute>} />
        <Route path="/tasks/:id"     element={<PrivateRoute><TaskDetailPage /></PrivateRoute>} />
        <Route path="/tasks/:id/edit" element={<PrivateRoute><UpdateTaskPage /></PrivateRoute>} />

        <Route path="/"  element={<Navigate to="/login" replace />} />
        <Route path="*"  element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
