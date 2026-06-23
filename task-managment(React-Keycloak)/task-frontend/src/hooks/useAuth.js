import { useKeycloak } from '@react-keycloak/web';

/**
 * useAuth — thin wrapper around @react-keycloak/web.
 *
 * Returns:
 *   initialized   — true once Keycloak has completed check-sso
 *   authenticated — true if the user is logged in
 *   username      — preferred_username from JWT
 *   roles         — realm roles array (e.g. ["ADMIN", "offline_access"])
 *   isAdmin       — true if roles includes "ADMIN"
 *   isUser        — true if roles includes "USER"
 *   token         — current access token string
 *   login()       — redirects to Keycloak login page
 *   logout()      — ends session, redirects to app root
 *   updateToken   — refreshes token if expiring within minValidity seconds
 *                   ALWAYS call before an API request to avoid 401 on expiry
 */
export function useAuth() {
  const { keycloak, initialized } = useKeycloak();

  const roles = keycloak?.tokenParsed?.realm_access?.roles || [];

  return {
    initialized,
    authenticated: keycloak?.authenticated || false,
    username:      keycloak?.tokenParsed?.preferred_username || '',
    roles,
    isAdmin:       roles.includes('ADMIN'),
    isUser:        roles.includes('USER'),
    token:         keycloak?.token || '',

    login:  () => keycloak?.login(),
    logout: () => keycloak?.logout({ redirectUri: window.location.origin }),

    updateToken: (minValidity = 30) =>
      keycloak?.updateToken(minValidity) ?? Promise.resolve(false),
  };
}
