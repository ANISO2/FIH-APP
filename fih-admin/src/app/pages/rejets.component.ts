import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsOption } from 'echarts';
import { StatsService } from '../core/stats.service';
import { RejetsData, RejetScan } from '../core/models';
import { LoadingSkeletonComponent } from '../shared/loading-skeleton.component';
import { EmptyStateComponent } from '../shared/empty-state.component';
import { ChartCardComponent } from '../shared/chart-card.component';
import { BRAND, SERIES_COLORS } from '../shared/echarts-theme';
import { NumPipe, PctPipe, realDate } from '../shared/format';

/** Map a rejection description to a stable category (mirrors the backend grouping). */
function categorie(desc: string | null): string {
  const d = (desc || '').toLowerCase();
  if (d.includes('utilis')) return 'Déjà utilisé';
  if (d.includes('date')) return 'Date incohérente';
  if (d.includes('porte')) return 'Porte inaccessible';
  return 'Autre';
}
const CAT_COLOR: Record<string, string> = {
  'Déjà utilisé': '#b54708',
  'Date incohérente': '#5b3aa6',
  'Porte inaccessible': '#1f5f8b',
  'Autre': '#5b6470'
};

/**
 * Part C — Analyse des rejets. Read-only study of refused turnstile scans
 * (transactionstate = false).
 *
 * Refonte visuelle : une carte « Synthèse » regroupe le taux de rejet, la
 * répartition par motif et par porte sous forme de barres segmentées + légende
 * (plus lisible et moins encombré que les anciens compteurs + camemberts).
 * Suivent les graphes dans le temps / par modèle / par événement, puis le détail.
 * Aucune logique de données changée ; tout reste annuel et en français.
 */
