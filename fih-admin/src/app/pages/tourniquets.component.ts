import { Component, effect, signal } from '@angular/core';
import { StatsService } from '../core/stats.service';
import { YearStore } from '../core/year-store.service';
import { TourniquetEvent } from '../core/models';
import { LoadingSkeletonComponent } from '../shared/loading-skeleton.component';
import { EmptyStateComponent } from '../shared/empty-state.component';
import { NumPipe, FDatePipe } from '../shared/format';

/**
 * §5.3 — Statistique des tourniquets. One block per spectacle (event): a header
 * line of totals (Audience, Transactions Billets/Vouchers, Tourniquets) and a
 * table per ticket model with two groups — "Code à barre accessibles"
 * (Billet | Voucher | Audience) and "Transactions tourniquet" (Billet | Voucher).
 */
@Component({
  selector: 'app-tourniquets',
  standalone: true,
  imports: [LoadingSkeletonComponent, EmptyStateComponent, NumPipe, FDatePipe],
  template: `
    <h2 class="text-xl font-bold text-ink mb-1">Statistique des tourniquets</h2>
    <p class="text-sm text-muted mb-5">Codes accessibles et transactions par spectacle et modèle · {{ years.label() }}</p>

    @if (loading()) {
      <app-loading-skeleton [height]="160" />
      <div class="mt-6"><app-loading-skeleton [height]="260" /></div>
    } @else if (error()) {
      <app-empty-state [error]="true" title="Impossible de charger les tourniquets"
                       message="Le serveur est peut-être indisponible." />
    } @else if (events().length === 0) {
      <div class="surface-card p-6"><app-empty-state title="Aucun enregistrement trouvé." message="" /></div>
    } @else {
      @for (e of events(); track e.eventId) {
        <div class="surface-card overflow-hidden mb-6">
          <!-- En-tête événement + totaux -->
          <div class="px-5 py-4 border-b border-line flex flex-wrap items-center gap-x-8 gap-y-2">
            <div class="font-semibold text-ink mr-auto">
              {{ e.eventTitle }}<span class="text-xs text-muted ml-2">{{ e.eventDate | fdate:true }}</span>
            </div>
            <div class="text-sm"><span class="text-muted">Audience</span> <span class="font-semibold text-ink ml-1">{{ e.audience | num }}</span></div>
            <div class="text-sm"><span class="text-muted">Transactions Billets</span> <span class="font-semibold text-ink ml-1">{{ e.transactionsBillets | num }}</span></div>
            <div class="text-sm"><span class="text-muted">Transactions Vouchers</span> <span class="font-semibold text-ink ml-1">{{ e.transactionsVouchers | num }}</span></div>
            <div class="text-sm"><span class="text-muted">Tourniquets</span> <span class="font-bold ml-1" style="color:var(--primary)">{{ e.tourniquets | num }}</span></div>
          </div>

          <div class="table-scroll">
            <table class="w-full text-sm whitespace-nowrap">
              <thead>
                <tr class="grp text-muted">
                  <th class="px-3 py-2 text-left" rowspan="2">Modèle</th>
                  <th class="px-3 py-2 text-center border-l border-line" colspan="3">Code à barre accessibles</th>
                  <th class="px-3 py-2 text-center border-l border-line" colspan="2">Transactions tourniquet</th>
                </tr>
                <tr class="sub text-muted text-xs">
                  <th class="px-3 py-1.5 text-right border-l border-line">Billet</th>
                  <th class="px-3 py-1.5 text-right">Voucher</th>
                  <th class="px-3 py-1.5 text-right">Audience</th>
                  <th class="px-3 py-1.5 text-right border-l border-line">Billet</th>
                  <th class="px-3 py-1.5 text-right">Voucher</th>
                </tr>
              </thead>
              <tbody>
                @for (r of e.rows; track r.modelId) {
                  <tr class="border-b border-line/60 hover:bg-bg transition-colors">
                    <td class="px-3 py-2.5 font-medium text-ink">{{ r.modelName }}</td>
                    <td class="px-3 py-2.5 text-right border-l border-line">{{ r.billetCodes | num }}</td>
                    <td class="px-3 py-2.5 text-right">{{ r.voucherCodes | num }}</td>
                    <td class="px-3 py-2.5 text-right font-semibold">{{ r.audience | num }}</td>
                    <td class="px-3 py-2.5 text-right border-l border-line">{{ r.billetTransactions | num }}</td>
                    <td class="px-3 py-2.5 text-right">{{ r.voucherTransactions | num }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      }
    }
  `
})
export class TourniquetsComponent {
  loading = signal(true);
  error = signal(false);
  events = signal<TourniquetEvent[]>([]);

  constructor(private stats: StatsService, public years: YearStore) {
    effect(() => {
      if (!this.years.ready()) return;
      this.fetch(this.years.year());
    });
  }

  private fetch(year: number | null): void {
    this.loading.set(true);
    this.error.set(false);
    this.stats.tourniquets(year).subscribe({
      next: (e) => { this.events.set(e); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); }
    });
  }
}
