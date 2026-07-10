import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EntryByDay, EventDetail, EventRollup, Gate, Overview, TicketTypes,
  RecetteSummary, RecetteEventHeader, RecetteModelRow,
  RecetteGuichetSummary, RecetteGuichetDetail, TourniquetEvent, RejetsData
} from './models';

/**
 * One typed method per stats endpoint. All GET, all read-only.
 *
 * Toutes les statistiques portent sur l'intégralité de la base : aucun filtre
 * par année. Les endpoints à cache court acceptent `refresh=true` (bouton
 * « Actualiser ») pour contourner ce cache.
 */
@Injectable({ providedIn: 'root' })
export class StatsService {
  constructor(private http: HttpClient) {}

  private refreshOpts(refresh: boolean): { params?: HttpParams } {
    return refresh ? { params: new HttpParams().set('refresh', 'true') } : {};
  }

  overview(): Observable<Overview> {
    return this.http.get<Overview>('/api/stats/overview');
  }
  entriesByDay(): Observable<EntryByDay[]> {
    return this.http.get<EntryByDay[]>('/api/stats/entries-by-day');
  }
  gate(): Observable<Gate> {
    return this.http.get<Gate>('/api/stats/gate');
  }
  ticketTypes(): Observable<TicketTypes> {
    return this.http.get<TicketTypes>('/api/stats/ticket-types');
  }
  events(): Observable<EventRollup[]> {
    return this.http.get<EventRollup[]>('/api/stats/events');
  }
  eventDetail(id: number): Observable<EventDetail> {
    return this.http.get<EventDetail>(`/api/stats/events/${id}`);
  }

  // ---- Recette ----
  // `refresh=true` (le bouton « Actualiser ») contourne le cache court du serveur.
  recetteSummary(refresh = false): Observable<RecetteSummary[]> {
    return this.http.get<RecetteSummary[]>('/api/stats/recette/summary', this.refreshOpts(refresh));
  }
  /** Détaillée: the collapsible panel headers (per-event totals). */
  recetteDetailHeaders(refresh = false): Observable<RecetteEventHeader[]> {
    return this.http.get<RecetteEventHeader[]>('/api/stats/recette/detail', this.refreshOpts(refresh));
  }
  /** Détaillée: per-model rows for one event, fetched when its panel expands. */
  recetteDetailRows(eventId: number): Observable<RecetteModelRow[]> {
    return this.http.get<RecetteModelRow[]>(`/api/stats/recette/detail/${eventId}`);
  }

  // ---- Recette par guichet (§5.2) ----
  recetteGuichetSummary(): Observable<RecetteGuichetSummary[]> {
    return this.http.get<RecetteGuichetSummary[]>('/api/stats/recette/guichet/summary');
  }
  recetteGuichetDetail(): Observable<RecetteGuichetDetail[]> {
    return this.http.get<RecetteGuichetDetail[]>('/api/stats/recette/guichet/detail');
  }

  // ---- Statistique des tourniquets (§5.3) ----
  tourniquets(refresh = false): Observable<TourniquetEvent[]> {
    return this.http.get<TourniquetEvent[]>('/api/stats/tourniquets', this.refreshOpts(refresh));
  }

  // ---- Analyse des rejets (Part C) ----
  rejets(refresh = false): Observable<RejetsData> {
    return this.http.get<RejetsData>('/api/stats/rejets', this.refreshOpts(refresh));
  }
}
