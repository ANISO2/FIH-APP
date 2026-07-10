import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="min-h-screen grid place-items-center p-4"
         style="background: linear-gradient(135deg, #1f5f8b 0%, #0f9d9d 100%);">
      <div class="surface-card w-full max-w-md p-8">
        <div class="flex items-center gap-3 mb-6">
          <span class="msr text-3xl text-primary">festival</span>
          <div>
            <div class="font-bold text-lg text-ink leading-none">FIH</div>
            <div class="text-sm text-muted">Connexion administrateur</div>
          </div>
        </div>

        <form (ngSubmit)="submit()" class="space-y-4">
          <label class="block">
            <span class="text-sm font-medium text-ink">Nom d'utilisateur</span>
            <input name="username" [(ngModel)]="username" required autocomplete="username"
                   class="mt-1 w-full px-3 py-2.5 rounded-xl border border-line bg-white focus:border-accent outline-none transition-colors"
                   placeholder="admin" />
          </label>
          <label class="block">
            <span class="text-sm font-medium text-ink">Mot de passe</span>
            <input name="password" type="password" [(ngModel)]="password" required autocomplete="current-password"
                   class="mt-1 w-full px-3 py-2.5 rounded-xl border border-line bg-white focus:border-accent outline-none transition-colors"
                   placeholder="••••••••" />
          </label>

          @if (error()) {
            <div class="flex items-center gap-2 text-sm rounded-xl px-3 py-2.5"
                 style="background:#fbeae0;color:var(--warn);">
              <span class="msr text-[18px]">error</span> {{ error() }}
            </div>
          }

          <button type="submit" [disabled]="loading() || !username || !password"
                  class="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl text-white font-semibold transition-all disabled:opacity-60"
                  style="background: var(--primary);">
            @if (loading()) {
              <span class="msr animate-spin text-[18px]">progress_activity</span> Connexion en cours…
            } @else { Se connecter }
          </button>
        </form>
      </div>
    </div>
  `
})
export class LoginComponent {
  username = '';
  password = '';
  loading = signal(false);
  error = signal<string | null>(null);

  constructor(private auth: AuthService, private router: Router) {}

  submit(): void {
    if (!this.username || !this.password) return;
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(this.username, this.password).subscribe({
      next: () => {
        this.loading.set(false);
        // Feature 1 — the restricted account lands directly on Invitations & Badges.
        this.router.navigate([this.auth.isInvitationsOnly() ? '/badges' : '']);
      },
      error: (err) => {
        this.loading.set(false);
        // Feature 3 — 401 = bad credentials; anything else (backend down/inactive,
        // server error) = a generic server-side error message.
        this.error.set(err.status === 401
          ? 'Identifiant ou mot de passe invalide.'
          : 'Une erreur s\'est produite dans le serveur');
      }
    });
  }
}
