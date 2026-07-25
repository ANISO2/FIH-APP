import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { LoginResponse } from './models';

const TOKEN_KEY = 'fih_token';
const NAME_KEY = 'fih_name';
const ROLE_KEY = 'fih_role';

// --- JWT expiry helpers (module-level, client-side, best-effort) -------------

/** Decodes a base64url segment to a UTF-8 string. */
function b64UrlDecode(input: string): string {
  let s = input.replace(/-/g, '+').replace(/_/g, '/');
  const pad = s.length % 4;
  if (pad) s += '='.repeat(4 - pad);
  const decoded = atob(s);
  return decodeURIComponent(
    decoded
      .split('')
      .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
      .join('')
  );
}

/** The JWT `exp` in milliseconds, or null if it can't be read. */
function jwtExpMillis(token: string | null): number | null {
  if (!token) return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  try {
    const payload = JSON.parse(b64UrlDecode(parts[1])) as { exp?: number };
    return typeof payload.exp === 'number' ? payload.exp * 1000 : null;
  } catch {
    return null;
  }
}

/** True when there is no token, or its `exp` is readable and in the past. */
function jwtExpired(token: string | null): boolean {
  if (!token) return true;
  const exp = jwtExpMillis(token);
  // Unreadable exp (malformed/legacy) -> let the server decide, don't force logout.
  return exp !== null && Date.now() >= exp;
}

/** Reads the stored token, discarding (and clearing) it if already expired. */
function readStoredToken(): string | null {
  const raw = localStorage.getItem(TOKEN_KEY);
  if (raw && jwtExpired(raw)) {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(NAME_KEY);
    localStorage.removeItem(ROLE_KEY);
    return null;
  }
  return raw;
}

/** Holds the JWT and the logged-in admin's display info. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  // A token left in localStorage from a previous session may be EXPIRED. If we
  // treat it as "logged in", the dashboard loads, fires its /api/stats calls
  // with the dead token, the first 401 logs us out and redirects to /login, and
  // the remaining calls abort — the "freeze -> back to login" loop. So we drop
  // an expired token up front and never consider it valid.
  //
  // This also covers a token that expires DURING a session: isLoggedIn() and
  // token() re-evaluate expiry on every read, so authGuard sends the user back
  // to /login on the next navigation instead of letting a dead token through.
  private readonly _token = signal<string | null>(readStoredToken());
  readonly displayName = signal<string | null>(localStorage.getItem(NAME_KEY));
  readonly role = signal<string | null>(localStorage.getItem(ROLE_KEY));
  readonly isLoggedIn = computed(() => !jwtExpired(this._token()));
  /** Feature 1 — restricted account limited to the Invitations & Badges section. */
  readonly isInvitationsOnly = computed(() => this.role() === 'INVITATIONS');

  constructor(private http: HttpClient) {}

  /** Returns the token only while it is still valid; null once expired. */
  token(): string | null {
    const t = this._token();
    return jwtExpired(t) ? null : t;
  }

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', { username, password }).pipe(
      tap((res) => {
        localStorage.setItem(TOKEN_KEY, res.token);
        localStorage.setItem(NAME_KEY, res.displayName);
        localStorage.setItem(ROLE_KEY, res.role);
        this._token.set(res.token);
        this.displayName.set(res.displayName);
        this.role.set(res.role);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(NAME_KEY);
    localStorage.removeItem(ROLE_KEY);
    this._token.set(null);
    this.displayName.set(null);
    this.role.set(null);
  }
}
