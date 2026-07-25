import { inject } from '@angular/core';
import { CanActivateChildFn, CanActivateFn, Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Racine de la section « Invitations & Badges » — la SEULE zone autorisée au
 * compte restreint (rôle INVITATIONS). Doit rester aligné avec :
 *   - app.routes.ts        -> 'badges' et 'badges/:eventId/:modelId'
 *   - shell.component.ts   -> visibleNav() filtre sur path === 'badges'
 *   - login.component.ts   -> redirection post-login vers '/badges'
 *   - SecurityConfig.java  -> /api/badges/** et /api/invitations/**
 *                             = hasAnyRole("ADMIN", "INVITATIONS")
 */
const INVITATIONS_HOME = '/badges';

/** Retire la query string et le fragment pour ne comparer que le chemin. */
function pathOf(url: string): string {
  return url.split('?')[0].split('#')[0];
}

/**
 * Garde d'authentification posée sur la route shell ('').
 *
 * Session valide -> accès autorisé. Sinon -> /login.
 *
 * `isLoggedIn()` s'appuie désormais sur l'expiration réelle du JWT (voir
 * auth.service.ts) : un jeton périmé laissé en localStorage n'est plus
 * considéré comme une session ouverte, ce qui évitait le cycle
 * « tableau de bord -> 401 -> retour login ».
 */
export const authGuard: CanActivateFn = (): boolean | UrlTree => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isLoggedIn()) {
    return true;
  }
  return router.createUrlTree(['/login']);
};

/**
 * Garde de rôle posée en `canActivateChild` sur le shell.
 *
 * Le compte restreint (rôle INVITATIONS) ne peut atteindre que
 * « Invitations & Badges ». Toute autre route enfant — y compris la route par
 * défaut '' (Vue d'ensemble), qui appellerait /api/stats/overview et
 * recevrait un 403 non intercepté — est redirigée vers /badges.
 *
 * Les comptes ADMIN ne sont pas concernés : la garde les laisse passer.
 */
export const invitationsRouteGuard: CanActivateChildFn = (
  _childRoute,
  state
): boolean | UrlTree => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isInvitationsOnly()) {
    return true;
  }

  const url = pathOf(state.url);
  const allowed = url === INVITATIONS_HOME || url.startsWith(`${INVITATIONS_HOME}/`);

  return allowed ? true : router.createUrlTree([INVITATIONS_HOME]);
};
