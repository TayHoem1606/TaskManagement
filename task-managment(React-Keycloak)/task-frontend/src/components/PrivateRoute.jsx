import { useAuth } from '../hooks/useAuth';

export default function PrivateRoute({ children }) {
  const { initialized, authenticated, login } = useAuth();

  if (!initialized) {
    return (
      <div className="spinner-wrap" style={{ minHeight: 'calc(100vh - 62px)' }}>
        <div className="spinner" />
        <p>Loading authentication…</p>
      </div>
    );
  }

  if (!authenticated) {
    login();
    return (
      <div className="spinner-wrap" style={{ minHeight: 'calc(100vh - 62px)' }}>
        <p>Redirecting to login…</p>
      </div>
    );
  }

  return children;
}
