import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Availability, BadgeItem, Page, PhotoCheck, Affectee } from './models';

/** Badge data + PDF download calls. PDFs come back as Blobs we save to disk. */
@Injectable({ providedIn: 'root' })
export class BadgeService {
  constructor(private http: HttpClient) {}

  /**
   * Set/update the "Affectée à" name for an invitation serial. This is the only
   * write call in the backoffice; it upserts into the app-owned badge_affectation
   * table via PUT /api/invitations/{numeroserie}/affectee.
   */
  saveAffectee(numeroserie: string, name: string): Observable<Affectee> {
    return this.http.put<Affectee>(
      `/api/invitations/${encodeURIComponent(numeroserie)}/affectee`, { name });
  }

  availability(eventId?: number, withPhotoCheck = false): Observable<Availability[]> {
    let url = `/api/badges/availability?withPhotoCheck=${withPhotoCheck}`;
    if (eventId != null) url += `&eventId=${eventId}`;
    return this.http.get<Availability[]>(url);
  }

  items(eventId: number, modelId: number, page: number, size: number, search?: string): Observable<Page<BadgeItem>> {
    let url = `/api/badges/items?eventId=${eventId}&modelId=${modelId}&page=${page}&size=${size}`;
    if (search) url += `&search=${encodeURIComponent(search)}`;
    return this.http.get<Page<BadgeItem>>(url);
  }

  photoCheck(eventId: number, modelId: number): Observable<PhotoCheck> {
    return this.http.get<PhotoCheck>(`/api/badges/photo-check?eventId=${eventId}&modelId=${modelId}`);
  }

  // observe: 'response' so we can read the Content-Disposition filename header.
  single(type: string, code: string): Observable<HttpResponse<Blob>> {
    return this.http.get(`/api/badges/single?type=${type}&code=${encodeURIComponent(code)}`,
      { observe: 'response', responseType: 'blob' });
  }

  batch(eventId: number, modelId: number, codes: string[] | null, layout: 'single' | 'sheet'): Observable<HttpResponse<Blob>> {
    return this.http.post(`/api/badges/batch?layout=${layout}`,
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
