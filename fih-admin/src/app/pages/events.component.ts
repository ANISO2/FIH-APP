import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { StatsService } from '../core/stats.service';
import { EventRollup } from '../core/models';
import { LoadingSkeletonComponent } from '../shared/loading-skeleton.component';
import { EmptyStateComponent } from '../shared/empty-state.component';
import { NumPipe, PctPipe, GDatePipe } from '../shared/format';

type SortKey = 'title' | 'date' | 'scans' | 'accepted' | 'rejected' | 'acceptanceRate';

@Component({
  selector: 'app-events',
  standalone: true,
  imports: [FormsModule, LoadingSkeletonComponent, EmptyStateComponent, NumPipe, PctPipe, GDatePipe],
  template: `
    <div class="flex items-center justify-between gap-3 mb-4 flex-wrap">
      <h2 class="text-xl font-bold text-ink">Événements</h2>
      <div class="relative">
        <span class="msr absolute left-3 top-1/2 -translate-y-1/2 text-muted text-[20px]">search</span>
        <input [(ngModel)]="query" placeholder="Filtrer par titre…"
               class="pl-10 pr-3 py-2.5 rounded-xl border border-line bg-white w-64 max-w-full focus:border-accent outline-none transition-colors" />
      </div>
    </div>

    @if (loading()) {
      <app-loading-skeleton [height]="380" />
    } @else if (error()) {
      <app-empty-state [error]="true" title="Impossible de charger les événements" message="Le serveur est peut-être indisponible." />
    } @else if (filtered().length === 0) {
      <app-empty-state icon="search_off" title="Aucun enregistrement trouvé" message="Essayez un autre terme de recherche." />
    } @else {
      <div class="surface-card overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="text-left text-muted border-b border-line">
                <th class="px-4 py-3 cursor-pointer select-none" (click)="sortBy('title')">Événement {{ caret('title') }}</th>
                <th class="px-4 py-3 cursor-pointer select-none" (click)="sortBy('date')">Date {{ caret('date') }}</th>
                <th class="px-4 py-3 cursor-pointer select-none text-right" (click)="sortBy('scans')">Entrées {{ caret('scans') }}</th>
                <th class="px-4 py-3 cursor-pointer select-none text-right" (click)="sortBy('accepted')">Acceptés {{ caret('accepted') }}</th>
                <th class="px-4 py-3 cursor-pointer select-none text-right" (click)="sortBy('rejected')">Refusés {{ caret('rejected') }}</th>
                <th class="px-4 py-3 cursor-pointer select-none" (click)="sortBy('acceptanceRate')">Acceptation {{ caret('acceptanceRate') }}</th>
                <th class="px-4 py-3 text-right">Public / VIP</th>
              </tr>
            </thead>
            <tbody>
              @for (e of filtered(); track e.eventId) {
                <tr class="border-b border-line/60 hover:bg-bg cursor-pointer transition-colors" (click)="open(e)">
                  <td class="px-4 py-3 font-medium text-ink">{{ e.title }}</td>
                  <td class="px-4 py-3 text-muted">{{ e.date | gdate }}</td>
                  <td class="px-4 py-3 text-right font-semibold">{{ e.scans | num }}</td>
                  <td class="px-4 py-3 text-right" style="color:var(--success)">{{ e.accepted | num }}</td>
                  <td class="px-4 py-3 text-right" style="color:var(--warn)">{{ e.rejected | num }}</td>
                  <td class="px-4 py-3">
                    <div class="flex items-center gap-2">
                      <div class="h-2 w-24 rounded-full bg-line overflow-hidden">
                        <div class="h-full rounded-full" [style.width.%]="e.acceptanceRate"
                             [style.background]="e.acceptanceRate >= 90 ? 'var(--success)' : 'var(--warn)'"></div>
                      </div>
                      <span class="text-xs font-medium text-muted">{{ e.acceptanceRate | pct }}</span>
                    </div>
                  </td>
                  <td class="px-4 py-3 text-right text-muted">{{ e.publicScans | num }} / {{ e.vipScans | num }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    }
  `
})
export class EventsComponent implements OnInit {
  loading = signal(true);
  error = signal(false);
  rows = signal<EventRollup[]>([]);
  query = '';
  private _query = signal('');
  sortKey = signal<SortKey>('date');
  sortAsc = signal(true);

  // garde le signal synchronisé avec ngModel
  ngDoCheck(): void { if (this._query() !== this.query) this._query.set(this.query); }

  filtered = computed(() => {
    const q = this._query().trim().toLowerCase();
    let list = this.rows().filter(e => !q || e.title.toLowerCase().includes(q));
    const key = this.sortKey();
    const dir = this.sortAsc() ? 1 : -1;
    list = [...list].sort((a, b) => {
      const av = a[key]; const bv = b[key];
      if (typeof av === 'string' && typeof bv === 'string') return av.localeCompare(bv, 'fr') * dir;
      return ((av as number) - (bv as number)) * dir;
    });
    return list;
  });

  constructor(private stats: StatsService, private router: Router) {}

  ngOnInit(): void { this.fetch(); }

  private reqId = 0;
  private fetch(): void {
    const seq = ++this.reqId;       // 3.4 : ignore les réponses obsolètes
    this.loading.set(true);
    this.error.set(false);
    this.stats.events().subscribe({
      next: (r) => { if (seq !== this.reqId) return; this.rows.set(r); this.loading.set(false); },
      error: () => { if (seq !== this.reqId) return; this.error.set(true); this.loading.set(false); }
    });
  }

  sortBy(key: SortKey): void {
    if (this.sortKey() === key) this.sortAsc.update(v => !v);
    else { this.sortKey.set(key); this.sortAsc.set(true); }
  }
  caret(key: SortKey): string { return this.sortKey() === key ? (this.sortAsc() ? '▲' : '▼') : ''; }
  open(e: EventRollup): void { this.router.navigate(['events', e.eventId]); }
}
