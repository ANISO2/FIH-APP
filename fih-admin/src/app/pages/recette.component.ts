import { Component, OnInit, computed, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsOption } from 'echarts';
import { StatsService } from '../core/stats.service';
import { RecetteSummary, RecetteEventHeader, RecetteModelRow } from '../core/models';
import { LoadingSkeletonComponent } from '../shared/loading-skeleton.component';
import { EmptyStateComponent } from '../shared/empty-state.component';
import { ChartCardComponent } from '../shared/chart-card.component';
import { SERIES_COLORS } from '../shared/echarts-theme';
import { NumPipe, TndPipe, PctPipe, FDatePipe } from '../shared/format';

type SummaryKey = 'eventTitle' | 'billet' | 'voucher' | 'total';
type ViewMode = 'chart' | 'table';

/** Format a number as TND with fr grouping — used inside ECharts tooltips/labels. */
function tnd(v: number): string {
  return (v || 0).toLocaleString('fr-FR', { maximumFractionDigits: 0 }) + ' TND';
}

/**
 * Recette section.
 *
 * Change A — the "Recette par événement" diagram was removed. The remaining two
 *            diagrams stay; the "Génération · Vente · Reste" bar is now grouped
 *            by event (it reads the per-event détaillée headers, so it needs no
 *            extra request and respects the lazy-loading of the per-model rows).
 * Change B — Recette résumé: Kit column removed; each row carries a thin inline
 *            bar showing its share of total revenue; columns stay sortable.
 * Change C — Recette détaillée: redesigned as collapsible per-event panels
 *            (tourniquet style). The light header list (totals + sell-through)
 *            loads up-front; each event's per-model rows load ON EXPAND. Search,
 *            "Actualiser" (refresh) and "Export CSV" sit in the détaillée toolbar.
 *
 * SCALE: all aggregation is done in SQL (GROUP BY on the tiny, non-growing
 * `generation` table). The détaillée never fetches more than one event's rows
 * at a time, so a single request is always small regardless of ticket volume.
 */
