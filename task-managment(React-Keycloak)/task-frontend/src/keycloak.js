import Keycloak from 'keycloak-js';

/**
 * Keycloak singleton — created once outside React to prevent double init().
 * Config comes from Vite env vars (VITE_* prefix), set in .env for local dev
 * and passed as Docker build args for the containerised build.
 */
const keycloak = new Keycloak({
  url:      import.meta.env.VITE_KEYCLOAK_URL       || 'http://localhost:8180',
  realm:    import.meta.env.VITE_KEYCLOAK_REALM     || 'internship-task-realm',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'task-frontend',
});

export default keycloak;
