import { useNavigate } from 'react-router-dom';
import './UnauthorizedPage.css';
import Button from '../components/Button';

export default function UnauthorizedPage() {
  const navigate = useNavigate();

  return (
    <div className="unauthorized-page">
      <div className="unauthorized-card">
        <div className="unauthorized-card__icon">🚫</div>
        <h2 className="unauthorized-card__title">Access Denied</h2>
        <p className="unauthorized-card__msg">
          You do not have permission to perform this action.
        </p>
        <p className="unauthorized-card__hint">
          You can only update or delete tasks that you created.
          ADMIN users can modify any task.
        </p>
        <Button name="btn btn--primary" onClick={() => navigate('/tasks')}>
          Back to Tasks
        </Button>
      </div>
    </div>
  );
}
