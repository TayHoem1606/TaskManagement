import { createRoot } from 'react-dom/client';
import { ReactKeycloakProvider } from '@react-keycloak/web';
import keycloak from './keycloak';
import App from './App';
import './index.css'

/**
 * Application entry point.
 *
 * WHY NO <StrictMode>:
 *   React 18 StrictMode calls useEffect twice in development mode.
 *   ReactKeycloakProvider calls keycloak.init() inside useEffect.
 *   Double init() throws: "A 'Keycloak' instance can only be initialized once."
 *   Solution: remove StrictMode when using @react-keycloak/web.
 *
 * initOptions:
 *   onLoad: 'check-sso'
 *     Performs a silent SSO check on page load.
 *     If the user is already logged into Keycloak in this browser,
 *     they are automatically authenticated without seeing the login page.
 *     If not logged in, the app renders normally (unauthenticated state)
 *     until a protected route triggers keycloak.login().
 *
 *   pkceMethod: 'S256'
 *     Enables PKCE (Proof Key for Code Exchange) for the Authorization Code Flow.
 *     Required for public clients (SPAs) — prevents authorization code interception.
 *     Our task-frontend Keycloak client has pkce.code.challenge.method = S256.
 *
 *   flow: 'standard'
 *     Uses the Authorization Code Flow (most secure for SPAs).
 *     The deprecated Implicit Flow is NOT used.
 */
const initOptions = {
  onLoad: 'check-sso',
  pkceMethod: 'S256',
  flow: 'standard',
};

createRoot(document.getElementById('root')).render(
  <ReactKeycloakProvider authClient={keycloak} initOptions={initOptions}>
    <App />
  </ReactKeycloakProvider>
);