@Component({
  selector: 'app-recette',
  standalone: true,
  imports: [
    NgxEchartsDirective, ChartCardComponent,
    LoadingSkeletonComponent, EmptyStateComponent,
    NumPipe, TndPipe, PctPipe, FDatePipe
  ],
  template: `
    <div class="flex flex-wrap items-start justify-between gap-4 mb-5">
      <div>
        <h2 class="text-xl font-bold text-ink mb-1">Recette</h2>
        <p class="text-sm text-muted">Chiffre d'affaires par événement et par modèle</p>
      </div>
      <div class="flex items-center gap-3">
        <!-- Actualiser : recharge les agrégats en ignorant le cache court du serveur. -->
        <button class="btn-ghost" (click)="actualiser()" [disabled]="loading()" title="Recharger les données">
          <span class="msr text-[18px]" [class.spin]="loading()">refresh</span> Actualiser
        </button>
        <!-- Bascule Tableau / Diagramme : inchangée. -->
        <div class="segmented" role="tablist" aria-label="Mode d'affichage">
          <button role="tab" [attr.aria-selected]="view() === 'chart'" [class.active]="view() === 'chart'"
                  (click)="view.set('chart')">
            <span class="msr text-[18px]">insights</span> Diagramme
          </button>
          <button role="tab" [attr.aria-selected]="view() === 'table'" [class.active]="view() === 'table'"
                  (click)="view.set('table')">
            <span class="msr text-[18px]">table_rows</span> Tableau
          </button>
        </div>
      </div>
    </div>

    @if (loading()) {
      <app-loading-skeleton [height]="220" />
      <div class="mt-6"><app-loading-skeleton [height]="320" /></div>
    } @else if (error()) {
      <app-empty-state [error]="true" title="Impossible de charger la recette" message="Le serveur est peut-être indisponible." />
    } @else if (summary().length === 0 && headers().length === 0) {
      <div class="surface-card p-6"><app-empty-state title="Aucun enregistrement trouvé." message="" /></div>
    } @else if (view() === 'chart') {

      <!-- ============ VUE DIAGRAMME ============ -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-5 mb-5">
        <app-chart-card title="Recette par catégorie" subtitle="Répartition Billet / Voucher (TND)">
          <div echarts theme="fih" [options]="categoryOpt()" class="w-full h-[300px]"></div>
        </app-chart-card>
        <app-chart-card title="Audience" subtitle="Billets vendus vs restants">
          <div echarts theme="fih" [options]="audienceOpt()" class="w-full h-[300px]"></div>
        </app-chart-card>
      </div>
      <app-chart-card title="Génération · Vente · Reste" subtitle="Par événement · longueur de la barre = généré (vendu + reste)">
        <div echarts theme="fih" [options]="gvrOpt()" class="w-full" [style.height.px]="gvrHeight()"></div>
      </app-chart-card>

    } @else {

      <!-- ============ VUE TABLEAU ============ -->
      <!-- Résumé -->
      <div class="surface-card overflow-hidden mb-8">
        <div class="px-5 py-3 border-b border-line font-semibold text-ink">Recette résumé</div>
        @if (summary().length === 0) {
          <div class="p-6"><app-empty-state title="Aucun enregistrement trouvé." message="" /></div>
        } @else {
          <div class="table-scroll table-zebra">
            <table class="text-sm">
              <thead>
                <tr class="single text-left text-muted">
                  <th class="px-4 py-3 cursor-pointer select-none" (click)="sortSummary('eventTitle')">Événement {{ caretS('eventTitle') }}</th>
                  <th class="px-4 py-3 text-right cursor-pointer select-none" (click)="sortSummary('billet')">Billet {{ caretS('billet') }}</th>
                  <th class="px-4 py-3 text-right cursor-pointer select-none" (click)="sortSummary('voucher')">Voucher {{ caretS('voucher') }}</th>
                  <th class="px-4 py-3 text-right cursor-pointer select-none" (click)="sortSummary('total')">Total {{ caretS('total') }}</th>
                </tr>
              </thead>
              <tbody>
                @for (r of summarySorted(); track r.eventId) {
                  <tr class="border-b border-line/60 transition-colors">
                    <td class="px-4 py-3 font-medium text-ink">
                      <div>{{ r.eventTitle }}<span class="text-xs text-muted ml-1">{{ r.eventDate | fdate:true }}</span></div>
                      <!-- Repère visuel : part de cet événement dans la recette totale. -->
                      <div class="share-bar mt-1.5" [style.--w]="sharePct(r.total) + '%'" [title]="(sharePct(r.total) | pct) + ' du total'"></div>
                    </td>
                    <td class="px-4 py-3 text-right tabular">{{ r.billet | tnd }}</td>
                    <td class="px-4 py-3 text-right tabular">{{ r.voucher | tnd }}</td>
                    <td class="px-4 py-3 text-right font-semibold text-ink tabular">{{ r.total | tnd }}</td>
                  </tr>
                }
              </tbody>
              <tfoot>
                <tr class="border-t-2 border-line bg-bg font-bold text-ink">
                  <td class="px-4 py-3">Total</td>
                  <td class="px-4 py-3 text-right tabular">{{ grand().billet | tnd }}</td>
                  <td class="px-4 py-3 text-right tabular">{{ grand().voucher | tnd }}</td>
                  <td class="px-4 py-3 text-right tabular" style="color:var(--primary)">{{ grand().total | tnd }}</td>
                </tr>
              </tfoot>
            </table>
          </div>
        }
      </div>

      <!-- Détaillée -->
      <div class="flex flex-wrap items-center justify-between gap-3 mb-3">
        <div>
          <div class="font-semibold text-ink">Recette détaillée</div>
          <div class="text-xs text-muted">Les invitations affectées sont comptées comme vendues (et non en reste).</div>
        </div>
        <div class="flex items-center gap-2">
          <div class="search-box">
            <span class="msr text-[18px] text-muted">search</span>
            <input type="text" placeholder="Rechercher un événement ou un modèle…"
                   [value]="query()" (input)="onSearch($any($event.target).value)" aria-label="Rechercher" />
            @if (query()) { <button class="msr text-[18px] text-muted" (click)="onSearch('')" aria-label="Effacer">close</button> }
          </div>
          <button class="btn-ghost" (click)="exportCsv()" [disabled]="exporting()" title="Exporter en CSV">
            <span class="msr text-[18px]">download</span> Export CSV
          </button>
        </div>
      </div>

      @if (filteredHeaders().length === 0) {
        <div class="surface-card p-6"><app-empty-state title="Aucun enregistrement trouvé." message="" /></div>
      } @else {
        @for (h of filteredHeaders(); track h.eventId) {
          <div class="surface-card overflow-hidden mb-4">
            <!-- En-tête événement (cliquable) + totaux + taux de vente -->
            <button class="w-full text-left px-5 py-4 flex flex-wrap items-center gap-x-6 gap-y-2 hover:bg-bg transition-colors"
                    (click)="toggle(h.eventId)" [attr.aria-expanded]="isOpen(h.eventId)">
              <span class="msr text-[20px] text-muted transition-transform" [class.rot]="isOpen(h.eventId)">chevron_right</span>
              <span class="font-semibold text-ink mr-auto">
                {{ h.eventTitle }}<span class="text-xs text-muted ml-2">{{ h.eventDate | fdate:true }}</span>
              </span>
              <span class="text-sm"><span class="text-muted">Généré</span> <span class="font-semibold text-ink ml-1 tabular">{{ h.totalGenere | num }}</span></span>
              <span class="text-sm"><span class="text-muted">Vendu</span> <span class="font-semibold text-ink ml-1 tabular">{{ h.totalVendu | num }}</span></span>
              <span class="text-sm"><span class="text-muted">Reste</span> <span class="font-semibold text-ink ml-1 tabular">{{ h.totalReste | num }}</span></span>
              <span class="text-sm"><span class="text-muted">Recette</span> <span class="font-bold ml-1 tabular" style="color:var(--accent)">{{ h.recetteTotale | tnd }}</span></span>
              <span class="flex items-center gap-2 min-w-[140px]">
                <span class="taux-bar"><span [style.width.%]="h.tauxVente"></span></span>
                <span class="text-xs font-semibold" style="color:var(--primary)">{{ h.tauxVente | pct }}</span>
              </span>
            </button>

            @if (isOpen(h.eventId)) {
              @if (isLoadingRows(h.eventId)) {
                <div class="px-5 pb-4"><app-loading-skeleton [height]="120" /></div>
              } @else {
                <div class="table-scroll">
                  <table class="w-full text-sm whitespace-nowrap">
                    <thead>
                      <tr class="grp text-muted">
                        <th class="px-3 py-2 text-left" rowspan="2">Modèle</th>
                        <th class="px-3 py-2 text-right" rowspan="2">Montant</th>
                        <th class="px-3 py-2 text-center border-l border-line" colspan="3">Billet</th>
                        <th class="px-3 py-2 text-center border-l border-line" colspan="3">Voucher</th>
                        <th class="px-3 py-2 text-right border-l border-line" rowspan="2">Total vendu</th>
                        <th class="px-3 py-2 text-right" rowspan="2">Recette · TND</th>
                        <th class="px-3 py-2 text-right" rowspan="2">Taux</th>
                      </tr>
                      <tr class="sub text-muted text-xs">
                        <th class="px-3 py-1.5 text-right border-l border-line">Génération</th>
                        <th class="px-3 py-1.5 text-right">Vente</th>
                        <th class="px-3 py-1.5 text-right">Reste</th>
                        <th class="px-3 py-1.5 text-right border-l border-line">Génération</th>
                        <th class="px-3 py-1.5 text-right">Vente</th>
                        <th class="px-3 py-1.5 text-right">Reste</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (r of visibleRows(h.eventId); track r.modelId) {
                        <tr class="border-b border-line/60 hover:bg-bg transition-colors">
                          <td class="px-3 py-2.5 font-medium text-ink">{{ r.modelName }}</td>
                          <td class="px-3 py-2.5 text-right tabular">{{ r.montant | tnd }}</td>
                          <td class="px-3 py-2.5 text-right border-l border-line tabular">{{ r.billetGeneration | num }}</td>
                          <td class="px-3 py-2.5 text-right tabular">{{ r.billetVente | num }}</td>
                          <td class="px-3 py-2.5 text-right text-muted tabular">{{ r.billetReste | num }}</td>
                          <td class="px-3 py-2.5 text-right border-l border-line tabular">{{ r.voucherGeneration | num }}</td>
                          <td class="px-3 py-2.5 text-right tabular">{{ r.voucherVente | num }}</td>
                          <td class="px-3 py-2.5 text-right text-muted tabular">{{ r.voucherReste | num }}</td>
                          <td class="px-3 py-2.5 text-right border-l border-line font-semibold tabular">{{ r.totalVendu | num }}</td>
                          <td class="px-3 py-2.5 text-right font-semibold text-ink tabular">{{ r.recetteTnd | tnd }}</td>
                          <td class="px-3 py-2.5 text-right tabular" style="color:var(--primary)">{{ r.tauxVente | pct }}</td>
                        </tr>
                      }
                    </tbody>
                    <tfoot>
                      <tr class="border-t-2 border-line bg-bg font-semibold text-ink">
                        <td class="px-3 py-2.5" colspan="8">Sous-total {{ h.eventTitle }}</td>
                        <td class="px-3 py-2.5 text-right tabular">{{ h.totalVendu | num }}</td>
                        <td class="px-3 py-2.5 text-right tabular" style="color:var(--accent)">{{ h.recetteTotale | tnd }}</td>
                        <td class="px-3 py-2.5 text-right tabular">{{ h.tauxVente | pct }}</td>
                      </tr>
                    </tfoot>
                  </table>
                </div>
              }
            }
          </div>
        }

        <!-- Total général (suit le filtre de recherche) -->
        <div class="surface-card px-5 py-4 flex flex-wrap items-center gap-x-6 gap-y-2 bg-bg">
          <span class="font-bold text-ink mr-auto">Total général{{ query() ? ' (filtré)' : '' }}</span>
          <span class="text-sm"><span class="text-muted">Généré</span> <span class="font-semibold text-ink ml-1 tabular">{{ grandDetail().genere | num }}</span></span>
          <span class="text-sm"><span class="text-muted">Vendu</span> <span class="font-semibold text-ink ml-1 tabular">{{ grandDetail().vendu | num }}</span></span>
          <span class="text-sm"><span class="text-muted">Reste</span> <span class="font-semibold text-ink ml-1 tabular">{{ grandDetail().reste | num }}</span></span>
          <span class="text-base font-bold tabular" style="color:var(--primary)">{{ grandDetail().recette | tnd }}</span>
        </div>
      }
    }
  `,
  styles: [`
    .tabular { font-variant-numeric: tabular-nums; }
    .btn-ghost {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 7px 13px; border-radius: 9px; font-size: .875rem; font-weight: 500;
      color: var(--muted); background: var(--surface);
      border: 1px solid var(--line); cursor: pointer;
      transition: color .15s ease, background .15s ease, border-color .15s ease;
    }
    .btn-ghost:hover:not(:disabled) { color: var(--primary); border-color: var(--primary); }
    .btn-ghost:disabled { opacity: .55; cursor: default; }
    .search-box {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 0 10px; height: 38px; min-width: 240px;
      background: var(--surface); border: 1px solid var(--line); border-radius: 9px;
    }
    .search-box input { border: none; outline: none; background: transparent; flex: 1; font-size: .875rem; color: var(--ink); }
    .search-box button { border: none; background: transparent; cursor: pointer; }
    /* Part de la recette totale (résumé) */
    .share-bar { height: 4px; border-radius: 99px; background: var(--line); overflow: hidden; max-width: 220px; }
    .share-bar::after { content: ''; display: block; height: 100%; width: var(--w, 0%); background: var(--primary); }
    /* Taux de vente (détaillée) */
    .taux-bar { flex: 1; height: 6px; border-radius: 99px; background: var(--line); overflow: hidden; display: block; }
    .taux-bar > span { display: block; height: 100%; background: var(--primary); }
    .rot { transform: rotate(90deg); }
    .spin { animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class RecetteComponent implements OnInit {
  loading = signal(true);
  error = signal(false);
  exporting = signal(false);

  summary = signal<RecetteSummary[]>([]);
  headers = signal<RecetteEventHeader[]>([]);          // détaillée: per-event panel headers
  rowsByEvent = signal<Map<number, RecetteModelRow[]>>(new Map());  // détaillée: lazily-loaded rows
  loadingRows = signal<Set<number>>(new Set());
  expanded = signal<Set<number>>(new Set());

  view = signal<ViewMode>('chart');
  query = signal('');

  sortKey = signal<SummaryKey>('total');
  sortAsc = signal(false);

  // Total général (résumé) — calculé côté client, toujours cohérent au tri.
  grand = computed(() => {
    const acc = { billet: 0, voucher: 0, total: 0 };
    for (const r of this.summary()) { acc.billet += r.billet; acc.voucher += r.voucher; acc.total += r.total; }
    return acc;
  });

  // Total général (détaillée) — somme des en-têtes VISIBLES (suit la recherche),
  // calculée à partir des agrégats SQL par événement : toujours cohérent.
  grandDetail = computed(() => {
    const acc = { genere: 0, vendu: 0, reste: 0, recette: 0 };
    for (const h of this.filteredHeaders()) {
      acc.genere += h.totalGenere; acc.vendu += h.totalVendu; acc.reste += h.totalReste; acc.recette += h.recetteTotale;
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

  // Recherche : filtre les panneaux par titre d'événement OU par nom de modèle
  // déjà chargé (un panneau ouvert dont un modèle correspond reste visible).
  filteredHeaders = computed(() => {
    const q = this.norm(this.query());
    if (!q) return this.headers();
    const rows = this.rowsByEvent();
    return this.headers().filter(h =>
      this.norm(h.eventTitle).includes(q) ||
      (rows.get(h.eventId) ?? []).some(r => this.norm(r.modelName).includes(q)));
  });

  // ---- Diagrammes (Change A : « Recette par événement » supprimé) ----

  /** Donut Billet / Voucher à partir des totaux de recette. */
  categoryOpt = computed<EChartsOption>(() => {
    const g = this.grand();
    return {
      tooltip: { trigger: 'item', formatter: (p: any) => `${p.name}<br/><b>${tnd(p.value)}</b> (${p.percent}%)` },
      legend: { bottom: 0, icon: 'circle' },
      series: [{
        type: 'pie', radius: ['45%', '72%'], avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { formatter: (p: any) => tnd(p.value) },
        data: [
          { name: 'Billet', value: g.billet, itemStyle: { color: SERIES_COLORS[0] } },
          { name: 'Voucher', value: g.voucher, itemStyle: { color: SERIES_COLORS[1] } }
        ]
      }]
    };
  });

  /** Donut Audience : total vendu vs total restant (taux de vente global). */
  audienceOpt = computed<EChartsOption>(() => {
    let vendu = 0, reste = 0;
    for (const h of this.headers()) { vendu += h.totalVendu; reste += h.totalReste; }
    const fr = (v: number) => (v || 0).toLocaleString('fr-FR');
    return {
      tooltip: { trigger: 'item', formatter: (p: any) => `${p.name}<br/><b>${fr(p.value)}</b> (${p.percent}%)` },
      legend: { bottom: 0, icon: 'circle' },
      series: [{
        type: 'pie', radius: ['45%', '72%'], avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { formatter: (p: any) => fr(p.value) },
        data: [
          { name: 'Vendu', value: vendu, itemStyle: { color: SERIES_COLORS[1] } },
          { name: 'Reste', value: reste, itemStyle: { color: SERIES_COLORS[2] } }
        ]
      }]
    };
  });

  /**
   * Barres HORIZONTALES, une par événement, triées par quantité générée. Chaque
   * barre empile Vendu + Reste : sa longueur totale = quantité générée. Bien plus
   * lisible que 34 groupes de 3 barres verticales aux libellés tronqués.
   */
  gvrOpt = computed<EChartsOption>(() => {
    const rows = [...this.headers()].sort((a, b) => a.totalGenere - b.totalGenere); // asc -> plus gros en haut
    const fr = (v: number) => (v || 0).toLocaleString('fr-FR');
    return {
      grid: { left: 12, right: 24, top: 10, bottom: 36, containLabel: true },
      tooltip: {
        trigger: 'axis', axisPointer: { type: 'shadow' },
        formatter: (ps: any) => {
          const h = rows[ps[0].dataIndex];
          return `${h.eventTitle}<br/>Généré <b>${fr(h.totalGenere)}</b> · Vendu <b>${fr(h.totalVendu)}</b>`
               + `<br/>Reste <b>${fr(h.totalReste)}</b> · Taux <b>${fr(h.tauxVente)} %</b>`;
        }
      },
      legend: { bottom: 0, icon: 'circle' },
      xAxis: { type: 'value', axisLabel: { formatter: (v: number) => v >= 1000 ? (v / 1000) + 'k' : String(v) } },
      yAxis: { type: 'category', data: rows.map(h => h.eventTitle), axisLabel: { width: 150, overflow: 'truncate' } },
      series: [
        { name: 'Vendu', type: 'bar', stack: 'q', itemStyle: { color: SERIES_COLORS[1] }, barMaxWidth: 15, data: rows.map(h => h.totalVendu) },
        { name: 'Reste', type: 'bar', stack: 'q', itemStyle: { color: SERIES_COLORS[2], borderRadius: [0, 4, 4, 0] }, barMaxWidth: 15, data: rows.map(h => h.totalReste) }
      ]
    };
  });

  /** Hauteur du graphe horizontal : croît avec le nombre d'événements (lisible, sans entassement). */
  gvrHeight = computed(() => Math.max(340, this.headers().length * 26 + 70));

  constructor(private stats: StatsService) {}

  ngOnInit(): void { this.fetch(); }

  // ---- Chargement ----
  private reqId = 0;
  private fetch(refresh = false): void {
    const seq = ++this.reqId;       // 3.4 : ignore les réponses obsolètes
    this.loading.set(true);
    this.error.set(false);
    if (!refresh) {                 // start clean
      this.expanded.set(new Set());
      this.rowsByEvent.set(new Map());
    }
    this.stats.recetteSummary(refresh).subscribe({
      next: (s) => {
        if (seq !== this.reqId) return;
        this.summary.set(s);
        this.stats.recetteDetailHeaders(refresh).subscribe({
          next: (h) => {
            if (seq !== this.reqId) return;
            this.headers.set(h);
            // Recharge les lignes des panneaux restés ouverts (cache vidé au refresh).
            for (const id of this.expanded()) this.loadRows(id);
            this.loading.set(false);
          },
          error: () => { if (seq !== this.reqId) return; this.error.set(true); this.loading.set(false); }
        });
      },
      error: () => { if (seq !== this.reqId) return; this.error.set(true); this.loading.set(false); }
    });
  }

  /** « Actualiser » : recharge en ignorant le cache court du serveur. */
  actualiser(): void {
    this.rowsByEvent.set(new Map());   // force le rechargement des lignes ouvertes
    this.fetch(true);
  }

  // ---- Accordéon (chargement paresseux par événement) ----
  isOpen(id: number): boolean { return this.expanded().has(id); }
  isLoadingRows(id: number): boolean { return this.loadingRows().has(id); }
  visibleRows(id: number): RecetteModelRow[] {
    const rows = this.rowsByEvent().get(id) ?? [];
    const q = this.norm(this.query());
    if (!q) return rows;
    const h = this.headers().find(x => x.eventId === id);
    // Si la recherche correspond au TITRE de l'événement, on montre toutes ses
    // lignes (on cherche l'événement). On ne filtre les lignes par modèle que
    // lorsque l'événement n'est retenu qu'à cause d'un nom de modèle.
    if (h && this.norm(h.eventTitle).includes(q)) return rows;
    return rows.filter(r => this.norm(r.modelName).includes(q));
  }

  toggle(id: number): void {
    const open = new Set(this.expanded());
    if (open.has(id)) { open.delete(id); }
    else { open.add(id); this.loadRows(id); }
    this.expanded.set(open);
  }

  /**
   * Recherche : met à jour le filtre et ouvre (en chargeant leurs lignes) les
   * événements dont le TITRE correspond, pour que le détail s'affiche sans clic
   * supplémentaire. Borné à 8 résultats afin de ne pas ouvrir des dizaines de
   * panneaux d'un coup sur une recherche très large.
   */
  onSearch(value: string): void {
    this.query.set(value);
    const q = this.norm(value);
    if (!q) return;
    const matches = this.headers().filter(h => this.norm(h.eventTitle).includes(q));
    if (matches.length === 0 || matches.length > 8) return;
    const open = new Set(this.expanded());
    for (const h of matches) { open.add(h.eventId); this.loadRows(h.eventId); }
    this.expanded.set(open);
  }

  /** Charge les lignes d'un événement une seule fois (sauf refresh qui vide le cache). */
  private loadRows(id: number): void {
    if (this.rowsByEvent().has(id) || this.loadingRows().has(id)) return;
    const loadingNow = new Set(this.loadingRows()); loadingNow.add(id); this.loadingRows.set(loadingNow);
    this.stats.recetteDetailRows(id).subscribe({
      next: (rows) => {
        const map = new Map(this.rowsByEvent()); map.set(id, rows); this.rowsByEvent.set(map);
        const l = new Set(this.loadingRows()); l.delete(id); this.loadingRows.set(l);
      },
      error: () => { const l = new Set(this.loadingRows()); l.delete(id); this.loadingRows.set(l); }
    });
  }

  // ---- Export CSV (charge d'abord les lignes manquantes des événements filtrés) ----
  exportCsv(): void {
    const events = this.filteredHeaders();
    if (events.length === 0) return;
    this.exporting.set(true);
    const missing = events.filter(h => !this.rowsByEvent().has(h.eventId));
    if (missing.length === 0) { this.buildCsv(events); this.exporting.set(false); return; }
    forkJoin(missing.map(h => this.stats.recetteDetailRows(h.eventId))).subscribe({
      next: (lists) => {
        const map = new Map(this.rowsByEvent());
        missing.forEach((h, i) => map.set(h.eventId, lists[i]));
        this.rowsByEvent.set(map);
        this.buildCsv(events); this.exporting.set(false);
      },
      error: () => this.exporting.set(false)
    });
  }

  private buildCsv(events: RecetteEventHeader[]): void {
    const head = ['Événement', 'Date', 'Modèle', 'Montant (TND)',
      'Billet généré', 'Billet vendu', 'Billet reste',
      'Voucher généré', 'Voucher vendu', 'Voucher reste',
      'Total vendu', 'Recette (TND)', 'Taux de vente (%)'];
    const lines = [head.map(this.csvCell).join(';')];
    const rowsMap = this.rowsByEvent();
    for (const h of events) {
      for (const r of rowsMap.get(h.eventId) ?? []) {
        lines.push([
          h.eventTitle, h.eventDate, r.modelName, r.montant.toFixed(3),
          r.billetGeneration, r.billetVente, r.billetReste,
          r.voucherGeneration, r.voucherVente, r.voucherReste,
          r.totalVendu, r.recetteTnd.toFixed(3), r.tauxVente.toFixed(1)
        ].map(this.csvCell).join(';'));
      }
    }
    const blob = new Blob(['\uFEFF' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'recette-detaillee.csv';
    a.click();
    URL.revokeObjectURL(url);
  }

  private csvCell = (v: unknown): string => {
    const s = String(v ?? '');
    return /[";\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
  };

  // ---- Utilitaires ----
  sharePct(total: number): number {
    const g = this.grand().total;
    return g > 0 ? (total / g) * 100 : 0;
  }
  private norm(s: string): string {
    return (s || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
  }

  sortSummary(key: SummaryKey): void {
    if (this.sortKey() === key) this.sortAsc.update(v => !v);
    else { this.sortKey.set(key); this.sortAsc.set(key === 'eventTitle'); }
  }
  caretS(key: SummaryKey): string { return this.sortKey() === key ? (this.sortAsc() ? '▲' : '▼') : ''; }
}