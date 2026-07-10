import { Component, OnInit, computed, signal } from '@angular/core';
import { StatsService } from '../core/stats.service';
import { RecetteGuichetSummary, RecetteGuichetDetail } from '../core/models';
import { LoadingSkeletonComponent } from '../shared/loading-skeleton.component';
import { EmptyStateComponent } from '../shared/empty-state.component';
import { NumPipe, TndPipe, FDatePipe } from '../shared/format';

/**
 * §5.2 — Recette par guichet. Box-office revenue, driven by the point-of-sale
 * tables (vente / livraison / kit). Those are empty in the current edition, so
 * both tables legitimately show "Aucun enregistrement trouvé"; the structure is
 * ready for editions that record guichet activity. The "Télécharger" button
 * exports whatever is loaded to a CSV (summary + detail).
 */
@Component({
  selector: 'app-recette-guichet',
  standalone: true,
  imports: [LoadingSkeletonComponent, EmptyStateComponent, NumPipe, TndPipe, FDatePipe],
  template: `
    <div class="flex items-start justify-between gap-4 mb-5">
      <div>
        <h2 class="text-xl font-bold text-ink mb-1">Recette par guichet</h2>
        <p class="text-sm text-muted">Recette des points de vente (guichets)</p>
      </div>
      <button (click)="download()" [disabled]="loading() || (summary().length === 0 && detail().length === 0)"
              class="shrink-0 inline-flex items-center gap-2 px-4 py-2 rounded-xl text-white text-sm font-medium disabled:opacity-50"
              style="background:var(--primary)">
        <span class="msr text-[18px]">download</span> Télécharger
      </button>
    </div>

    @if (loading()) {
      <app-loading-skeleton [height]="200" />
      <div class="mt-6"><app-loading-skeleton [height]="280" /></div>
    } @else if (error()) {
      <app-empty-state [error]="true" title="Impossible de charger la recette par guichet"
                       message="Le serveur est peut-être indisponible." />
    } @else {

      <!-- Résumé -->
      <div class="surface-card overflow-hidden mb-8">
        <div class="px-5 py-3 border-b border-line font-semibold text-ink">Résumé</div>
        @if (summary().length === 0) {
          <div class="p-6"><app-empty-state title="Aucun enregistrement trouvé." message="" /></div>
        } @else {
          <div class="table-scroll">
            <table class="w-full text-sm">
              <thead>
                <tr class="single text-left text-muted">
                  <th class="px-4 py-3">Événement</th>
                  <th class="px-4 py-3 text-right">Billet</th>
                  <th class="px-4 py-3 text-right">Kit</th>
                  <th class="px-4 py-3 text-right">Total</th>
                </tr>
              </thead>
              <tbody>
                @for (r of summary(); track r.eventId) {
                  <tr class="border-b border-line/60 hover:bg-bg transition-colors">
                    <td class="px-4 py-3 font-medium text-ink">
                      {{ r.eventTitle }}<span class="text-xs text-muted ml-1">{{ r.eventDate | fdate:true }}</span>
                    </td>
                    <td class="px-4 py-3 text-right">{{ r.billet | tnd }}</td>
                    <td class="px-4 py-3 text-right">{{ r.kit | tnd }}</td>
                    <td class="px-4 py-3 text-right font-semibold text-ink">{{ r.total | tnd }}</td>
                  </tr>
                }
              </tbody>
              <tfoot>
                <tr class="border-t-2 border-line bg-bg font-bold text-ink">
                  <td class="px-4 py-3">Total</td>
                  <td class="px-4 py-3 text-right">{{ grand().billet | tnd }}</td>
                  <td class="px-4 py-3 text-right">{{ grand().kit | tnd }}</td>
                  <td class="px-4 py-3 text-right" style="color:var(--primary)">{{ grand().total | tnd }}</td>
                </tr>
              </tfoot>
            </table>
          </div>
        }
      </div>

      <!-- Détail -->
      <div class="surface-card overflow-hidden">
        <div class="px-5 py-3 border-b border-line font-semibold text-ink">Détail</div>
        @if (detail().length === 0) {
          <div class="p-6"><app-empty-state title="Aucun enregistrement trouvé." message="" /></div>
        } @else {
          <div class="table-scroll">
            <table class="w-full text-sm whitespace-nowrap">
              <thead>
                <tr class="grp text-muted">
                  <th class="px-3 py-2 text-left" rowspan="2">Événement</th>
                  <th class="px-3 py-2 text-left" rowspan="2">Modèle</th>
                  <th class="px-3 py-2 text-center border-l border-line" colspan="5">Billet</th>
                  <th class="px-3 py-2 text-right border-l border-line" rowspan="2">Kit</th>
                </tr>
                <tr class="sub text-muted text-xs">
                  <th class="px-3 py-1.5 text-right border-l border-line">Livraison</th>
                  <th class="px-3 py-1.5 text-right">Vente</th>
                  <th class="px-3 py-1.5 text-right">Prix Unitaire</th>
                  <th class="px-3 py-1.5 text-right">Recette</th>
                  <th class="px-3 py-1.5 text-right">Reste</th>
                </tr>
              </thead>
              <tbody>
                @for (r of detail(); track $index) {
                  <tr class="border-b border-line/60 hover:bg-bg transition-colors">
                    <td class="px-3 py-2.5 font-medium text-ink">{{ r.eventTitle }}</td>
                    <td class="px-3 py-2.5">{{ r.modelName }}</td>
                    <td class="px-3 py-2.5 text-right border-l border-line">{{ r.billetLivraison | num }}</td>
                    <td class="px-3 py-2.5 text-right">{{ r.billetVente | num }}</td>
                    <td class="px-3 py-2.5 text-right">{{ r.billetPrixUnitaire | tnd }}</td>
                    <td class="px-3 py-2.5 text-right font-semibold">{{ r.billetRecette | tnd }}</td>
                    <td class="px-3 py-2.5 text-right text-muted">{{ r.billetReste | num }}</td>
                    <td class="px-3 py-2.5 text-right border-l border-line">{{ r.kit | tnd }}</td>
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
export class RecetteGuichetComponent implements OnInit {
  loading = signal(true);
  error = signal(false);
  summary = signal<RecetteGuichetSummary[]>([]);
  detail = signal<RecetteGuichetDetail[]>([]);

  grand = computed(() => {
    const acc = { billet: 0, kit: 0, total: 0 };
    for (const r of this.summary()) { acc.billet += r.billet; acc.kit += r.kit; acc.total += r.total; }
    return acc;
  });

  constructor(private stats: StatsService) {}

  ngOnInit(): void { this.fetch(); }

  private reqId = 0;
  private fetch(): void {
    const seq = ++this.reqId;       // 3.4 : ignore les réponses obsolètes
    this.loading.set(true);
    this.error.set(false);
    this.stats.recetteGuichetSummary().subscribe({
      next: (s) => {
        if (seq !== this.reqId) return;
        this.summary.set(s);
        this.stats.recetteGuichetDetail().subscribe({
          next: (d) => { if (seq !== this.reqId) return; this.detail.set(d); this.loading.set(false); },
          error: () => { if (seq !== this.reqId) return; this.error.set(true); this.loading.set(false); }
        });
      },
      error: () => { if (seq !== this.reqId) return; this.error.set(true); this.loading.set(false); }
    });
  }

  /** Export the loaded summary + detail as a CSV (semicolon-separated, fr-friendly). */
  download(): void {
    const sep = ';';
    const lines: string[] = [];
    lines.push('Recette par guichet — Résumé');
    lines.push(['Événement', 'Date', 'Billet', 'Kit', 'Total'].join(sep));
    for (const r of this.summary()) {
      lines.push([r.eventTitle, r.eventDate, r.billet, r.kit, r.total].join(sep));
    }
    lines.push('');
    lines.push('Recette par guichet — Détail');
    lines.push(['Événement', 'Modèle', 'Livraison', 'Vente', 'Prix Unitaire', 'Recette', 'Reste', 'Kit'].join(sep));
    for (const r of this.detail()) {
      lines.push([r.eventTitle, r.modelName, r.billetLivraison, r.billetVente,
        r.billetPrixUnitaire, r.billetRecette, r.billetReste, r.kit].join(sep));
    }
    // BOM so Excel reads UTF-8 accents correctly
    const blob = new Blob(['\uFEFF' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'recette-guichet.csv';
    a.click();
    URL.revokeObjectURL(url);
  }
}
