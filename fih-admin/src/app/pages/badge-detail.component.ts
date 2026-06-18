import { Component, Input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { BadgeService } from '../core/badge.service';
import { Availability, BadgeItem, Page, PhotoCheck } from '../core/models';
import { LoadingSkeletonComponent } from '../shared/loading-skeleton.component';
import { EmptyStateComponent } from '../shared/empty-state.component';
import { NumPipe, FDatePipe } from '../shared/format';

@Component({
  selector: 'app-badge-detail',
  standalone: true,
  imports: [FormsModule, LoadingSkeletonComponent, EmptyStateComponent, NumPipe, FDatePipe],
  template: `
    <button (click)="back()" class="flex items-center gap-1 text-sm text-muted hover:text-ink mb-4 transition-colors">
      <span class="msr text-[18px]">arrow_back</span> Retour aux badges
    </button>

    @if (errorMsg()) {
      <div class="surface-card p-4 mb-4 flex items-center gap-2" style="background:#fbeae0;color:var(--warn)">
        <span class="msr">error</span> {{ errorMsg() }}
      </div>
    }

    @if (header(); as h) {
      <div class="surface-card p-6 mb-4">
        <h2 class="text-2xl font-bold text-ink">{{ h.eventTitle }}</h2>
        <p class="text-muted">{{ h.eventDate | fdate }} · {{ h.modelName }}</p>
        <div class="flex flex-wrap gap-2 mt-4 items-center">
          @for (z of h.accessZones; track z) {
            <span class="text-[11px] font-semibold px-2 py-0.5 rounded-full text-white" [style.background]="zoneColor(z)">{{ z }}</span>
          }
          <span class="chip">{{ h.injectedCount | num }} injectés</span>
          @if (coverage()) {
            <span class="chip" [style.color]="coverage()!.missing ? 'var(--warn)' : 'var(--success)'">
              {{ coverage()!.withPhoto }}/{{ coverage()!.total }} photos
            </span>
          }
        </div>
      </div>
    }

    <!-- Barre d'outils -->
    <div class="flex flex-wrap items-center gap-3 mb-4">
      <div class="relative">
        <span class="msr absolute left-3 top-1/2 -translate-y-1/2 text-muted text-[20px]">search</span>
        <input [(ngModel)]="search" (keyup.enter)="reload(0)" placeholder="série / code-barres / nom…"
               class="pl-10 pr-3 py-2.5 rounded-xl border border-line bg-white w-64 max-w-full focus:border-accent outline-none" />
      </div>
      <label class="flex items-center gap-2 text-sm cursor-pointer select-none">
        <input type="checkbox" [(ngModel)]="sheetLayout" /> Planche A4 (8 par page)
      </label>
      <div class="flex-1"></div>
      <button (click)="generateSelected()" [disabled]="selected.size === 0 || generating()"
              class="px-3 py-2 rounded-lg text-white text-sm font-medium disabled:opacity-50" style="background:var(--primary)">
        Générer la sélection ({{ selected.size }})
      </button>
      <button (click)="generateAll()" [disabled]="generating()"
              class="px-3 py-2 rounded-lg text-sm font-medium border border-line bg-white hover:bg-bg disabled:opacity-50">
        Tout générer
      </button>
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
      <app-empty-state icon="search_off" title="Aucun enregistrement trouvé" message="Essayez une autre recherche." />
      } @else {
      <div class="surface-card overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="text-left text-muted border-b border-line">
                <th class="px-4 py-3 w-10">
                  <input type="checkbox" [checked]="allOnPageSelected()" (change)="togglePage($event)" />
                </th>
                <th class="px-4 py-3">N° série</th>
                <th class="px-4 py-3">Code-barres</th>
                <th class="px-4 py-3">Titulaire</th>
                <th class="px-4 py-3">Affectée à</th>
                <th class="px-4 py-3 text-center">Photo</th>
                <th class="px-4 py-3 text-right">Badge</th>
              </tr>
            </thead>
            <tbody>
              @for (it of p.content; track it.numeroserie) {
                <tr class="border-b border-line/60 hover:bg-bg transition-colors">
                  <td class="px-4 py-3"><input type="checkbox" [checked]="selected.has(it.codebarre)" (change)="toggle(it.codebarre)" /></td>
                  <td class="px-4 py-3 font-medium text-ink">{{ it.numeroserie }}</td>
                  <td class="px-4 py-3 text-muted">{{ it.codebarre }}</td>
                  <td class="px-4 py-3">{{ it.holderName || '—' }}</td>
                  <td class="px-4 py-3">
                    <div class="flex items-center gap-1.5">
                      <input [(ngModel)]="it.affecteeA" (input)="savedOk.delete(it.numeroserie)" (keyup.enter)="saveName(it)"
                             placeholder="Nom à imprimer…"
                             class="px-2 py-1 rounded-lg border border-line bg-white text-sm w-44 focus:border-accent outline-none" />
                      <button (click)="saveName(it)" [disabled]="saving.has(it.numeroserie)"
                              class="p-1.5 rounded-lg border border-line hover:bg-bg disabled:opacity-50" title="Enregistrer le nom">
                        @if (saving.has(it.numeroserie)) { <span class="msr text-[16px] animate-spin">progress_activity</span> }
                        @else if (savedOk.has(it.numeroserie)) { <span class="msr text-[16px]" style="color:var(--success)">check</span> }
                        @else { <span class="msr text-[16px]">save</span> }
                      </button>
                    </div>
                  </td>
                  <td class="px-4 py-3 text-center">
                    @if (it.hasPhoto) { <span class="msr" style="color:var(--success)">check_circle</span> }
                    @else { <span class="msr text-muted">remove</span> }
                  </td>
                  <td class="px-4 py-3 text-right">
                    <button (click)="generateOne(it)" [disabled]="generating()"
                            class="px-2.5 py-1 rounded-lg text-sm border border-line hover:bg-bg disabled:opacity-50">PDF</button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <!-- Pagination -->
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
  `,
  styles: [`.chip{background:var(--bg);border:1px solid var(--line);border-radius:999px;padding:6px 14px;font-size:13px;font-weight:600;}`]
})
export class BadgeDetailComponent implements OnInit {
  @Input() eventId!: string;
  @Input() modelId!: string;

  loading = signal(true);
  generating = signal(false);
  errorMsg = signal<string | null>(null);
  header = signal<Availability | null>(null);
  coverage = signal<PhotoCheck | null>(null);
  page = signal<Page<BadgeItem> | null>(null);
  search = '';
  sheetLayout = false;
  size = 25;
  selected = new Set<string>();
  // "Affectée à" inline editing state, keyed by numeroserie.
  saving = new Set<string>();
  savedOk = new Set<string>();

  constructor(private badges: BadgeService, private router: Router) {}

  private get eId(): number { return Number(this.eventId); }
  private get mId(): number { return Number(this.modelId); }

  ngOnInit(): void {
    this.badges.availability(this.eId).subscribe({
      next: (rows) => this.header.set(rows.find(r => r.modelId === this.mId) ?? null),
      error: () => {}
    });
    this.badges.photoCheck(this.eId, this.mId).subscribe({ next: (c) => this.coverage.set(c), error: () => {} });
    this.reload(0);
  }

  reload(page: number): void {
    this.loading.set(true);
    this.badges.items(this.eId, this.mId, page, this.size, this.search).subscribe({
      next: (p) => { this.page.set(p); this.loading.set(false); },
      error: () => { this.loading.set(false); this.errorMsg.set('Impossible de charger les enregistrements.'); }
    });
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

  /** Upsert the "Affectée à" name for one row (the only write action in the UI). */
  saveName(it: BadgeItem): void {
    const name = (it.affecteeA || '').trim();
    if (!name) { this.errorMsg.set('Le nom est obligatoire.'); return; }
    this.saving.add(it.numeroserie);
    this.savedOk.delete(it.numeroserie);
    this.errorMsg.set(null);
    this.badges.saveAffectee(it.numeroserie, name).subscribe({
      next: (dto) => { it.affecteeA = dto.affecteeA; this.saving.delete(it.numeroserie); this.savedOk.add(it.numeroserie); },
      error: (e) => {
        this.saving.delete(it.numeroserie);
        this.errorMsg.set(e?.error?.message || 'Échec de l\'enregistrement du nom.');
      }
    });
  }

  generateOne(it: BadgeItem): void {
    this.run(() => this.badges.single(it.type.toLowerCase(), it.codebarre), `badge_${it.codebarre}.pdf`);
  }
  generateSelected(): void {
    this.run(() => this.badges.batch(this.eId, this.mId, [...this.selected], this.layout()), 'badges.pdf');
  }
  generateAll(): void {
    this.run(() => this.badges.batch(this.eId, this.mId, null, this.layout()), 'badges.pdf');
  }

  private layout(): 'single' | 'sheet' { return this.sheetLayout ? 'sheet' : 'single'; }

  private run(call: () => any, fallbackName: string): void {
    this.generating.set(true);
    this.errorMsg.set(null);
    call().subscribe({
      next: (res: any) => { this.badges.saveResponse(res, fallbackName); this.generating.set(false); this.noteMissing(); },
      error: () => { this.generating.set(false); this.errorMsg.set('Échec de la génération du PDF. Veuillez réessayer.'); }
    });
  }

  private noteMissing(): void {
    const c = this.coverage();
    if (c && c.missing > 0) {
      this.errorMsg.set(`Terminé — note : ${c.missing} badge(s) sur ${c.total} sans photo (image par défaut utilisée).`);
    }
  }

  zoneColor(z: string): string {
    const v = z.toLowerCase();
    if (v.startsWith('vip') || v === 'v') return 'var(--warn)';
    if (v.startsWith('press') || v === 'r') return '#5b3aa6';
    return 'var(--success)';
  }
  back(): void { this.router.navigate(['badges']); }
}
