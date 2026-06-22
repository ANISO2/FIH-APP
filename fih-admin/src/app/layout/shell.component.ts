import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { YearStore } from '../core/year-store.service';
import { Router } from '@angular/router';

interface NavItem { label: string; icon: string; path: string; }

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FormsModule],
  template: `
    <div class="min-h-screen flex">
      <!-- Barre latérale -->
      <aside
        class="fixed lg:sticky lg:top-0 z-40 h-screen w-64 shrink-0 flex flex-col text-white transition-transform duration-200"
        [style.background]="'var(--primary-700)'"
        [class.-translate-x-full]="!open()"
        [class.lg:translate-x-0]="true">
        <div class="px-5 py-5 flex items-center gap-3 border-b border-white/10">
          <span class="msr text-2xl">festival</span>
          <div class="leading-tight">
            <div class="font-bold">FIH</div>
            <div class="text-xs text-white/60">Backoffice</div>
          </div>
        </div>

        <nav class="flex-1 px-3 py-4 space-y-1">
          @for (item of nav; track item.path) {
            <a [routerLink]="item.path" routerLinkActive="bg-white/15 text-white"
               [routerLinkActiveOptions]="{ exact: item.path === '' }"
               (click)="closeOnMobile()"
               class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-white/75 hover:bg-white/10 hover:text-white transition-colors">
              <span class="msr text-[20px]">{{ item.icon }}</span>
              <span class="text-sm font-medium">{{ item.label }}</span>
            </a>
          }
        </nav>

        <div class="px-4 py-4 border-t border-white/10">
          <div class="flex items-center gap-3 mb-3">
            <div class="w-9 h-9 rounded-full bg-white/15 grid place-items-center font-semibold">
              {{ initials() }}
            </div>
            <div class="leading-tight min-w-0">
              <div class="text-sm font-medium truncate">{{ auth.displayName() || 'Admin' }}</div>
              <div class="text-xs text-white/50 truncate">{{ auth.role() }}</div>
            </div>
          </div>
          <button (click)="logout()"
                  class="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-xl bg-white/10 hover:bg-white/20 text-sm font-medium transition-colors">
            <span class="msr text-[18px]">logout</span> Déconnexion
          </button>
        </div>
      </aside>

      <!-- Fond sombre sur mobile -->
      @if (open()) {
        <div class="fixed inset-0 bg-black/30 z-30 lg:hidden" (click)="open.set(false)"></div>
      }

      <!-- Contenu principal -->
      <div class="flex-1 min-w-0 flex flex-col">
        <header class="sticky top-0 z-20 bg-surface border-b border-line shadow-[0_1px_3px_rgba(16,30,54,.08)]">
          <div class="px-4 sm:px-6 lg:px-8 h-16 flex items-center gap-3">
            <button class="lg:hidden p-2 -ml-2 rounded-lg hover:bg-line" (click)="open.set(true)"
                    aria-label="Ouvrir le menu">
              <span class="msr">menu</span>
            </button>
            <h1 class="text-lg font-semibold text-ink">FIH · Statistiques</h1>

            <div class="flex-1"></div>

            <!-- Sélecteur d'année global (3.2) -->
            <label class="flex items-center gap-2 text-sm text-muted">
              <span class="msr text-[20px]">calendar_month</span>
              <select [ngModel]="years.year()" (ngModelChange)="onYearChange($event)"
                      class="px-3 py-2 rounded-xl border border-line bg-white text-ink outline-none focus:border-accent cursor-pointer">
                <option [ngValue]="null">Toutes les années</option>
                @for (y of years.years(); track y) {
                  <option [ngValue]="y">{{ y }}</option>
                }
              </select>
            </label>
          </div>
        </header>
        <main class="flex-1 px-4 sm:px-6 lg:px-8 py-6 max-w-[1400px] w-full mx-auto">
          <router-outlet />
        </main>
      </div>
    </div>
  `
})
export class ShellComponent implements OnInit {
  open = signal(false);
  nav: NavItem[] = [
    { label: 'Vue d\'ensemble', icon: 'dashboard', path: '' },
    { label: 'Événements', icon: 'event', path: 'events' },
    { label: 'Recette', icon: 'payments', path: 'recette' },
    { label: 'Recette par guichet', icon: 'storefront', path: 'recette-guichet' },
    { label: 'Tourniquets', icon: 'meeting_room', path: 'tourniquets' },
    { label: 'Analyse des rejets', icon: 'report', path: 'rejets' },
    { label: 'Badges', icon: 'badge', path: 'badges' },
    { label: 'Portes', icon: 'sensor_door', path: 'gates' }
  ];

  constructor(public auth: AuthService, public years: YearStore, private router: Router) {}

  ngOnInit(): void { this.years.load(); }

  onYearChange(v: number | null): void { this.years.select(v); }

  initials(): string {
    const n = this.auth.displayName() || 'A';
    return n.split(' ').map(p => p[0]).slice(0, 2).join('').toUpperCase();
  }
  closeOnMobile(): void { if (window.innerWidth < 1024) this.open.set(false); }
  logout(): void { this.auth.logout(); this.router.navigate(['/login']); }
}
