import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { FDatePipe } from '../shared/format';
import { AccessLog, TicketDetails } from '../core/models';

/**
 * Détails d'un billet / voucher (3.2) — reproduit la fenêtre des maquettes :
 * les quatre états (Vente / Utilisation / Réservation / Activation) + l'identité,
 * puis l'historique d'accès Public et VIP. Lecture seule.
 */
@Component({
  selector: 'app-verification-detail',
  standalone: true,
  imports: [FDatePipe],
  template: `
    <div class="overlay" (click)="close.emit()">
      <div class="modal surface-card" (click)="$event.stopPropagation()">
        <!-- En-tête : identité + états -->
        <div class="px-5 py-4 border-b border-line">
          <div class="flex items-start gap-3">
            <div class="min-w-0">
              <div class="text-xs text-muted">{{ data.type === 'BILLET' ? 'Billet' : 'Voucher' }}</div>
              <div class="text-lg font-semibold text-ink truncate">{{ data.eventTitle || '—' }}</div>
              <div class="text-sm text-muted">{{ data.ticketModel || '—' }}</div>
            </div>
            <button class="btn-ghost ml-auto shrink-0" (click)="close.emit()">
              <span class="msr text-[18px]">close</span> Fermer
            </button>
          </div>

          <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-4">
            <div><div class="lbl">Vente</div><span class="chip" [class.yes]="data.vente">{{ oui(data.vente) }}</span></div>
            <div><div class="lbl">Utilisation</div><span class="chip" [class.yes]="data.utilisation">{{ oui(data.utilisation) }}</span></div>
            <div><div class="lbl">Réservation</div><span class="chip" [class.yes]="data.reservation">{{ oui(data.reservation) }}</span></div>
            <div><div class="lbl">Activation</div><span class="chip" [class.yes]="data.activation">{{ oui(data.activation) }}</span></div>
          </div>
          <div class="grid grid-cols-2 gap-3 mt-3">
            <div><div class="lbl">Numéro de série</div><div class="font-medium text-ink tabular">{{ data.numeroserie }}</div></div>
            <div><div class="lbl">Code à barre</div><div class="font-medium text-ink tabular">{{ data.codebarre || '—' }}</div></div>
          </div>
        </div>

        <!-- Journaux d'accès -->
        <div class="px-5 py-4 space-y-5 overflow-auto" style="max-height:60vh">
          <div>
            <div class="flex items-center gap-2 mb-2">
              <span class="msr text-[18px]" style="color:var(--primary)">meeting_room</span>
              <span class="font-semibold text-ink">Accès Public</span>
              <span class="text-xs text-muted">({{ data.publicLog.length }})</span>
            </div>
            @if (data.publicLog.length === 0) {
              <p class="text-sm text-muted py-3 px-1">Aucun enregistrement trouvé.</p>
            } @else {
              <div class="table-scroll table-zebra rounded-xl border border-line">
                <table class="text-sm">
                  <thead><tr class="single text-left text-muted">
                    <th class="px-3 py-2">Référence</th><th class="px-3 py-2">Date</th><th class="px-3 py-2">Heure</th>
                    <th class="px-3 py-2">Code à barre</th><th class="px-3 py-2">Porte</th><th class="px-3 py-2 text-center">Statut</th>
                  </tr></thead>
                  <tbody>
                    @for (l of data.publicLog; track l.reference) {
                      <tr class="border-b border-line/60">
                        <td class="px-3 py-2 tabular">{{ l.reference }}</td>
                        <td class="px-3 py-2">{{ l.date | fdate }}</td>
                        <td class="px-3 py-2 tabular">{{ heure(l) }}</td>
                        <td class="px-3 py-2 tabular">{{ l.codebarre || '—' }}</td>
                        <td class="px-3 py-2">{{ l.porte || '—' }}</td>
                        <td class="px-3 py-2 text-center">
                          <span class="msr" [style.color]="l.granted ? 'var(--success)' : 'var(--warn)'">
                            {{ l.granted ? 'check_circle' : 'cancel' }}
                          </span>
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </div>

          <div>
            <div class="flex items-center gap-2 mb-2">
              <span class="msr text-[18px]" style="color:var(--accent)">star</span>
              <span class="font-semibold text-ink">Accès VIP</span>
              <span class="text-xs text-muted">({{ data.vipLog.length }})</span>
            </div>
            @if (data.vipLog.length === 0) {
              <p class="text-sm text-muted py-3 px-1">Aucun enregistrement trouvé.</p>
            } @else {
              <div class="table-scroll table-zebra rounded-xl border border-line">
                <table class="text-sm">
                  <thead><tr class="single text-left text-muted">
                    <th class="px-3 py-2">Référence</th><th class="px-3 py-2">Date</th><th class="px-3 py-2">Heure</th>
                    <th class="px-3 py-2">Code à barre</th><th class="px-3 py-2">Porte</th><th class="px-3 py-2 text-center">Statut</th>
                  </tr></thead>
                  <tbody>
                    @for (l of data.vipLog; track l.reference) {
                      <tr class="border-b border-line/60">
                        <td class="px-3 py-2 tabular">{{ l.reference }}</td>
                        <td class="px-3 py-2">{{ l.date | fdate }}</td>
                        <td class="px-3 py-2 tabular">{{ heure(l) }}</td>
                        <td class="px-3 py-2 tabular">{{ l.codebarre || '—' }}</td>
                        <td class="px-3 py-2">{{ l.porte || '—' }}</td>
                        <td class="px-3 py-2 text-center">
                          <span class="msr" [style.color]="l.granted ? 'var(--success)' : 'var(--warn)'">
                            {{ l.granted ? 'check_circle' : 'cancel' }}
                          </span>
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .overlay {
      position: fixed; inset: 0; z-index: 50;
      background: rgba(16, 30, 54, .45);
      display: flex; align-items: flex-start; justify-content: center;
      padding: 24px 16px; overflow: auto;
    }
    .modal { width: 100%; max-width: 820px; background: var(--surface); }
    .lbl { font-size: .72rem; color: var(--muted); margin-bottom: 3px; }
    .tabular { font-variant-numeric: tabular-nums; }
    .chip {
      display: inline-block; padding: 2px 12px; border-radius: 99px;
      font-size: .8rem; font-weight: 600;
      background: var(--line); color: var(--muted);
    }
    .chip.yes { background: rgba(15, 157, 157, .14); color: var(--accent); }
    .btn-ghost {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 7px 13px; border-radius: 9px; font-size: .875rem; font-weight: 500;
      color: var(--muted); background: var(--surface);
      border: 1px solid var(--line); cursor: pointer;
      transition: color .15s ease, border-color .15s ease;
    }
    .btn-ghost:hover { color: var(--primary); border-color: var(--primary); }
  `]
})
export class VerificationDetailModalComponent {
  @Input({ required: true }) data!: TicketDetails;
  @Output() close = new EventEmitter<void>();

  /** Ferme la fenêtre avec la touche Échap. */
  @HostListener('document:keydown.escape')
  onEscape(): void { this.close.emit(); }

  oui(v: boolean): string { return v ? 'Oui' : 'Non'; }

  /** "2025-08-14T18:20:29.329" -> "18:20:29" */
  heure(l: AccessLog): string {
    if (!l.time) return '—';
    const d = new Date(l.time);
    if (isNaN(d.getTime())) return '—';
    return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }
}
