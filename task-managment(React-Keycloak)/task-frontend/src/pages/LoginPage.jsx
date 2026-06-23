import { useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import './LoginPage.css';
import Button from '../components/Button';
import logo from '../assets/task-manager.png'

export default function LoginPage() {
  const { initialized, authenticated, login } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (initialized && authenticated) {
      navigate('/tasks', { replace: true });
    }
  }, [initialized, authenticated, navigate]);

  if (!initialized) {
    return (
      <div className="login-page">
        <p className="login-card__loading">Loading…</p>
      </div>
    );
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-card__logo"><img className="login-card__logo" src={logo} alt='task manager logo'/></div>
        <h1 className="login-card__title">Task Management</h1>
        <p className="login-card__subtitle">
          Sign in with your account to manage your tasks.
        </p>
        <Button name="login-card__btn" onClick={login} >Login</Button>
      </div>
    </div>
  );
}
