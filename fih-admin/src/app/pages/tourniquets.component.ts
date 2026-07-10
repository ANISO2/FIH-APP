import { Component, effect, signal, OnInit, OnDestroy } from '@angular/core';
import { StatsService } from '../core/stats.service';
import { TourniquetEvent } from '../core/models';
import { LoadingSkeletonComponent } from '../shared/loading-skeleton.component';
import { EmptyStateComponent } from '../shared/empty-state.component';
import { NumPipe, PctPipe, GDatePipe } from '../shared/format';

/**
 * §5.3 — Statistique des tourniquets. One block per spectacle (event): a header
 * line of totals (Audience, Transactions Billets/Vouchers, Tourniquets) and a
 * table per ticket model with two groups — "Code à barre accessibles"
 * (Billet | Voucher | Audience) and "Transactions tourniquet" (Billet | Voucher).
 */
@Component({
  selector: 'app-tourniquets',
  standalone: true,
  imports: [LoadingSkeletonComponent, EmptyStateComponent, NumPipe, PctPipe, GDatePipe],
  template: `
    <div class="flex flex-wrap items-start justify-between gap-4 mb-5">
      <div>
        <h2 class="text-xl font-bold text-ink mb-1">Statistique des tourniquets</h2>
        <p class="text-sm text-muted">Codes accessibles et transactions par spectacle et modèle</p>
      </div>
      <div class="flex items-center gap-2">
        <button class="btn-ghost" [class.on]="auto()" (click)="auto.set(!auto())"
                [title]="auto() ? 'Actualisation auto (30 s) activée' : 'Actualisation auto désactivée'">
          <span class="msr text-[18px]">{{ auto() ? 'sync' : 'sync_disabled' }}</span> Auto
        </button>
        <button class="btn-ghost" (click)="actualiser()" [disabled]="loading()" title="Recharger maintenant">
          <span class="msr text-[18px]" [class.spin]="loading()">refresh</span> Actualiser
        </button>
      </div>
    </div>

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
              {{ e.eventTitle }}<span class="text-xs text-muted ml-2">{{ e.eventDate | gdate:true }}</span>
            </div>
            <div class="text-sm" title="Billets émis / disponibles"><span class="text-muted">Émis</span> <span class="font-semibold text-ink ml-1">{{ e.audience | num }}</span></div>
            <div class="text-sm"><span class="text-muted">Transactions Billets</span> <span class="font-semibold text-ink ml-1">{{ e.transactionsBillets | num }}</span></div>
            <div class="text-sm"><span class="text-muted">Transactions Vouchers</span> <span class="font-semibold text-ink ml-1">{{ e.transactionsVouchers | num }}</span></div>
            <div class="text-sm" title="Entrées réelles aux tourniquets"><span class="text-muted">Entrées</span> <span class="font-bold ml-1" style="color:var(--primary)">{{ e.tourniquets | num }}</span></div>
            <div class="flex items-center gap-2 min-w-[150px]" title="Taux de présence = entrées réelles ÷ billets émis">
              <span class="text-sm text-muted">Présence</span>
              <span class="taux-bar"><span [style.width.%]="presence(e)"></span></span>
              <span class="text-xs font-semibold" style="color:var(--primary)">{{ presence(e) | pct }}</span>
            </div>
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
  `,
  styles: [`
    .btn-ghost {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 7px 13px; border-radius: 9px; font-size: .875rem; font-weight: 500;
      color: var(--muted); background: var(--surface);
      border: 1px solid var(--line); cursor: pointer;
      transition: color .15s ease, border-color .15s ease;
    }
    .btn-ghost:hover:not(:disabled) { color: var(--primary); border-color: var(--primary); }
    .btn-ghost:disabled { opacity: .55; cursor: default; }
    .btn-ghost.on { color: var(--primary); border-color: var(--primary); }
    .taux-bar { flex: 1; height: 6px; border-radius: 99px; background: var(--line); overflow: hidden; display: block; }
    .taux-bar > span { display: block; height: 100%; background: var(--primary); }
    .spin { animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class TourniquetsComponent implements OnInit, OnDestroy {
  loading = signal(true);
  error = signal(false);
  events = signal<TourniquetEvent[]>([]);
  auto = signal(false);                 // 3.3 : OFF par défaut (l'utilisateur active l'auto-actualisation)
  private timer?: ReturnType<typeof setInterval>;

  constructor(private stats: StatsService) {
    // 3.3 — Auto-actualisation OFF par défaut. Le minuteur n'existe QUE pendant
    // que « Auto » est actif : cet effect le démarre quand auto() passe à true
    // et l'arrête COMPLÈTEMENT quand auto() repasse à false. Endpoint mis en
    // cache (refresh=false) ; saute si une requête est en cours ou si l'onglet
    // est masqué.
    effect(() => {
      if (this.auto()) this.startTimer();
      else this.stopTimer();
    });
  }

  ngOnInit(): void { this.fetch(); }

  private startTimer(): void {
    if (this.timer) return;
    this.timer = setInterval(() => {
      if (!this.loading() && (typeof document === 'undefined' || !document.hidden)) {
        this.fetch(false);
      }
    }, 30_000);
  }

  private stopTimer(): void {
    if (this.timer) { clearInterval(this.timer); this.timer = undefined; }
  }

  ngOnDestroy(): void {
    this.stopTimer();
  }

  /** Taux de présence = entrées réelles (tourniquets) ÷ billets émis (audience). */
  presence(e: TourniquetEvent): number {
    return e.audience > 0 ? (e.tourniquets * 100) / e.audience : 0;
  }

  actualiser(): void { this.fetch(true); }

  private reqId = 0;
  private fetch(refresh = false): void {
    const seq = ++this.reqId;       // 3.4 : ignore les réponses obsolètes
    this.loading.set(true);
    this.error.set(false);
    this.stats.tourniquets(refresh).subscribe({
      next: (e) => { if (seq !== this.reqId) return; this.events.set(e); this.loading.set(false); },
      error: () => { if (seq !== this.reqId) return; this.error.set(true); this.loading.set(false); }
    });
  }
}