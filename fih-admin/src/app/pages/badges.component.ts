import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BadgeService } from '../core/badge.service';
import { Availability, MissingPoster } from '../core/models';
import { LoadingSkeletonComponent } from '../shared/loading-skeleton.component';
import { EmptyStateComponent } from '../shared/empty-state.component';
import { NumPipe, GDatePipe } from '../shared/format';

interface EventGroup {
  eventId: number; eventTitle: string; eventDate: string; hasPoster: boolean; rows: Availability[];
}

@Component({
  selector: 'app-badges',
  standalone: true,
  imports: [FormsModule, LoadingSkeletonComponent, EmptyStateComponent, NumPipe, GDatePipe],
  template: `
    <h2 class="text-xl font-bold text-ink mb-4">Invitations &amp; Badges</h2>

    @if (loading()) {
      <app-loading-skeleton [height]="360" />
    } @else if (error()) {
      <app-empty-state [error]="true" title="Impossible de charger la disponibilité" message="Le serveur est peut-être indisponible." />
    } @else {
      <!-- Bandeau récapitulatif -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <div class="surface-card p-5">
          <div class="text-sm text-muted">Total injectés</div>
          <div class="text-3xl font-extrabold text-ink">{{ totalInjected() | num }}</div>
          <div class="text-xs text-muted mt-1">enregistrements d'invitation dans la base</div>
        </div>
        <div class="surface-card p-5">
          <div class="text-sm text-muted">Événements avec affiche</div>
          <div class="text-3xl font-extrabold" style="color:var(--success)">{{ eventsWithPoster() | num }}</div>
          <div class="text-xs text-muted mt-1">une affiche par événement dans le dossier posters</div>
        </div>
        <div class="surface-card p-5">
          <div class="text-sm text-muted">Affiches manquantes</div>
          <div class="text-3xl font-extrabold" style="color:var(--warn)">{{ missingPosters().length | num }}</div>
          <div class="text-xs text-muted mt-1">événements sans fichier affiche</div>
        </div>
      </div>

      <!-- §6 — liste des affiches manquantes -->
      @if (missingPosters().length > 0) {
        <div class="surface-card p-4 mb-6" style="background:#fbeae0">
          <div class="flex items-center gap-2 mb-2" style="color:var(--warn)">
            <span class="msr">image_not_supported</span>
            <span class="font-semibold">Affiches manquantes ({{ missingPosters().length }})</span>
          </div>
          <div class="flex flex-wrap gap-2">
            @for (m of missingPosters(); track m.eventId) {
              <span class="text-xs px-2 py-1 rounded-lg bg-white border border-line text-ink">
                {{ m.eventTitle }} · <span class="text-muted">{{ m.invitationCount | num }} inv. · attendu {{ m.eventId }}.jpg</span>
              </span>
            }
          </div>
        </div>
      }

      <!-- Contrôles -->
      <div class="flex flex-wrap items-center gap-3 mb-4">
        <select [(ngModel)]="selectedEvent" (ngModelChange)="onEventChange()"
                class="px-3 py-2.5 rounded-xl border border-line bg-white outline-none focus:border-accent">
          <option [ngValue]="null">Tous les événements</option>
          @for (e of eventOptions(); track e.id) { <option [ngValue]="e.id">{{ e.title }}</option> }
        </select>
      </div>

      @if (groups().length === 0) {
        <app-empty-state icon="inbox" title="Aucun enregistrement trouvé" message="Rien ne correspond à ce filtre." />
      } @else {
        @for (g of groups(); track g.eventId) {
          <div class="surface-card mb-4 overflow-hidden">
            <div class="px-5 py-3 border-b border-line flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="font-semibold text-ink">{{ g.eventTitle }}</div>
                @if (g.hasPoster) {
                  <span class="inline-flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded-full"
                        style="background:rgba(10,124,74,.12);color:var(--success)">
                    <span class="msr text-[15px]">check_circle</span> Affiche
                  </span>
                } @else {
                  <span class="inline-flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded-full"
                        style="background:#fbeae0;color:var(--warn)">
                    <span class="msr text-[15px]">image_not_supported</span> Affiche manquante
                  </span>
                }
              </div>
              <div class="text-sm text-muted">{{ g.eventDate | gdate }}</div>
            </div>
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="text-left text-muted border-b border-line">
                    <th class="px-5 py-2.5">Modèle</th>
                    <th class="px-5 py-2.5">Zones d'accès</th>
                    <th class="px-5 py-2.5 text-right">Injectés</th>
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
                      <td class="px-5 py-3 text-right">
                        <button (click)="open(r)"
                                class="px-3 py-1.5 rounded-lg text-white text-sm font-medium transition-opacity hover:opacity-90"
                                style="background:var(--primary)">Gérer</button>
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
  missingPosters = signal<MissingPoster[]>([]);
  selectedEvent: number | null = null;
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
      if (!g) {
        g = { eventId: r.eventId, eventTitle: r.eventTitle, eventDate: r.eventDate, hasPoster: r.eventHasPoster, rows: [] };
        map.set(r.eventId, g);
      }
      g.rows.push(r);
    }
    return [...map.values()];
  });

  totalInjected = computed(() => this.rows().reduce((s, r) => s + r.injectedCount, 0));
  eventsWithPoster = computed(() => {
    const seen = new Map<number, boolean>();
    for (const r of this.rows()) if (!seen.has(r.eventId)) seen.set(r.eventId, r.eventHasPoster);
    return [...seen.values()].filter(Boolean).length;
  });

  constructor(private badges: BadgeService, private router: Router) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.badges.availability().subscribe({
      // Le type « FIH » général n'est rattaché à aucun spectacle daté : sa date
      // est stockée comme l'époque 1970-01-01. On l'affiche comme les autres
      // (libellé « Général » à la place de la date, via le pipe gdate).
      next: (r) => { this.rows.set(r); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); }
    });
    this.badges.missingPosters().subscribe({
      next: (m) => this.missingPosters.set(m),
      error: () => this.missingPosters.set([])
    });
  }

  onEventChange(): void { this.filterEvent.set(this.selectedEvent); }
  zoneColor(z: string): string {
    const v = z.toLowerCase();
    if (v.startsWith('vip') || v === 'v') return 'var(--warn)';
    if (v.startsWith('press') || v === 'r') return '#5b3aa6';
    return 'var(--success)';
  }
  open(r: Availability): void { this.router.navigate(['badges', r.eventId, r.modelId]); }
}
