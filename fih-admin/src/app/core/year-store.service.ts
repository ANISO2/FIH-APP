import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

/**
 * Sélecteur d'année global du backoffice (3.2).
 *
 * - La liste des années provient de la base (GET /api/stats/years), dérivée de
 *   evenement.ddate — jamais codée en dur.
 * - `year() === null` signifie « Toutes les années ».
 * - Par défaut, l'année la plus récente présente est sélectionnée.
 *
 * Les pages de statistiques lisent `year()` dans un effect() et rechargent
 * leurs données à chaque changement.
 */
@Injectable({ providedIn: 'root' })
export class YearStore {
  /** Années disponibles, de la plus récente à la plus ancienne. */
  readonly years = signal<number[]>([]);
  /** Année sélectionnée ; null = Toutes les années. */
  readonly year = signal<number | null>(null);
  /** Vrai une fois la liste des années chargée. */
  readonly ready = signal(false);

  /** Libellé affichable de l'année courante. */
  readonly label = computed(() => this.year() === null ? 'Toutes les années' : String(this.year()));

  private loaded = false;

  constructor(private http: HttpClient) {}

  /** Charge la liste des années une seule fois et fixe l'année par défaut. */
  load(): void {
    if (this.loaded) return;
    this.loaded = true;
    this.http.get<number[]>('/api/stats/years').subscribe({
      next: (ys) => {
        const sorted = [...ys].sort((a, b) => b - a);
        this.years.set(sorted);
        if (sorted.length > 0) this.year.set(sorted[0]); // la plus récente
        this.ready.set(true);
      },
      error: () => { this.years.set([]); this.ready.set(true); }
    });
  }

  /** Change l'année (number) ou passe à « Toutes les années » (null). */
  select(year: number | null): void {
    this.year.set(year);
  }
}
