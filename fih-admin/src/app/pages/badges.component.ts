import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BadgeService } from '../core/badge.service';
import { Availability } from '../core/models';
import { LoadingSkeletonComponent } from '../shared/loading-skeleton.component';
import { EmptyStateComponent } from '../shared/empty-state.component';
import { NumPipe, FDatePipe } from '../shared/format';

interface EventGroup { eventId: number; eventTitle: string; eventDate: string; rows: Availability[]; }

@Component({
  selector: 'app-badges',
  standalone: true,
  imports: [FormsModule, LoadingSkeletonComponent, EmptyStateComponent, NumPipe, FDatePipe],
  template: `
    <h2 class="text-xl font-bold text-ink mb-4">Invitations &amp; Badges</h2>

    @if (loading()) {
      <app-loading-skeleton [height]="360" />
    } @else if (error()) {
      <app-empty-state [error]="true" title="Impossible de charger la disponibilité" message="Le backend est-il démarré ?" />
    } @else {
      <!-- Bandeau récapitulatif -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <div class="surface-card p-5">
          <div class="text-sm text-muted">Total injectés</div>
          <div class="text-3xl font-extrabold text-ink">{{ totalInjected() | num }}</div>
          <div class="text-xs text-muted mt-1">enregistrements d'invitation dans la base</div>
        </div>
        <div class="surface-card p-5">
          <div class="text-sm text-muted">Avec photo</div>
          <div class="text-3xl font-extrabold" style="color:var(--success)">
            {{ photoChecked() ? (totalWithPhoto() | num) : '—' }}
          </div>
          <div class="text-xs text-muted mt-1">{{ photoChecked() ? 'photos trouvées sur le disque' : 'lancez la vérification' }}</div>
        </div>
        <div class="surface-card p-5">
          <div class="text-sm text-muted">Photo manquante</div>
          <div class="text-3xl font-extrabold" style="color:var(--warn)">
            {{ photoChecked() ? (totalMissing() | num) : '—' }}
          </div>
          <div class="text-xs text-muted mt-1">{{ photoChecked() ? 'fichier requis dans le dossier photos' : '' }}</div>
        </div>
      </div>

      <!-- Contrôles -->
      <div class="flex flex-wrap items-center gap-3 mb-4">
        <select [(ngModel)]="selectedEvent" (ngModelChange)="onEventChange()"
                class="px-3 py-2.5 rounded-xl border border-line bg-white outline-none focus:border-accent">
          <option [ngValue]="null">Tous les événements</option>
          @for (e of eventOptions(); track e.id) { <option [ngValue]="e.id">{{ e.title }}</option> }
        </select>
        <label class="flex items-center gap-2 text-sm text-ink cursor-pointer select-none">
          <input type="checkbox" [(ngModel)]="photoCheckModel" (ngModelChange)="onPhotoToggle($event)" />
          Vérifier la couverture photo (plus lent)
        </label>
      </div>

      @if (groups().length === 0) {
        <app-empty-state icon="inbox" title="Aucun enregistrement trouvé" message="Rien ne correspond à ce filtre." />
      } @else {
        @for (g of groups(); track g.eventId) {
          <div class="surface-card mb-4 overflow-hidden">
            <div class="px-5 py-3 border-b border-line flex items-center justify-between">
              <div class="font-semibold text-ink">{{ g.eventTitle }}</div>
              <div class="text-sm text-muted">{{ g.eventDate | fdate }}</div>
            </div>
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="text-left text-muted border-b border-line">
                    <th class="px-5 py-2.5">Modèle</th>
                    <th class="px-5 py-2.5">Zones d'accès</th>
                    <th class="px-5 py-2.5 text-right">Injectés</th>
                    <th class="px-5 py-2.5">Couverture photo</th>
                    <th class="px-5 py-2.5 text-right">Action</th>
                  </tr>
                </thead>
                <tbody>
                  @for (r of g.rows; track r.modelId) {
                    <tr class="border-b border-line/60 hover:bg-bg transition-colors">
                      <td class="px-5 py-3 font-medium text-ink">{{ r.modelName }}</td>
                      <td class="px-5 py-3">
                        <span class="inline-flex gap-1 flex-wrap">
                          @for (z of r.accessZones; track z) {
                            <span class="text-[11px] font-semibold px-2 py-0.5 rounded-full text-white"
                                  [style.background]="zoneColor(z)">{{ z }}</span>
                          }
                        </span>
                      </td>
                      <td class="px-5 py-3 text-right text-2xl font-extrabold text-primary">{{ r.injectedCount | num }}</td>
                      <td class="px-5 py-3">
                        @if (photoChecked() && r.withPhotoCount !== null) {
                          <div class="flex items-center gap-2">
                            <div class="h-2 w-28 rounded-full bg-line overflow-hidden">
                              <div class="h-full" style="background:var(--success)"
                                   [style.width.%]="coverage(r)"></div>
                            </div>
                            <span class="text-xs text-muted">{{ r.withPhotoCount }}/{{ r.injectedCount }}</span>
                          </div>
                        } @else { <span class="text-xs text-muted">—</span> }
                      </td>
                      <td class="px-5 py-3 text-right">
                        <button (click)="open(r)"
                                class="px-3 py-1.5 rounded-lg text-white text-sm font-medium transition-opacity hover:opacity-90"
                                style="background:var(--primary)">Générer</button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        }
      }
    }
  `
})
export class BadgesComponent implements OnInit {
  loading = signal(true);
  error = signal(false);
  rows = signal<Availability[]>([]);
  selectedEvent: number | null = null;
  photoCheckModel = false;
  photoChecked = signal(false);
  private filterEvent = signal<number | null>(null);

  eventOptions = computed(() => {
    const seen = new Map<number, string>();
    for (const r of this.rows()) if (!seen.has(r.eventId)) seen.set(r.eventId, r.eventTitle);
    return [...seen.entries()].map(([id, title]) => ({ id, title }));
  });

  groups = computed<EventGroup[]>(() => {
    const ev = this.filterEvent();
    const filtered = this.rows().filter(r => ev == null || r.eventId === ev);
    const map = new Map<number, EventGroup>();
    for (const r of filtered) {
      let g = map.get(r.eventId);
      if (!g) { g = { eventId: r.eventId, eventTitle: r.eventTitle, eventDate: r.eventDate, rows: [] }; map.set(r.eventId, g); }
      g.rows.push(r);
    }
    return [...map.values()];
  });

  totalInjected = computed(() => this.rows().reduce((s, r) => s + r.injectedCount, 0));
  totalWithPhoto = computed(() => this.rows().reduce((s, r) => s + (r.withPhotoCount ?? 0), 0));
  totalMissing = computed(() => this.rows().reduce((s, r) => s + (r.missingPhotoCount ?? 0), 0));

  constructor(private badges: BadgeService, private router: Router) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.badges.availability(undefined, this.photoChecked()).subscribe({
      next: (r) => { this.rows.set(r); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); }
    });
  }

  onEventChange(): void { this.filterEvent.set(this.selectedEvent); }
  onPhotoToggle(v: boolean): void { this.photoChecked.set(v); this.load(); }
  coverage(r: Availability): number { return r.injectedCount ? Math.round((r.withPhotoCount ?? 0) * 100 / r.injectedCount) : 0; }
  zoneColor(z: string): string {
    const v = z.toLowerCase();
    if (v.startsWith('vip') || v === 'v') return 'var(--warn)';
    if (v.startsWith('press') || v === 'r') return '#5b3aa6';
    return 'var(--success)';
  }
  open(r: Availability): void { this.router.navigate(['badges', r.eventId, r.modelId]); }
}
