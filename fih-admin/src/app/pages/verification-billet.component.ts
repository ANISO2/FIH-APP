import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FDatePipe } from '../shared/format';
import { EmptyStateComponent } from '../shared/empty-state.component';
import { LoadingSkeletonComponent } from '../shared/loading-skeleton.component';
import { VerificationDetailModalComponent } from './verification-detail-modal.component';
import { VerificationService } from '../core/verification.service';
import { BilletSearchRow, PageResult, SearchField, SearchMode, TicketDetails } from '../core/models';

/**
 * Vérification Billet (3.2) — recherche d'un billet par code à barre ou numéro
 * de série (colonnes indexées), liste paginée côté serveur, puis fenêtre de
 * détails. Entièrement en lecture seule.
 */
@Component({
  selector: 'app-verification-billet',
  standalone: true,
  imports: [FormsModule, RouterLink, FDatePipe, EmptyStateComponent, LoadingSkeletonComponent, VerificationDetailModalComponent],
  template: `
    <div class="mb-5">
      <h2 class="text-xl font-semibold text-ink">Vérification — Billet</h2>
      <p class="text-sm text-muted">Recherche par code à barre ou numéro de série.</p>
    </div>

    <!-- Barre de recherche -->
    <div class="surface-card p-4 mb-6">
      <div class="flex flex-col lg:flex-row lg:items-end gap-3">
        <label class="flex flex-col gap-1 text-sm">
          <span class="text-muted">Rechercher par</span>
          <select [(ngModel)]="field" class="ctrl">
            <option value="codebarre">Code à barre</option>
            <option value="numeroserie">Numéro de série</option>
          </select>
        </label>
        <label class="flex flex-col gap-1 text-sm">
          <span class="text-muted">Correspondance</span>
          <select [(ngModel)]="mode" class="ctrl">
            <option value="exact">Exacte</option>
            <option value="prefix">Commence par</option>
          </select>
        </label>
        <label class="flex flex-col gap-1 text-sm flex-1">
          <span class="text-muted">{{ field === 'numeroserie' ? 'Numéro de série' : 'Code à barre' }}</span>
          <input [(ngModel)]="value" (keyup.enter)="search(0)" class="ctrl"
                 [placeholder]="field === 'numeroserie' ? 'ex. 7570000200' : 'ex. 124185LJ'" />
        </label>
        <div class="flex gap-2">
          <button class="btn-primary" (click)="search(0)" [disabled]="loading()">
            <span class="msr text-[18px]">search</span> Rechercher
          </button>
          <button class="btn-ghost" (click)="exportCsv()" [disabled]="!result() || result()!.content.length === 0">
            <span class="msr text-[18px]">download</span> Télécharger
          </button>
        </div>
      </div>
      @if (mode === 'prefix') {
        <p class="text-xs text-muted mt-2">La recherche « commence par » est limitée à un préfixe indexé ; la recherche « contient » n'est pas proposée (analyse complète).</p>
      }
    </div>

    <!-- Résultats -->
    @if (loading()) {
      <app-loading-skeleton [height]="260" />
    } @else if (error()) {
      <app-empty-state [error]="true" title="Recherche impossible"
                       message="Le serveur est peut-être indisponible ou votre session a expiré." />
    } @else if (searched() && (!result() || result()!.content.length === 0)) {
      @if (crossType()) {
        <app-empty-state icon="swap_horiz" title="Ce code correspond à un voucher, pas à un billet."
                         [message]="'Ce code existe parmi les vouchers. Ouvrez la recherche Voucher pour « ' + value + ' ».'" />
        <div class="flex justify-center mt-4">
          <a class="btn-primary" [routerLink]="['/verification/voucher']"
             [queryParams]="{ value: value, field: field, mode: mode }">
            <span class="msr text-[18px]">arrow_forward</span> Rechercher dans les vouchers
          </a>
        </div>
      } @else {
        <app-empty-state title="Aucun enregistrement trouvé." message="Vérifiez le code saisi et réessayez." />
      }
    } @else if (result() && result()!.content.length > 0) {
      <div class="surface-card overflow-hidden">
        <div class="px-5 py-3 border-b border-line flex items-center gap-2">
          <span class="font-semibold text-ink">Liste des billets</span>
          <span class="text-xs text-muted">{{ result()!.totalElements }} résultat(s)</span>
        </div>
        <div class="table-scroll table-zebra">
          <table class="text-sm">
            <thead><tr class="single text-left text-muted">
              <th class="px-4 py-3">Numéro de série</th>
              <th class="px-4 py-3">Code à barre</th>
              <th class="px-4 py-3 text-center">Activation</th>
              <th class="px-4 py-3 text-center">Livré</th>
              <th class="px-4 py-3 text-center">Vendu</th>
              <th class="px-4 py-3 text-center">Utilisé</th>
              <th class="px-4 py-3">Événement</th>
              <th class="px-4 py-3">Modèle</th>
              <th class="px-4 py-3">Date vente</th>
              <th class="px-4 py-3">Livreur</th>
              <th class="px-4 py-3">Date livraison</th>
              <th class="px-4 py-3 text-center">Détails</th>
            </tr></thead>
            <tbody>
              @for (r of result()!.content; track r.numeroserie) {
                <tr class="border-b border-line/60">
                  <td class="px-4 py-3 tabular font-medium text-ink">{{ r.numeroserie }}</td>
                  <td class="px-4 py-3 tabular">{{ r.codebarre || '—' }}</td>
                  <td class="px-4 py-3 text-center"><span class="msr" [style.color]="r.activation ? 'var(--success)' : 'var(--warn)'">{{ r.activation ? 'check_circle' : 'cancel' }}</span></td>
                  <td class="px-4 py-3 text-center"><span class="msr" [style.color]="r.livre ? 'var(--success)' : 'var(--warn)'">{{ r.livre ? 'check_circle' : 'cancel' }}</span></td>
                  <td class="px-4 py-3 text-center"><span class="msr" [style.color]="r.vendu ? 'var(--success)' : 'var(--warn)'">{{ r.vendu ? 'check_circle' : 'cancel' }}</span></td>
                  <td class="px-4 py-3 text-center"><span class="msr" [style.color]="r.utilise ? 'var(--success)' : 'var(--warn)'">{{ r.utilise ? 'check_circle' : 'cancel' }}</span></td>
                  <td class="px-4 py-3">{{ r.eventTitle || '—' }}</td>
                  <td class="px-4 py-3">{{ r.modelName || '—' }}</td>
                  <td class="px-4 py-3">{{ r.dateVente | fdate }}</td>
                  <td class="px-4 py-3">{{ r.livreur || '—' }}</td>
                  <td class="px-4 py-3">{{ r.dateLivraison | fdate }}</td>
                  <td class="px-4 py-3 text-center">
                    <button class="icon-btn" (click)="openDetails(r)" title="Voir les détails">
                      <span class="msr text-[20px]">info</span>
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        @if (result()!.totalPages > 1) {
          <div class="px-5 py-3 border-t border-line flex items-center justify-end gap-3 text-sm">
            <span class="text-muted">Page {{ result()!.page + 1 }} / {{ result()!.totalPages }}</span>
            <button class="btn-ghost" (click)="search(result()!.page - 1)" [disabled]="result()!.page === 0">
              <span class="msr text-[18px]">chevron_left</span>
            </button>
            <button class="btn-ghost" (click)="search(result()!.page + 1)" [disabled]="result()!.page + 1 >= result()!.totalPages">
              <span class="msr text-[18px]">chevron_right</span>
            </button>
          </div>
        }
      </div>
    } @else {
      <app-empty-state icon="search" title="Lancez une recherche"
                       message="Saisissez un code à barre ou un numéro de série, puis « Rechercher »." />
    }

    @if (detail()) {
      <app-verification-detail [data]="detail()!" (close)="detail.set(null)" />
    }
  `,
  styles: [`
    .ctrl {
      height: 38px; padding: 0 12px; border-radius: 9px;
      border: 1px solid var(--line); background: var(--surface); color: var(--ink);
      outline: none; font-size: .875rem;
    }
    .ctrl:focus { border-color: var(--accent); }
    .tabular { font-variant-numeric: tabular-nums; }
    .btn-primary {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 8px 16px; border-radius: 9px; font-size: .875rem; font-weight: 600;
      color: #fff; background: var(--primary); border: 1px solid var(--primary); cursor: pointer;
      transition: opacity .15s ease;
    }
    .btn-primary:hover:not(:disabled) { opacity: .92; }
    .btn-primary:disabled { opacity: .55; cursor: default; }
    .btn-ghost {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 8px 13px; border-radius: 9px; font-size: .875rem; font-weight: 500;
      color: var(--muted); background: var(--surface); border: 1px solid var(--line); cursor: pointer;
      transition: color .15s ease, border-color .15s ease;
    }
    .btn-ghost:hover:not(:disabled) { color: var(--primary); border-color: var(--primary); }
    .btn-ghost:disabled { opacity: .55; cursor: default; }
    .icon-btn {
      display: inline-flex; padding: 4px; border-radius: 8px; border: none;
      background: transparent; color: var(--primary); cursor: pointer;
    }
    .icon-btn:hover { background: var(--bg); }
  `]
})
export class VerificationBilletComponent {
  field: SearchField = 'codebarre';
  mode: SearchMode = 'exact';
  value = '';

