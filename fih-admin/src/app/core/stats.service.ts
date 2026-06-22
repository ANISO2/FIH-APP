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
 * Year filter (3.2): every dashboard method accepts an optional `year`. Pass
 * `null` (or omit) for "Toutes les années"; pass a number to filter to that
 * edition. The parameter is only appended to the URL when it is a real number,
 * so the "all years" case sends no `year` param and the backend reproduces the
 * original numbers.
 */
@Injectable({ providedIn: 'root' })
export class StatsService {
  constructor(private http: HttpClient) {}

  private withYear(year: number | null | undefined, refresh = false): { params?: HttpParams } {
    let params = new HttpParams();
    if (year !== null && year !== undefined) params = params.set('year', String(year));
    if (refresh) params = params.set('refresh', 'true');
    return params.keys().length ? { params } : {};
  }

  /** Distinct festival years present in the DB (most-recent first). */
  years(): Observable<number[]> { return this.http.get<number[]>('/api/stats/years'); }

  overview(year?: number | null): Observable<Overview> {
    return this.http.get<Overview>('/api/stats/overview', this.withYear(year));
  }
  entriesByDay(year?: number | null): Observable<EntryByDay[]> {
    return this.http.get<EntryByDay[]>('/api/stats/entries-by-day', this.withYear(year));
  }
  gate(year?: number | null): Observable<Gate> {
    return this.http.get<Gate>('/api/stats/gate', this.withYear(year));
  }
  ticketTypes(year?: number | null): Observable<TicketTypes> {
    return this.http.get<TicketTypes>('/api/stats/ticket-types', this.withYear(year));
  }
  events(year?: number | null): Observable<EventRollup[]> {
    return this.http.get<EventRollup[]>('/api/stats/events', this.withYear(year));
  }
  eventDetail(id: number): Observable<EventDetail> {
    return this.http.get<EventDetail>(`/api/stats/events/${id}`);
  }

  // ---- Recette ----
  // `refresh=true` (the "Actualiser" button) bypasses the short server cache.
  recetteSummary(year?: number | null, refresh = false): Observable<RecetteSummary[]> {
    return this.http.get<RecetteSummary[]>('/api/stats/recette/summary', this.withYear(year, refresh));
  }
  /** Détaillée: the collapsible panel headers (per-event totals). */
  recetteDetailHeaders(year?: number | null, refresh = false): Observable<RecetteEventHeader[]> {
    return this.http.get<RecetteEventHeader[]>('/api/stats/recette/detail', this.withYear(year, refresh));
  }
  /** Détaillée: per-model rows for one event, fetched when its panel expands. */
  recetteDetailRows(eventId: number): Observable<RecetteModelRow[]> {
    return this.http.get<RecetteModelRow[]>(`/api/stats/recette/detail/${eventId}`);
  }

  // ---- Recette par guichet (§5.2) ----
  recetteGuichetSummary(year?: number | null): Observable<RecetteGuichetSummary[]> {
    return this.http.get<RecetteGuichetSummary[]>('/api/stats/recette/guichet/summary', this.withYear(year));
  }
  recetteGuichetDetail(year?: number | null): Observable<RecetteGuichetDetail[]> {
    return this.http.get<RecetteGuichetDetail[]>('/api/stats/recette/guichet/detail', this.withYear(year));
  }

  // ---- Statistique des tourniquets (§5.3) ----
  tourniquets(year?: number | null): Observable<TourniquetEvent[]> {
    return this.http.get<TourniquetEvent[]>('/api/stats/tourniquets', this.withYear(year));
  }

  // ---- Analyse des rejets (Part C) ----
  rejets(year?: number | null): Observable<RejetsData> {
    return this.http.get<RejetsData>('/api/stats/rejets', this.withYear(year));
  }
}
