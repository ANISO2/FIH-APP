import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Attaches the JWT to every request, and on a 401 logs out and bounces to /login.
 * inject() is called at the top because it needs the injection context.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token();

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((err) => {
      if (err.status === 401 && !req.url.includes('/api/auth/login')) {
        // Only tear the session down if the token WE sent is still the token in
        // use. Two reasons:
        //
        // 1. A request sent with no token at all can't prove the session is dead
        //    — there was nothing to reject. Logging out here is meaningless.
        // 2. A 401 answering an OLD token must not wipe a NEW one. The dashboard
        //    fires five calls in parallel (forkJoin in overview.component.ts); if
        //    a slow reply from a previous session lands after a fresh login, the
        //    unconditional logout() below deleted the brand-new token and kicked
        //    the user back to /login — a "login freezes -> back to login" loop
        //    with valid credentials.
        //
        // Comparing the sent token against the current one makes the handler
        // idempotent and immune to that race. A genuine expiry still logs out:
        // the token is unchanged, so the branch is taken.
        if (token && token === auth.token()) {
          auth.logout();
          router.navigate(['/login']);
        }
      }
      return throwError(() => err);
    })
  );
};
