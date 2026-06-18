import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EntryByDay, EventDetail, EventRollup, Gate, Overview, TicketTypes,
  RecetteSummary, RecetteDetail
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

  private withYear(year: number | null | undefined): { params?: HttpParams } {
    return (year === null || year === undefined)
      ? {}
      : { params: new HttpParams().set('year', String(year)) };
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
  recetteSummary(year?: number | null): Observable<RecetteSummary[]> {
    return this.http.get<RecetteSummary[]>('/api/stats/recette/summary', this.withYear(year));
  }
  recetteDetail(year?: number | null): Observable<RecetteDetail[]> {
    return this.http.get<RecetteDetail[]>('/api/stats/recette/detail', this.withYear(year));
  }
}