@Component({
  selector: 'app-rejets',
  standalone: true,
  imports: [
    FormsModule, NgxEchartsDirective, ChartCardComponent,
    LoadingSkeletonComponent, EmptyStateComponent, NumPipe, PctPipe
  ],
  template: `
    <div class="flex flex-wrap items-start justify-between gap-4 mb-5">
      <div>
        <h2 class="text-xl font-bold text-ink mb-1">Analyse des rejets</h2>
        <p class="text-sm text-muted">Transactions refusées aux tourniquets</p>
      </div>
      <button class="btn-ghost" (click)="actualiser()" [disabled]="loading()" title="Recharger les données">
        <span class="msr text-[18px]" [class.spin]="loading()">refresh</span> Actualiser
      </button>
    </div>

    @if (loading()) {
      <app-loading-skeleton [height]="150" />
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-5 mt-5">
        <app-loading-skeleton [height]="300" /><app-loading-skeleton [height]="300" />
      </div>
    } @else if (error()) {
      <app-empty-state [error]="true" title="Impossible de charger l'analyse des rejets"
                       message="Le serveur est peut-être indisponible." />
    } @else if (!data() || data()!.totalScans === 0) {
      <div class="surface-card p-6"><app-empty-state title="Aucun enregistrement trouvé." message="" /></div>
    } @else {

      <!-- ============ SYNTHÈSE ============ -->
      <div class="surface-card p-5 sm:p-6 mb-6">
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 lg:divide-x lg:divide-line">
          <!-- Taux de rejet -->
          <div class="lg:pr-6">
            <div class="kpi-label">Taux de rejet</div>
            <div class="flex items-baseline gap-2 mt-1">
              <span class="text-4xl font-bold leading-none" style="color:var(--warn)">{{ data()!.tauxRejet | pct }}</span>
            </div>
            <div class="text-sm text-muted mt-1.5">{{ data()!.totalRejets | num }} refusés · {{ data()!.totalScans | num }} scans</div>
            <div class="seg mt-4">
              <span [style.width.%]="rejetShare()" style="background:var(--warn)"></span>
              <span [style.width.%]="100 - rejetShare()" style="background:var(--success)"></span>
            </div>
            <div class="flex items-center justify-between text-xs mt-2">
              <span class="flex items-center gap-1.5"><i class="dot" style="background:var(--warn)"></i><span class="text-muted">Rejetés</span> <b class="text-ink">{{ data()!.totalRejets | num }}</b></span>
              <span class="flex items-center gap-1.5"><b class="text-ink">{{ data()!.totalAcceptes | num }}</b> <span class="text-muted">Acceptés</span><i class="dot" style="background:var(--success)"></i></span>
            </div>
          </div>

          <!-- Répartition par motif -->
          <div class="lg:px-6">
            <div class="kpi-label">Motifs de rejet</div>
            <div class="seg mt-3">
              @for (c of catBreakdown(); track c.label) {
                <span [style.width.%]="c.pct" [style.background]="c.color" [title]="c.label"></span>
              }
            </div>
            <ul class="mt-3 space-y-2">
              @for (c of catBreakdown(); track c.label) {
                <li class="flex items-center gap-2 text-sm">
                  <i class="dot" [style.background]="c.color"></i>
                  <span class="text-ink mr-auto">{{ c.label }}</span>
                  <span class="font-semibold text-ink tabular">{{ c.valeur | num }}</span>
                  <span class="text-muted text-xs tabular w-14 text-right">{{ c.pct | pct }}</span>
                </li>
              }
            </ul>
          </div>

          <!-- Répartition par porte -->
          <div class="lg:pl-6">
            <div class="kpi-label">Par porte</div>
            @if (porteBreakdown().length === 0) {
              <p class="text-sm text-muted mt-3">Aucune donnée de porte.</p>
            } @else {
              <div class="seg mt-3">
                @for (p of porteBreakdown(); track p.label) {
                  <span [style.width.%]="p.pct" [style.background]="p.color" [title]="p.label"></span>
                }
              </div>
              <ul class="mt-3 space-y-2">
                @for (p of porteBreakdown(); track p.label) {
                  <li class="flex items-center gap-2 text-sm">
                    <i class="dot" [style.background]="p.color"></i>
                    <span class="text-ink mr-auto">{{ p.label }}</span>
                    <span class="font-semibold text-ink tabular">{{ p.valeur | num }}</span>
                    <span class="text-muted text-xs tabular w-14 text-right">{{ p.pct | pct }}</span>
                  </li>
                }
              </ul>
            }
          </div>
        </div>
      </div>

      <!-- ============ GRAPHES ============ -->
      <app-chart-card title="Rejets dans le temps" subtitle="Par jour — pics d'affluence aux portes" class="block mb-5">
        <div echarts theme="fih" [options]="jourOpt()" class="w-full h-[280px]"></div>
      </app-chart-card>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-5 mb-6">
        <app-chart-card title="Rejets par modèle" subtitle="Top 10 modèles de billet">
          <div echarts theme="fih" [options]="modeleOpt()" class="w-full h-[340px]"></div>
        </app-chart-card>
        <app-chart-card title="Rejets par événement" subtitle="Top 10 soirées avec le plus de refus">
          <div echarts theme="fih" [options]="evenementOpt()" class="w-full h-[340px]"></div>
        </app-chart-card>
      </div>

      <!-- ============ DÉTAIL ============ -->
      <div class="surface-card overflow-hidden">
        <div class="px-5 py-3 border-b border-line flex flex-wrap items-center gap-3">
          <span class="font-semibold text-ink mr-auto">Détail des rejets</span>
          @if (data()!.scansTronques) {
            <span class="chip" style="color:var(--warn); background:rgba(181,71,8,.10)">Liste limitée à 2000 lignes</span>
          }
          <div class="search-box">
            <span class="msr text-[18px] text-muted">search</span>
            <input [ngModel]="search()" (ngModelChange)="search.set($event)" placeholder="Rechercher…"
                   aria-label="Rechercher dans les rejets" />
            @if (search()) { <button class="msr text-[18px] text-muted" (click)="search.set('')" aria-label="Effacer">close</button> }
          </div>
        </div>
        @if (filtered().length === 0) {
          <div class="p-6"><app-empty-state title="Aucun enregistrement trouvé." message="" /></div>
        } @else {
          <div class="table-scroll">
            <table class="w-full text-sm whitespace-nowrap">
              <thead>
                <tr class="single text-left text-muted">
                  <th class="px-4 py-3">Code-barres</th>
                  <th class="px-4 py-3">Événement</th>
                  <th class="px-4 py-3">Porte</th>
                  <th class="px-4 py-3">Date / heure</th>
                  <th class="px-4 py-3">Motif</th>
                </tr>
              </thead>
              <tbody>
                @for (s of filtered(); track $index) {
                  <tr class="border-b border-line/60 hover:bg-bg transition-colors">
                    <td class="px-4 py-3 font-medium text-ink tabular">{{ s.codebarre || '—' }}</td>
                    <td class="px-4 py-3">{{ s.eventTitle || '—' }}</td>
                    <td class="px-4 py-3">
                      <span class="chip" [style.color]="porteColor(s.porte)" [style.background]="porteTint(s.porte)">{{ porteLabel(s.porte) }}</span>
                    </td>
                    <td class="px-4 py-3 text-muted tabular">{{ fmtDateTime(s.dateTime) }}</td>
                    <td class="px-4 py-3">
                      <span class="inline-flex items-center gap-2">
                        <i class="dot" [style.background]="catColor(s.description)"></i>
                        <span class="text-ink">{{ cat(s.description) }}</span>
                      </span>
                      @if (s.description) { <span class="text-muted ml-2">· {{ s.description }}</span> }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .tabular { font-variant-numeric: tabular-nums; }
    .kpi-label { font-size: .72rem; font-weight: 600; letter-spacing: .04em; text-transform: uppercase; color: var(--muted); }
    .seg { display: flex; height: 9px; border-radius: 99px; overflow: hidden; background: var(--line); gap: 1px; }
    .seg > span { display: block; height: 100%; min-width: 2px; }
    .dot { width: 9px; height: 9px; border-radius: 50%; display: inline-block; flex: none; }
    .chip { display: inline-block; padding: 2px 9px; border-radius: 99px; font-size: .75rem; font-weight: 500; }
    .btn-ghost {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 7px 13px; border-radius: 9px; font-size: .875rem; font-weight: 500;
      color: var(--muted); background: var(--surface);
      border: 1px solid var(--line); cursor: pointer;
      transition: color .15s ease, border-color .15s ease;
    }
    .btn-ghost:hover:not(:disabled) { color: var(--primary); border-color: var(--primary); }
    .btn-ghost:disabled { opacity: .55; cursor: default; }
    .search-box {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 0 10px; height: 38px; min-width: 220px;
      background: var(--surface); border: 1px solid var(--line); border-radius: 9px;
    }
    .search-box input { border: none; outline: none; background: transparent; flex: 1; font-size: .875rem; color: var(--ink); }
    .search-box button { border: none; background: transparent; cursor: pointer; }
    .spin { animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class RejetsComponent implements OnInit {
  loading = signal(true);
  error = signal(false);
  data = signal<RejetsData | null>(null);
  search = signal('');

  rejetShare = computed(() => {
    const d = this.data();
    return d && d.totalScans > 0 ? (d.totalRejets * 100) / d.totalScans : 0;
  });

  /** Motifs avec part (%) et couleur, pour la barre segmentée + légende. */
  catBreakdown = computed(() => {
    const rows = this.data()?.parCategorie ?? [];
    const total = rows.reduce((s, r) => s + r.valeur, 0) || 1;
    return rows.map(r => ({ label: r.label, valeur: r.valeur, pct: (r.valeur * 100) / total, color: CAT_COLOR[r.label] ?? BRAND.muted }));
  });

  /** Portes avec part (%) et couleur. */
  porteBreakdown = computed(() => {
    const rows = this.data()?.parPorte ?? [];
    const total = rows.reduce((s, r) => s + r.valeur, 0) || 1;
    return rows.map(r => ({ label: r.label, valeur: r.valeur, pct: (r.valeur * 100) / total, color: this.porteColor(r.label) }));
  });

  filtered = computed<RejetScan[]>(() => {
    const d = this.data();
    if (!d) return [];
    const q = this.search().trim().toLowerCase();
    if (!q) return d.scans;
    return d.scans.filter(s =>
      (s.codebarre || '').toLowerCase().includes(q) ||
      (s.eventTitle || '').toLowerCase().includes(q) ||
      (s.porte || '').toLowerCase().includes(q) ||
      (s.description || '').toLowerCase().includes(q) ||
      categorie(s.description).toLowerCase().includes(q));
  });

  // ---- Charts ----
  jourOpt = computed<EChartsOption>(() => {
    const rows = this.data()?.parJour ?? [];
    return {
      grid: { left: 8, right: 16, top: 16, bottom: 8, containLabel: true },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: rows.map(r => this.fmtDay(r.jour)) },
      yAxis: { type: 'value' },
      series: [{
        type: 'line', smooth: true, showSymbol: false, name: 'Rejets',
        areaStyle: { opacity: 0.12 }, itemStyle: { color: BRAND.warn }, lineStyle: { color: BRAND.warn },
        data: rows.map(r => r.rejets)
      }]
    };
  });

  modeleOpt = computed<EChartsOption>(() => {
    const rows = [...(this.data()?.parModele ?? [])].slice(0, 10).reverse();
    return {
      grid: { left: 8, right: 24, top: 10, bottom: 8, containLabel: true },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: rows.map(r => r.modelName), axisLabel: { width: 140, overflow: 'truncate' } },
      series: [{ type: 'bar', barMaxWidth: 16, itemStyle: { color: SERIES_COLORS[0], borderRadius: [0, 4, 4, 0] }, data: rows.map(r => r.rejets) }]
    };
  });

  evenementOpt = computed<EChartsOption>(() => {
    const rows = [...(this.data()?.parEvenement ?? [])].slice(0, 10).reverse();
    return {
      grid: { left: 12, right: 24, top: 10, bottom: 8, containLabel: true },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: rows.map(r => r.eventTitle), axisLabel: { width: 150, overflow: 'truncate' } },
      series: [{ type: 'bar', barMaxWidth: 16, itemStyle: { color: BRAND.warn, borderRadius: [0, 4, 4, 0] }, data: rows.map(r => r.rejets) }]
    };
  });

  constructor(private stats: StatsService) {}

  ngOnInit(): void { this.fetch(); }

  actualiser(): void { this.fetch(true); }

  private reqId = 0;
  private fetch(refresh = false): void {
    const seq = ++this.reqId;       // 3.4 : ignore les réponses obsolètes
    this.loading.set(true);
    this.error.set(false);
    this.stats.rejets(refresh).subscribe({
      next: (d) => { if (seq !== this.reqId) return; this.data.set(d); this.loading.set(false); },
      error: () => { if (seq !== this.reqId) return; this.error.set(true); this.loading.set(false); }
    });
  }

  cat(desc: string | null): string { return categorie(desc); }
  catColor(desc: string | null): string { return CAT_COLOR[categorie(desc)] ?? BRAND.muted; }
  porteLabel(p: string | null): string {
    const v = (p || '').toLowerCase();
    if (v.startsWith('vip')) return 'VIP';
    if (v.startsWith('public')) return 'Public';
    return p || '—';
  }
  porteColor(p: string | null): string {
    const v = (p || '').toLowerCase();
    if (v.startsWith('vip')) return BRAND.accent;
    if (v.startsWith('public')) return BRAND.primary;
    return BRAND.muted;
  }
  porteTint(p: string | null): string {
    const v = (p || '').toLowerCase();
    if (v.startsWith('vip')) return 'rgba(15,157,157,.10)';
    if (v.startsWith('public')) return 'rgba(31,95,139,.10)';
    return 'rgba(91,100,112,.10)';
  }
  fmtDateTime(s: string | null): string {
    const d = realDate(s);
    return d ? d.toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }) : '—';
  }
  fmtDay(s: string): string {
    const d = realDate(s);
    return d ? d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' }) : '—';
  }
}
