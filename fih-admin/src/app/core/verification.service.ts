import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PageResult, BilletSearchRow, VoucherSearchRow, TicketDetails, SearchField, SearchMode
} from './models';

/**
 * Backoffice "Vérification" (3.2). All GET, all read-only, all under
 * /api/stats/verification (admin-only). Search hits an indexed column only;
 * pagination is server-side. Details are fetched lazily when a row is opened.
 */
@Injectable({ providedIn: 'root' })
export class VerificationService {
  private readonly base = '/api/stats/verification';

  constructor(private http: HttpClient) {}

  private params(value: string, field: SearchField, mode: SearchMode, page: number, size: number): HttpParams {
    return new HttpParams()
      .set('value', value)
      .set('field', field)
      .set('mode', mode)
      .set('page', String(page))
      .set('size', String(size));
  }

  searchBillets(value: string, field: SearchField, mode: SearchMode, page: number, size: number):
      Observable<PageResult<BilletSearchRow>> {
    return this.http.get<PageResult<BilletSearchRow>>(`${this.base}/billets`,
      { params: this.params(value, field, mode, page, size) });
  }

  billetDetails(numeroserie: string): Observable<TicketDetails> {
    return this.http.get<TicketDetails>(`${this.base}/billets/${encodeURIComponent(numeroserie)}/details`);
  }

  searchVouchers(value: string, field: SearchField, mode: SearchMode, page: number, size: number):
      Observable<PageResult<VoucherSearchRow>> {
    return this.http.get<PageResult<VoucherSearchRow>>(`${this.base}/vouchers`,
      { params: this.params(value, field, mode, page, size) });
  }

  voucherDetails(numeroserie: string): Observable<TicketDetails> {
    return this.http.get<TicketDetails>(`${this.base}/vouchers/${encodeURIComponent(numeroserie)}/details`);
  }
}
