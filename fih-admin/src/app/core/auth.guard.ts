import { inject } from '@angular/core';
import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Blocks every page except /login unless a token is present. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isLoggedIn() ? true : router.createUrlTree(['/login']);
};

 
export const invitationsRouteGuard: CanActivateChildFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isInvitationsOnly()) return true;
  const path = route.routeConfig?.path ?? '';
  const allowed = path === 'badges' || path.startsWith('badges/');
  return allowed ? true : router.createUrlTree(['/badges']);
};
