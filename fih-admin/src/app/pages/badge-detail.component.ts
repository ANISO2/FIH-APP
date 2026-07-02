import { Component, Input, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BadgeService } from '../core/badge.service';
import { Availability, BadgeItem, Page, LotPreview, BadgeCounts, BadgeStatus } from '../core/models';
import { LoadingSkeletonComponent } from '../shared/loading-skeleton.component';
import { EmptyStateComponent } from '../shared/empty-state.component';
import { NumPipe, FDatePipe } from '../shared/format';

const CONFIRM_SINGLE =
  "Attention : cette affectation est définitive. L'entrée sera considérée comme délivrée et " +
  "affectée une seule fois — vous ne pourrez plus la modifier. Confirmer ?";

@Component({
  selector: 'app-badge-detail',
  standalone: true,
  imports: [FormsModule, LoadingSkeletonComponent, EmptyStateComponent, NumPipe, FDatePipe],
  template: `
    <button (click)="back()" class="flex items-center gap-1 text-sm text-muted hover:text-ink mb-4 transition-colors">
      <span class="msr text-[18px]">arrow_back</span> Retour aux invitations & badges
    </button>

    @if (errorMsg()) {
      <div class="surface-card p-4 mb-4 flex items-center gap-2" style="background:#fbeae0;color:var(--warn)">
        <span class="msr">error</span> {{ errorMsg() }}
      </div>
    }

    @if (skipMsg()) {
      <div class="surface-card p-4 mb-4 flex items-center gap-2" style="background:#fbeae0;color:var(--warn)">
        <span class="msr">info</span> {{ skipMsg() }}
      </div>
    }

    @if (header(); as h) {
      <div class="surface-card p-6 mb-4">
        <div class="flex items-start justify-between gap-4">
          <div>
            <h2 class="text-2xl font-bold text-ink">{{ h.eventTitle }}</h2>
            <p class="text-muted">{{ h.eventDate | fdate }} · {{ h.modelName }}</p>
          </div>
          <!-- Feature 3 — clear badge telling the operator whether this type prints. -->
          @if (h.printable) {
            <span class="inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-full"
                  style="background:rgba(10,124,74,.12);color:var(--success)">
              <span class="msr text-[16px]">print</span> Type imprimable
            </span>
          } @else {
            <span class="inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-full"
                  style="background:var(--bg);color:var(--muted)" title="Ce type ne s'imprime pas : affectation uniquement">
              <span class="msr text-[16px]">how_to_reg</span> Affectation uniquement
            </span>
          }
        </div>
        <div class="flex flex-wrap gap-2 mt-4 items-center">
          @for (z of h.accessZones; track z) {
            <span class="text-[11px] font-semibold px-2 py-0.5 rounded-full text-white" [style.background]="zoneColor(z)">{{ z }}</span>
          }
          <span class="chip">{{ h.injectedCount | num }} injectés</span>
          @if (h.printable) {
            @if (h.eventHasPoster) {
              <span class="chip" style="color:var(--success)">Affiche présente</span>
            } @else {
              <span class="chip" style="color:var(--warn)">Affiche manquante ({{ h.eventId }}.jpg)</span>
            }
          }
        </div>
      </div>
    }

    <!-- ============== Affectation par lot (Change C) ============== -->
    <div class="surface-card p-5 mb-4">
      <div class="flex items-center gap-2 mb-3">
        <span class="msr text-primary">groups</span>
        <h3 class="font-semibold text-ink">Affectation par lot</h3>
        <span class="text-xs text-muted">— attribue NOM-01, NOM-02 … à une plage de numéros de série</span>
      </div>
      <div class="flex flex-wrap items-end gap-3">
        <label class="text-sm">
          <span class="block text-muted mb-1">N° série début</span>
          <input [(ngModel)]="lotStart" placeholder="ex. 8250000000"
                 class="px-3 py-2 rounded-lg border border-line bg-white w-44 focus:border-accent outline-none" />
        </label>
        <label class="text-sm">
          <span class="block text-muted mb-1">N° série fin</span>
          <input [(ngModel)]="lotEnd" placeholder="ex. 8250000050"
                 class="px-3 py-2 rounded-lg border border-line bg-white w-44 focus:border-accent outline-none" />
        </label>
        <label class="text-sm">
          <span class="block text-muted mb-1">Nom de base</span>
          <input [(ngModel)]="lotBase" placeholder="ex. ANIS"
                 class="px-3 py-2 rounded-lg border border-line bg-white w-44 focus:border-accent outline-none" />
        </label>
        <button (click)="doPreview()" [disabled]="lotLoading()"
                class="px-3 py-2 rounded-lg text-sm font-medium border border-line bg-white hover:bg-bg disabled:opacity-50">
          @if (lotLoading()) { <span class="msr text-[16px] animate-spin align-middle">progress_activity</span> } Prévisualiser
        </button>
      </div>

      @if (lotMsg()) {
        <div class="mt-3 text-sm px-3 py-2 rounded-lg" style="background:rgba(10,124,74,.10);color:var(--success)">{{ lotMsg() }}</div>
      }

      @if (preview(); as p) {
        <div class="mt-4 border-t border-line pt-4">
          <div class="flex flex-wrap gap-2 mb-3">
            <span class="chip"><b>{{ p.eligibleCount | num }}</b>&nbsp;entrée(s) éligible(s)</span>
            @if (p.alreadyAssignedCount > 0) {
              <span class="chip" style="color:var(--warn)">{{ p.alreadyAssignedCount }} déjà affectée(s)</span>
            }
            @if (p.nonInvitationCount > 0) {
              <span class="chip" style="color:var(--warn)">{{ p.nonInvitationCount }} entrée(s) payante(s) ignorée(s)</span>
            }
            @if (p.baseNameAlreadyUsed) {
              <span class="chip">Séquence « {{ lotBase }} » poursuivie</span>
            }
          </div>

          @if (p.eligibleCount === 0) {
            <div class="text-sm text-muted">Aucune entrée affectable dans cette plage.</div>
          } @else if (!p.canAssign) {
            <div class="text-sm px-3 py-2 rounded-lg mb-3" style="background:#fbeae0;color:var(--warn)">
              Lot bloqué : certaines entrées de la plage sont déjà affectées
              ({{ p.alreadyAssignedSerials.join(', ') }}). Ajustez la plage.
            </div>
          }

          @if (p.items.length > 0) {
            <div class="text-xs text-muted mb-2">
              Aperçu : {{ p.items[0].proposedName }} … {{ p.items[p.items.length - 1].proposedName }}
            </div>
            <div class="table-scroll" style="max-height:260px">
              <table class="w-full text-sm">
                <thead>
                  <tr class="single text-left text-muted">
                    <th class="px-4 py-2">N° série</th>
                    <th class="px-4 py-2">Événement</th>
                    <th class="px-4 py-2">Nom proposé</th>
                    <th class="px-4 py-2">État</th>
                  </tr>
                </thead>
                <tbody>
                  @for (it of p.items; track it.numeroserie) {
                    <tr class="border-b border-line/60">
                      <td class="px-4 py-2 font-medium text-ink">{{ it.numeroserie }}</td>
                      <td class="px-4 py-2 text-muted">{{ it.eventTitle }}</td>
                      <td class="px-4 py-2">{{ it.proposedName }}</td>
                      <td class="px-4 py-2">
                        @if (it.alreadyAssigned) {
                          <span style="color:var(--warn)">déjà : {{ it.existingName }}</span>
                        } @else {
                          <span style="color:var(--success)">à affecter</span>
                        }
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <div class="flex flex-wrap gap-2 mt-3">
              <button (click)="askAssign(p)" [disabled]="!p.canAssign || lotAssigning()"
                      class="px-3 py-2 rounded-lg text-white text-sm font-medium disabled:opacity-50" style="background:var(--primary)">
                @if (lotAssigning()) { <span class="msr text-[16px] animate-spin align-middle">progress_activity</span> }
                Confirmer l'affectation ({{ p.eligibleCount }})
              </button>
              <!-- Feature 3 — PDF generation only for printable types. -->
              @if (assignedDone() && printable()) {
                <button (click)="generateLot()" [disabled]="generating()"
                        class="px-3 py-2 rounded-lg text-sm font-medium border border-line bg-white hover:bg-bg disabled:opacity-50">
                  Générer les PDF du lot
                </button>
              }
              @if (assignedDone()) {
                <button (click)="downloadManifest()"
                        class="px-3 py-2 rounded-lg text-sm font-medium border border-line bg-white hover:bg-bg">
                  Manifeste CSV
                </button>
              }
            </div>
          }
        </div>
      }
    </div>

    <!-- ============== Feature 2 — compteur + filtre ============== -->
    <div class="surface-card p-5 mb-4">
      <div class="flex flex-wrap items-center justify-between gap-6">
        <!-- Compteur : « X affectées / Y restantes » -->
        @if (counts(); as c) {
          <div class="flex items-center gap-5">
            <div>
              <div class="text-[11px] font-semibold uppercase tracking-wide text-muted">Affectées</div>
              <div class="text-2xl font-extrabold" style="color:var(--success)">{{ c.affected | num }}</div>
            </div>
            <div class="w-px h-9 bg-line"></div>
            <div>
              <div class="text-[11px] font-semibold uppercase tracking-wide text-muted">Restantes</div>
              <div class="text-2xl font-extrabold text-primary">{{ c.pending | num }}</div>
            </div>
            <div class="w-px h-9 bg-line"></div>
            <div>
              <div class="text-[11px] font-semibold uppercase tracking-wide text-muted">Total</div>
              <div class="text-2xl font-extrabold text-ink">{{ c.total | num }}</div>
            </div>
          </div>
          <div class="flex-1 min-w-[180px] max-w-xs">
            <div class="flex justify-between text-xs text-muted mb-1">
              <span>{{ affectedPct() }} % affectées</span>
              <span>{{ c.affected | num }} / {{ c.total | num }}</span>
            </div>
            <div class="h-2 rounded-full overflow-hidden" style="background:var(--line)">
              <div class="h-full rounded-full transition-all" [style.width.%]="affectedPct()" style="background:var(--success)"></div>
            </div>
          </div>
        } @else {
          <div class="text-sm text-muted">Chargement du compteur…</div>
        }
      </div>

      <!-- Filtre segmenté : Restantes (défaut) · Affectées · Tout -->
      <div class="mt-4 inline-flex rounded-xl border border-line overflow-hidden text-sm font-medium">
        <button (click)="setStatus('pending')"
                [style.background]="isStatus('pending') ? 'var(--primary)' : 'white'"
                [style.color]="isStatus('pending') ? 'white' : 'var(--ink)'"
                class="px-4 py-2 flex items-center gap-1.5 transition-colors">
          <span class="msr text-[17px]">hourglass_empty</span> Restantes
        </button>
        <button (click)="setStatus('affected')"
                [style.background]="isStatus('affected') ? 'var(--primary)' : 'white'"
                [style.color]="isStatus('affected') ? 'white' : 'var(--ink)'"
                class="px-4 py-2 flex items-center gap-1.5 border-l border-line transition-colors">
          <span class="msr text-[17px]">how_to_reg</span> Voir les invitations affectées
        </button>
        <button (click)="setStatus('all')"
                [style.background]="isStatus('all') ? 'var(--primary)' : 'white'"
                [style.color]="isStatus('all') ? 'white' : 'var(--ink)'"
                class="px-4 py-2 flex items-center gap-1.5 border-l border-line transition-colors">
          <span class="msr text-[17px]">list</span> Afficher tout
        </button>
      </div>
    </div>

    <!-- ============== Barre d'outils ============== -->
    <div class="flex flex-wrap items-center gap-3 mb-4">
      <div class="relative">
        <span class="msr absolute left-3 top-1/2 -translate-y-1/2 text-muted text-[20px]">search</span>
        <input [(ngModel)]="search" (keyup.enter)="reload(0)" placeholder="série / code-barres / nom…"
               class="pl-10 pr-3 py-2.5 rounded-xl border border-line bg-white w-64 max-w-full focus:border-accent outline-none" />
      </div>
      <div class="flex-1"></div>
      <!-- Feature 3 — batch PDF actions only for printable types. -->
      @if (printable()) {
        <button (click)="generateSelected()" [disabled]="selected.size === 0 || generating()"
                class="px-3 py-2 rounded-lg text-white text-sm font-medium disabled:opacity-50" style="background:var(--primary)">
          Générer la sélection ({{ selected.size }})
        </button>
        <button (click)="generateAll()" [disabled]="generating()"
                class="px-3 py-2 rounded-lg text-sm font-medium border border-line bg-white hover:bg-bg disabled:opacity-50">
          Tout générer
        </button>
      }
    </div>

    @if (generating()) {
      <div class="surface-card p-4 mb-4 flex items-center gap-2 text-muted">
        <span class="msr animate-spin">progress_activity</span> Génération du PDF…
      </div>
    }

    @if (loading()) {
      <app-loading-skeleton [height]="360" />
    }
    @if (!loading() && page(); as p) {
      @if (p.content.length === 0) {
      <app-empty-state icon="search_off" title="Aucun enregistrement trouvé" [message]="emptyMessage()" />
      } @else {
      <div class="surface-card overflow-hidden">
        <div class="table-scroll">
          <table class="w-full text-sm">
            <thead>
              <tr class="single text-left text-muted">
                @if (printable()) {
                  <th class="px-4 py-3 w-10">
                    <input type="checkbox" [checked]="allOnPageSelected()" (change)="togglePage($event)" />
                  </th>
                }
                <th class="px-4 py-3">N° série</th>
                <th class="px-4 py-3">Code-barres</th>
                <th class="px-4 py-3">Titulaire</th>
                <th class="px-4 py-3">Affectée à</th>
                <th class="px-4 py-3">Statut</th>
                @if (printable()) { <th class="px-4 py-3 text-right">Badge</th> }
              </tr>
            </thead>
            <tbody>
              @for (it of p.content; track it.numeroserie) {
                <tr class="border-b border-line/60 hover:bg-bg transition-colors">
                  @if (printable()) {
                    <td class="px-4 py-3"><input type="checkbox" [checked]="selected.has(it.codebarre)" (change)="toggle(it.codebarre)" /></td>
                  }
                  <td class="px-4 py-3 font-medium text-ink">{{ it.numeroserie }}</td>
                  <td class="px-4 py-3 text-muted">{{ it.codebarre }}</td>
                  <td class="px-4 py-3">{{ it.holderName || '—' }}</td>
                  <td class="px-4 py-3">
                    @if (isAssigned(it)) {
                      <span class="inline-flex items-center gap-1.5 font-medium text-ink">
                        <span class="msr text-[15px] text-muted" title="Affectation définitive">lock</span>
                        {{ it.affecteeA }}
                      </span>
                    } @else {
                      <div class="flex items-center gap-1.5">
                        <input [ngModel]="drafts.get(it.numeroserie) || ''"
                               (ngModelChange)="drafts.set(it.numeroserie, $event)"
                               (keyup.enter)="askSaveName(it)"
                               placeholder="Nom de base (→ NOM-01)…"
                               class="px-2 py-1 rounded-lg border border-line bg-white text-sm w-44 focus:border-accent outline-none" />
                        <button (click)="askSaveName(it)" [disabled]="saving.has(it.numeroserie)"
                                class="p-1.5 rounded-lg border border-line hover:bg-bg disabled:opacity-50" title="Affecter (définitif)"
                                aria-label="Affecter le nom">
                          @if (saving.has(it.numeroserie)) { <span class="msr text-[16px] animate-spin">progress_activity</span> }
                          @else { <span class="msr text-[16px]">save</span> }
                        </button>
                      </div>
                    }
                  </td>
                  <td class="px-4 py-3">
                    @if (it.printedAt) {
                      <span class="text-xs font-semibold px-2 py-0.5 rounded-full" style="background:rgba(10,124,74,.12);color:var(--success)">Imprimé</span>
                    } @else if (isAssigned(it)) {
                      <span class="text-xs font-semibold px-2 py-0.5 rounded-full" style="background:var(--bg);color:var(--muted)">Affecté</span>
                    } @else {
                      <span class="text-xs text-muted">Libre</span>
                    }
                  </td>
                  @if (printable()) {
                    <td class="px-4 py-3 text-right">
                      <button (click)="generateOne(it)"
                              [disabled]="generating() || !isAssigned(it)"
                              [title]="isAssigned(it) ? 'Générer le PDF' : 'Affectez un nom à cette entrée pour générer son PDF'"
                              class="px-2.5 py-1 rounded-lg text-sm border border-line hover:bg-bg disabled:opacity-50">PDF</button>
                    </td>
                  }
                </tr>
              }
            </tbody>
          </table>
        </div>
        <div class="flex items-center justify-between px-4 py-3 border-t border-line text-sm text-muted">
          <span>{{ p.totalElements | num }} enregistrements · page {{ p.page + 1 }} / {{ p.totalPages || 1 }}</span>
          <span class="flex gap-2">
            <button (click)="reload(p.page - 1)" [disabled]="p.page === 0"
                    class="px-3 py-1.5 rounded-lg border border-line bg-white disabled:opacity-40">Précédent</button>
            <button (click)="reload(p.page + 1)" [disabled]="p.page + 1 >= p.totalPages"
                    class="px-3 py-1.5 rounded-lg border border-line bg-white disabled:opacity-40">Suivant</button>
          </span>
        </div>
      </div>
      }
    }

    <!-- ============== Boîte de confirmation (définitif) ============== -->
    @if (confirmOpen()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center p-4" style="background:rgba(0,0,0,.45)">
        <div class="surface-card p-6 max-w-md w-full">
          <div class="flex items-center gap-2 mb-3" style="color:var(--warn)">
            <span class="msr">warning</span><span class="font-semibold">Affectation définitive</span>
          </div>
          <p class="text-sm text-ink mb-3">{{ confirmText() }}</p>
          @if (confirmWarn()) {
            <div class="text-sm px-3 py-2 rounded-lg mb-4" style="background:#fbeae0;color:var(--warn)">{{ confirmWarn() }}</div>
          }
          <div class="flex justify-end gap-2">
            <button (click)="cancelConfirm()" class="px-3 py-2 rounded-lg text-sm border border-line bg-white hover:bg-bg">Annuler</button>
            <button (click)="doConfirm()" class="px-3 py-2 rounded-lg text-sm font-medium text-white" style="background:var(--primary)">Confirmer</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`.chip{background:var(--bg);border:1px solid var(--line);border-radius:999px;padding:6px 14px;font-size:13px;font-weight:600;}`]
})
export class BadgeDetailComponent implements OnInit {
  @Input() eventId!: string;
  @Input() modelId!: string;

  loading = signal(true);
  generating = signal(false);
  errorMsg = signal<string | null>(null);
  skipMsg = signal<string | null>(null);   // Change D: unaffected entries skipped during batch
  header = signal<Availability | null>(null);
  page = signal<Page<BadgeItem> | null>(null);
  search = '';
  size = 25;
  selected = new Set<string>();
  saving = new Set<string>();
  // Rows already assigned (locked). Kept separate from the text being typed so
  // editing the draft never flips a row into the locked state mid-keystroke.
  assigned = new Set<string>();
  // In-progress text for not-yet-assigned rows, keyed by numeroserie.
  drafts = new Map<string, string>();

  // Feature 2 — assignment status filter (default: only the pending ones) + live counter.
  status = signal<BadgeStatus>('pending');
  counts = signal<BadgeCounts | null>(null);

  // Feature 3 — whether this model may be printed (Imprimer). Assign-only when false.
  printable = computed(() => this.header()?.printable ?? false);

  // Lot state (Change C)
  lotStart = '';
  lotEnd = '';
  lotBase = '';
  lotLoading = signal(false);
  lotAssigning = signal(false);
  lotMsg = signal<string | null>(null);
  preview = signal<LotPreview | null>(null);
  assignedDone = signal(false);
  private assignedSerials: string[] = [];

  // Confirm modal
  confirmOpen = signal(false);
  confirmText = signal('');
  confirmWarn = signal<string | null>(null);
  private pendingAction: (() => void) | null = null;

  constructor(private badges: BadgeService, private router: Router) {}

  private get eId(): number { return Number(this.eventId); }
  private get mId(): number { return Number(this.modelId); }

  ngOnInit(): void {
    this.badges.availability(this.eId).subscribe({
      next: (rows) => this.header.set(rows.find(r => r.modelId === this.mId) ?? null),
      error: () => {}
    });
    this.loadCounts();
    this.reload(0);
  }

  // Feature 2 — the "X affectées / Y restantes" counter (search-independent).
  loadCounts(): void {
    this.badges.counts(this.eId, this.mId).subscribe({
      next: (c) => this.counts.set(c),
      error: () => this.counts.set(null)
    });
  }
  affectedPct(): number {
    const c = this.counts();
    if (!c || c.total === 0) return 0;
    return Math.round((c.affected / c.total) * 100);
  }

  // Feature 2 — switch the visible set. Selection is cleared to avoid acting on hidden rows.
  isStatus(s: BadgeStatus): boolean { return this.status() === s; }
  setStatus(s: BadgeStatus): void {
    if (this.status() === s) return;
    this.status.set(s);
    this.selected.clear();
    this.reload(0);
  }
  emptyMessage(): string {
    switch (this.status()) {
      case 'pending':  return 'Toutes les entrées de ce type ont déjà été affectées.';
      case 'affected': return 'Aucune entrée affectée pour le moment.';
      default:         return 'Essayez une autre recherche.';
    }
  }

  reload(page: number): void {
    this.loading.set(true);
    this.badges.items(this.eId, this.mId, page, this.size, this.search, this.status()).subscribe({
      next: (p) => {
        // Rebuild the locked set from server truth: a row is locked iff the DB
        // already has a name for it. This is what survives a refresh.
        for (const it of p.content) {
          if (it.affecteeA && it.affecteeA.trim()) this.assigned.add(it.numeroserie);
        }
        this.page.set(p);
        this.loading.set(false);
      },
      error: () => { this.loading.set(false); this.errorMsg.set('Impossible de charger les enregistrements.'); }
    });
  }

  /** True when this row is already assigned in the DB (locked, definitive). */
  isAssigned(it: BadgeItem): boolean {
    return this.assigned.has(it.numeroserie) || !!(it.affecteeA && it.affecteeA.trim());
  }

  toggle(code: string): void { this.selected.has(code) ? this.selected.delete(code) : this.selected.add(code); }
  allOnPageSelected(): boolean {
    const c = this.page()?.content ?? [];
    return c.length > 0 && c.every(i => this.selected.has(i.codebarre));
  }
  togglePage(ev: Event): void {
    const on = (ev.target as HTMLInputElement).checked;
    for (const i of this.page()?.content ?? []) on ? this.selected.add(i.codebarre) : this.selected.delete(i.codebarre);
  }

  // ---- Confirm modal ----
  private askConfirm(text: string, action: () => void): void {
    this.confirmText.set(text);
    this.pendingAction = action;
    this.confirmOpen.set(true);
  }
  doConfirm(): void { this.confirmOpen.set(false); this.confirmWarn.set(null); const a = this.pendingAction; this.pendingAction = null; if (a) a(); }
  cancelConfirm(): void { this.confirmOpen.set(false); this.confirmWarn.set(null); this.pendingAction = null; }

  // ---- Single one-time assignment (Change B) ----
  askSaveName(it: BadgeItem): void {
    const base = (this.drafts.get(it.numeroserie) || '').trim();
    if (!base) { this.errorMsg.set('Le nom est obligatoire.'); return; }
    this.confirmWarn.set(null);
    this.askConfirm(CONFIRM_SINGLE, () => this.saveName(it, base));
  }
  private saveName(it: BadgeItem, name: string): void {
    this.saving.add(it.numeroserie);
    this.errorMsg.set(null);
    this.badges.saveAffectee(it.numeroserie, name).subscribe({
      next: (dto) => {
        it.affecteeA = dto.affecteeA;
        it.printedAt = dto.printedAt;
        this.assigned.add(it.numeroserie);   // lock only AFTER a successful save
        this.drafts.delete(it.numeroserie);
        this.saving.delete(it.numeroserie);
        this.loadCounts();                    // Feature 2 — keep the counter live
        // In the "Restantes" view the row has just left the set: refresh it away.
        if (this.status() === 'pending') this.reload(this.page()?.page ?? 0);
      },
      error: (e) => {
        this.saving.delete(it.numeroserie);
        // 409 => already assigned on the server: lock it so it can't be retried.
        if (e?.status === 409) this.assigned.add(it.numeroserie);
        this.errorMsg.set(e?.error?.message || "Échec de l'affectation du nom.");
        this.reload(this.page()?.page ?? 0);
      }
    });
  }

  // ---- Lot (Change C) ----
  private lotReq() {
    return { startSerie: this.lotStart.trim(), endSerie: this.lotEnd.trim(), baseName: this.lotBase.trim() };
  }
  doPreview(): void {
    const req = this.lotReq();
    if (!req.startSerie || !req.endSerie || !req.baseName) { this.errorMsg.set('Début, fin et nom de base sont obligatoires.'); return; }
    this.errorMsg.set(null);
    this.lotMsg.set(null);
    this.assignedDone.set(false);
    this.lotLoading.set(true);
    this.badges.lotPreview(req).subscribe({
      next: (p) => { this.preview.set(p); this.lotLoading.set(false); },
      error: (e) => { this.lotLoading.set(false); this.errorMsg.set(e?.error?.message || 'Échec de la prévisualisation.'); }
    });
  }
  askAssign(p: LotPreview): void {
    if (!p.canAssign) return;
    const text = "Attention : cette affectation par lot est définitive. " + p.eligibleCount +
      " entrée(s) seront affectées une seule fois et ne pourront plus être modifiées. Confirmer ?";
    this.confirmWarn.set(null);
    this.askConfirm(text, () => this.assignLot());
  }
  private assignLot(): void {
    const req = this.lotReq();
    this.lotAssigning.set(true);
    this.errorMsg.set(null);
    this.badges.lotAssign(req).subscribe({
      next: (res) => {
        this.lotAssigning.set(false);
        this.assignedSerials = res.assigned.map(a => a.numeroserie);
        this.assignedDone.set(true);
        this.lotMsg.set(res.assignedCount + ' entrée(s) affectée(s) avec succès.');
        this.loadCounts();                    // Feature 2 — keep the counter live
        this.reload(this.page()?.page ?? 0);
      },
      error: (e) => {
        this.lotAssigning.set(false);
        this.errorMsg.set(e?.error?.message || "Échec de l'affectation du lot.");
        this.doPreview();
      }
    });
  }
  generateLot(): void {
    if (this.assignedSerials.length === 0) return;
    this.run(() => this.badges.batch(this.eId, this.mId, this.assignedSerials), 'lot.pdf');
  }
  downloadManifest(): void {
    const req = this.lotReq();
    this.badges.lotManifest(req.startSerie, req.endSerie).subscribe({
      next: (res) => this.badges.saveResponse(res, 'manifest_lot.csv'),
      error: () => this.errorMsg.set('Échec du téléchargement du manifeste.')
    });
  }

  // ---- Generation ----
  generateOne(it: BadgeItem): void {
    this.run(() => this.badges.single(it.type.toLowerCase(), it.codebarre), `badge_${it.codebarre}.pdf`);
  }
  generateSelected(): void {
    this.run(() => this.badges.batch(this.eId, this.mId, [...this.selected]), 'badges.pdf');
  }
  generateAll(): void {
    this.run(() => this.badges.batch(this.eId, this.mId, null), 'badges.pdf');
  }
  private run(call: () => any, fallbackName: string): void {
    this.generating.set(true);
    this.errorMsg.set(null);
    this.skipMsg.set(null);
    call().subscribe({
      next: (res: any) => {
        this.badges.saveResponse(res, fallbackName);
        // Change D: the backend skips unaffected entries and lists them
        // in the X-Skipped-* headers — surface that to the admin.
        const count = Number(res.headers?.get('X-Skipped-Count') || 0);
        if (count > 0) {
          const serials = res.headers?.get('X-Skipped-Serials') || '';
          const suffix = count > 50 ? ' …' : '';
          this.skipMsg.set(count + ' entrée(s) non affectée(s) ignorée(s) : ' + serials + suffix);
        }
        this.generating.set(false);
        this.loadCounts();
        this.reload(this.page()?.page ?? 0);
      },
      error: (err: any) => {
        this.generating.set(false);
        this.errorMsg.set(err?.status === 422
          ? "Aucune entrée affectée : affectez un nom avant de générer le PDF."
          : 'Échec de la génération du PDF. Veuillez réessayer.');
      }
    });
  }

  zoneColor(z: string): string {
    const v = z.toLowerCase();
    if (v.startsWith('vip') || v === 'v') return 'var(--warn)';
    if (v.startsWith('press') || v === 'r') return '#5b3aa6';
    return 'var(--success)';
  }
  back(): void { this.router.navigate(['badges']); }
}