  loading = signal(false);
  error = signal(false);
  searched = signal(false);
  result = signal<PageResult<BilletSearchRow> | null>(null);
  detail = signal<TicketDetails | null>(null);
  crossType = signal(false);   // true si le code existe en tant que voucher

  private readonly size = 20;
  private reqId = 0;

  constructor(private api: VerificationService, route: ActivatedRoute) {
    // Pré-remplissage depuis « Rechercher dans les billets » de l'autre onglet.
    const qp = route.snapshot.queryParamMap;
    const v = qp.get('value');
    if (v) {
      this.value = v;
      const f = qp.get('field'); if (f === 'numeroserie' || f === 'codebarre') this.field = f;
      const m = qp.get('mode'); if (m === 'exact' || m === 'prefix') this.mode = m;
      this.search(0);
    }
  }

  search(page: number): void {
    const v = this.value.trim();
    this.searched.set(true);
    this.crossType.set(false);
    if (!v) { this.result.set(null); this.error.set(false); return; }
    const id = ++this.reqId;
    this.loading.set(true);
    this.error.set(false);
    this.api.searchBillets(v, this.field, this.mode, Math.max(page, 0), this.size).subscribe({
      next: (res) => {
        if (id !== this.reqId) return;          // ignore stale responses
        this.result.set(res);
        this.loading.set(false);
        if (res.totalElements === 0) this.checkOtherType(v, id);
      },
      error: () => {
        if (id !== this.reqId) return;
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }

  /** Aucun billet : ce code existe-t-il en tant que voucher ? (réutilise l'endpoint voucher) */
  private checkOtherType(value: string, id: number): void {
    this.api.searchVouchers(value, this.field, this.mode, 0, 1).subscribe({
      next: (res) => { if (id !== this.reqId) return; this.crossType.set(res.totalElements > 0); },
      error: () => {}
    });
  }

  openDetails(r: BilletSearchRow): void {
    this.api.billetDetails(r.numeroserie).subscribe({
      next: (d) => this.detail.set(d),
      error: () => this.detail.set(null)
    });
  }

  exportCsv(): void {
    const rows = this.result()?.content ?? [];
    if (rows.length === 0) return;
    const head = ['Numéro de série', 'Code à barre', 'Activation', 'Livré', 'Vendu', 'Utilisé',
      'Événement', 'Modèle', 'Date vente', 'Livreur', 'Date livraison'];
    const oui = (b: boolean) => (b ? 'Oui' : 'Non');
    const body = rows.map(r => [
      r.numeroserie, r.codebarre ?? '', oui(r.activation), oui(r.livre), oui(r.vendu), oui(r.utilise),
      r.eventTitle ?? '', r.modelName ?? '', r.dateVente ?? '', r.livreur ?? '', r.dateLivraison ?? ''
    ]);
    const csv = [head, ...body]
      .map(line => line.map(c => `"${String(c).replace(/"/g, '""')}"`).join(';'))
      .join('\n');
    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'verification-billets.csv';
    a.click();
    URL.revokeObjectURL(a.href);
  }
}
