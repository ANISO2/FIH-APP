import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { LoginResponse } from './models';

const TOKEN_KEY = 'fih_token';
const NAME_KEY = 'fih_name';
const ROLE_KEY = 'fih_role';

/** Holds the JWT and the logged-in admin's display info. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  readonly displayName = signal<string | null>(localStorage.getItem(NAME_KEY));
  readonly role = signal<string | null>(localStorage.getItem(ROLE_KEY));
  readonly isLoggedIn = computed(() => !!this._token());

  constructor(private http: HttpClient) {}

  token(): string | null { return this._token(); }

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
