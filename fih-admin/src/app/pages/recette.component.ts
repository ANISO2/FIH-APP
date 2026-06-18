import { Component, computed, effect, signal } from '@angular/core';
import { StatsService } from '../core/stats.service';
import { YearStore } from '../core/year-store.service';
import { RecetteSummary, RecetteDetail } from '../core/models';
import { LoadingSkeletonComponent } from '../shared/loading-skeleton.component';
import { EmptyStateComponent } from '../shared/empty-state.component';
import { NumPipe, TndPipe, FDatePipe } from '../shared/format';

type SummaryKey = 'eventTitle' | 'billet' | 'voucher' | 'kit' | 'total';

@Component({
  selector: 'app-recette',
  standalone: true,
  imports: [LoadingSkeletonComponent, EmptyStateComponent, NumPipe, TndPipe, FDatePipe],
  template: `
    <h2 class="text-xl font-bold text-ink mb-1">Recette</h2>
    <p class="text-sm text-muted mb-5">Chiffre d'affaires par événement et par modèle · {{ years.label() }}</p>

    @if (loading()) {
      <app-loading-skeleton [height]="220" />
      <div class="mt-6"><app-loading-skeleton [height]="320" /></div>
    } @else if (error()) {
      <app-empty-state [error]="true" title="Impossible de charger la recette" message="Le serveur est peut-être indisponible." />
    } @else {

      <!-- Table 1 — Recette résumé -->
      <div class="surface-card overflow-hidden mb-8">
        <div class="px-5 py-3 border-b border-line font-semibold text-ink">Recette résumé</div>
        @if (summary().length === 0) {
          <div class="p-6"><app-empty-state title="Aucun enregistrement trouvé." message="" /></div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="text-left text-muted border-b border-line">
                  <th class="px-4 py-3 cursor-pointer select-none" (click)="sortSummary('eventTitle')">Événement {{ caretS('eventTitle') }}</th>
                  <th class="px-4 py-3 text-right cursor-pointer select-none" (click)="sortSummary('billet')">Billet {{ caretS('billet') }}</th>
                  <th class="px-4 py-3 text-right cursor-pointer select-none" (click)="sortSummary('voucher')">Voucher {{ caretS('voucher') }}</th>
                  <th class="px-4 py-3 text-right cursor-pointer select-none" (click)="sortSummary('kit')">Kit {{ caretS('kit') }}</th>
                  <th class="px-4 py-3 text-right cursor-pointer select-none" (click)="sortSummary('total')">Total {{ caretS('total') }}</th>
                </tr>
              </thead>
              <tbody>
                @for (r of summarySorted(); track r.eventId) {
                  <tr class="border-b border-line/60 hover:bg-bg transition-colors">
                    <td class="px-4 py-3 font-medium text-ink">
                      {{ r.eventTitle }}
                      <span class="text-xs text-muted ml-1">{{ r.eventDate | fdate:true }}</span>
                    </td>
                    <td class="px-4 py-3 text-right">{{ r.billet | tnd }}</td>
                    <td class="px-4 py-3 text-right">{{ r.voucher | tnd }}</td>
                    <td class="px-4 py-3 text-right">{{ r.kit | tnd }}</td>
                    <td class="px-4 py-3 text-right font-semibold text-ink">{{ r.total | tnd }}</td>
                  </tr>
                }
              </tbody>
              <tfoot>
                <tr class="border-t-2 border-line bg-bg font-bold text-ink">
                  <td class="px-4 py-3">Total</td>
                  <td class="px-4 py-3 text-right">{{ grand().billet | tnd }}</td>
                  <td class="px-4 py-3 text-right">{{ grand().voucher | tnd }}</td>
                  <td class="px-4 py-3 text-right">{{ grand().kit | tnd }}</td>
                  <td class="px-4 py-3 text-right" style="color:var(--primary)">{{ grand().total | tnd }}</td>
                </tr>
              </tfoot>
            </table>
          </div>
        }
      </div>

      <!-- Table 2 — Recette détaillée -->
      <div class="surface-card overflow-hidden">
        <div class="px-5 py-3 border-b border-line font-semibold text-ink">Recette détaillée</div>
        @if (detail().length === 0) {
          <div class="p-6"><app-empty-state title="Aucun enregistrement trouvé." message="" /></div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-sm whitespace-nowrap">
              <thead>
                <tr class="text-muted border-b border-line">
                  <th class="px-3 py-2 text-left" rowspan="2">Événement</th>
                  <th class="px-3 py-2 text-left" rowspan="2">Modèle</th>
                  <th class="px-3 py-2 text-right" rowspan="2">Montant</th>
                  <th class="px-3 py-2 text-center border-l border-line" colspan="3">Voucher</th>
                  <th class="px-3 py-2 text-center border-l border-line" colspan="3">Billet</th>
                  <th class="px-3 py-2 text-center border-l border-line" colspan="3">Kit</th>
                  <th class="px-3 py-2 text-right border-l border-line" rowspan="2">Total</th>
                  <th class="px-3 py-2 text-right" rowspan="2">Recette - TND</th>
                </tr>
                <tr class="text-muted border-b border-line text-xs">
                  <th class="px-3 py-1.5 text-right border-l border-line">Génération</th>
                  <th class="px-3 py-1.5 text-right">Vente</th>
                  <th class="px-3 py-1.5 text-right">Reste</th>
                  <th class="px-3 py-1.5 text-right border-l border-line">Génération</th>
                  <th class="px-3 py-1.5 text-right">Vente</th>
                  <th class="px-3 py-1.5 text-right">Reste</th>
                  <th class="px-3 py-1.5 text-right border-l border-line">Génération</th>
                  <th class="px-3 py-1.5 text-right">Vente</th>
                  <th class="px-3 py-1.5 text-right">Reste</th>
                </tr>
              </thead>
              <tbody>
                @for (r of detail(); track $index) {
                  <tr class="border-b border-line/60 hover:bg-bg transition-colors">
                    <td class="px-3 py-2.5 font-medium text-ink">{{ r.eventTitle }}</td>
                    <td class="px-3 py-2.5">{{ r.modelName }}</td>
                    <td class="px-3 py-2.5 text-right">{{ r.montant | tnd }}</td>
                    <td class="px-3 py-2.5 text-right border-l border-line">{{ r.voucherGeneration | num }}</td>
                    <td class="px-3 py-2.5 text-right">{{ r.voucherVente | num }}</td>
                    <td class="px-3 py-2.5 text-right text-muted">{{ r.voucherReste | num }}</td>
                    <td class="px-3 py-2.5 text-right border-l border-line">{{ r.billetGeneration | num }}</td>
                    <td class="px-3 py-2.5 text-right">{{ r.billetVente | num }}</td>
                    <td class="px-3 py-2.5 text-right text-muted">{{ r.billetReste | num }}</td>
                    <td class="px-3 py-2.5 text-right border-l border-line">{{ r.kitGeneration | num }}</td>
                    <td class="px-3 py-2.5 text-right">{{ r.kitVente | num }}</td>
                    <td class="px-3 py-2.5 text-right text-muted">{{ r.kitReste | num }}</td>
                    <td class="px-3 py-2.5 text-right border-l border-line font-semibold">{{ r.total | num }}</td>
                    <td class="px-3 py-2.5 text-right font-semibold text-ink">{{ r.recetteTnd | tnd }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    }
  `
})
export class RecetteComponent {
  loading = signal(true);
  error = signal(false);
  summary = signal<RecetteSummary[]>([]);
  detail = signal<RecetteDetail[]>([]);

