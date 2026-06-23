import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.js';
import logo from '../assets/task-manager.png'
import './Navbar.css';
import Button from './Button.jsx';

export default function Navbar() {
  const { initialized, authenticated, username, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  if (!initialized) return null;

  return (
    <nav className="navbar">
      <span
        className="navbar__brand"
        onClick={() => navigate('/tasks')}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => e.key === 'Enter' && navigate('/tasks')}
      >
        <img src={logo} className="navbar__brand-icon" alt='task manager logo'/>
        Task Management
      </span>

      {authenticated && (
        <div className="navbar__right">
          <span className="navbar__username">👤 {username}</span>

          <span className={`navbar__role-badge ${isAdmin ? 'navbar__role-badge--admin' : 'navbar__role-badge--user'}`}>
            {isAdmin ? 'ADMIN' : 'USER'}
          </span>

          <Button name="navbar__logout-btn" onClick={logout}>
            Logout
          </Button>
        </div>
      )}
    </nav>
  );
}
