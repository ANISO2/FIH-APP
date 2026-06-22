import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Availability, BadgeItem, Page, Affectee,
  MissingPoster, LotRequest, LotPreview, LotResult
} from './models';

/** Badge data + PDF download calls. PDFs come back as Blobs we save to disk. */
@Injectable({ providedIn: 'root' })
export class BadgeService {
  constructor(private http: HttpClient) {}

  /**
   * Set the "Affectée à" name for ONE invitation serial (Change B). One-time:
   * the backend returns 409 if the serial is already assigned.
   */
  saveAffectee(numeroserie: string, name: string): Observable<Affectee> {
    return this.http.put<Affectee>(
      `/api/invitations/${encodeURIComponent(numeroserie)}/affectee`, { name });
  }

  // ---- Change C: lot (affectation par plage de séries) ----
  /** Dry-run a lot: matched invitations, proposed names, conflicts/warnings. */
  lotPreview(req: LotRequest): Observable<LotPreview> {
    return this.http.post<LotPreview>('/api/invitations/affectation/lot/preview', req);
  }
  /** Assign a whole lot immutably (409 if any serial in range is already named). */
  lotAssign(req: LotRequest): Observable<LotResult> {
    return this.http.post<LotResult>('/api/invitations/affectation/lot', req);
  }
  /** Download the CSV manifest (nom, numeroserie, codebarre, evenement) for a range. */
  lotManifest(startSerie: string, endSerie: string): Observable<HttpResponse<Blob>> {
    const qs = `startSerie=${encodeURIComponent(startSerie)}&endSerie=${encodeURIComponent(endSerie)}`;
    return this.http.get(`/api/invitations/affectation/lot/manifest?${qs}`,
      { observe: 'response', responseType: 'blob' });
  }

  availability(eventId?: number): Observable<Availability[]> {
    let url = `/api/badges/availability`;
    if (eventId != null) url += `?eventId=${eventId}`;
    return this.http.get<Availability[]>(url);
  }

  /** §6 — events with invitations but no poster file yet. */
  missingPosters(): Observable<MissingPoster[]> {
    return this.http.get<MissingPoster[]>('/api/badges/posters/missing');
  }

  items(eventId: number, modelId: number, page: number, size: number, search?: string): Observable<Page<BadgeItem>> {
    let url = `/api/badges/items?eventId=${eventId}&modelId=${modelId}&page=${page}&size=${size}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    return this.http.get<Page<BadgeItem>>(url);
  }

  // observe: 'response' so we can read the Content-Disposition filename header.
  single(type: string, code: string): Observable<HttpResponse<Blob>> {
    return this.http.get(`/api/badges/single?type=${type}&code=${encodeURIComponent(code)}`,
      { observe: 'response', responseType: 'blob' });
  }

  // Change B: the « Planche A4 » layout is gone — batch always produces one
  // ticket per page, so there is no longer a layout parameter.
  batch(eventId: number, modelId: number, codes: string[] | null): Observable<HttpResponse<Blob>> {
    return this.http.post(`/api/badges/batch`,
      { eventId, modelId, codes: codes && codes.length ? codes : null },
      { observe: 'response', responseType: 'blob' });
  }

  /** Trigger a browser download from a Blob response, using the server filename if present. */
  saveResponse(res: HttpResponse<Blob>, fallbackName: string): void {
    const blob = res.body!;
    const cd = res.headers.get('Content-Disposition') || '';
    const match = /filename="?([^"]+)"?/.exec(cd);
    const name = match ? match[1] : fallbackName;
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = name;
    a.click();
    URL.revokeObjectURL(url);
  }
}