  sortKey = signal<SummaryKey>('total');
  sortAsc = signal(false);

  // Total général calculé côté client : toujours cohérent quel que soit le tri.
  grand = computed(() => {
    const acc = { billet: 0, voucher: 0, kit: 0, total: 0 };
    for (const r of this.summary()) {
      acc.billet += r.billet; acc.voucher += r.voucher; acc.kit += r.kit; acc.total += r.total;
    }
    return acc;
  });

  summarySorted = computed(() => {
    const key = this.sortKey();
    const dir = this.sortAsc() ? 1 : -1;
    return [...this.summary()].sort((a, b) => {
      const av = a[key]; const bv = b[key];
      if (typeof av === 'string' && typeof bv === 'string') return av.localeCompare(bv, 'fr') * dir;
      return ((av as number) - (bv as number)) * dir;
    });
  });

  constructor(private stats: StatsService, public years: YearStore) {
    effect(() => {
      if (!this.years.ready()) return;
      const year = this.years.year();
      this.fetch(year);
    });
  }

  private fetch(year: number | null): void {
    this.loading.set(true);
    this.error.set(false);
    this.stats.recetteSummary(year).subscribe({
      next: (s) => {
        this.summary.set(s);
        this.stats.recetteDetail(year).subscribe({
          next: (d) => { this.detail.set(d); this.loading.set(false); },
          error: () => { this.error.set(true); this.loading.set(false); }
        });
      },
      error: () => { this.error.set(true); this.loading.set(false); }
    });
  }

  sortSummary(key: SummaryKey): void {
    if (this.sortKey() === key) this.sortAsc.update(v => !v);
    else { this.sortKey.set(key); this.sortAsc.set(key === 'eventTitle'); }
  }
  caretS(key: SummaryKey): string { return this.sortKey() === key ? (this.sortAsc() ? '▲' : '▼') : ''; }
}
