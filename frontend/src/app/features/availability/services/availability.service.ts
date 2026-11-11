import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AvailabilityBlock, AvailabilityBlockRequest, AvailabilityRule } from '../models/availability.models';

@Injectable({ providedIn: 'root' })
export class AvailabilityService {
  private readonly http = inject(HttpClient);
  private readonly rulesUrl = `${environment.apiUrl}/availability-rules`;
  private readonly blocksUrl = `${environment.apiUrl}/availability-blocks`;

  getRules(): Observable<AvailabilityRule[]> {
    return this.http.get<AvailabilityRule[]>(this.rulesUrl);
  }

  /** Sends the whole week: days left out stay closed. */
  saveRules(rules: readonly AvailabilityRule[]): Observable<AvailabilityRule[]> {
    return this.http.put<AvailabilityRule[]>(this.rulesUrl, rules);
  }

  /** Without dates the API lists the blocks from today on. */
  getBlocks(from?: string, to?: string): Observable<AvailabilityBlock[]> {
    let params = new HttpParams();
    if (from) {
      params = params.set('from', from);
    }
    if (to) {
      params = params.set('to', to);
    }
    return this.http.get<AvailabilityBlock[]>(this.blocksUrl, { params });
  }

  createBlock(request: AvailabilityBlockRequest): Observable<AvailabilityBlock> {
    return this.http.post<AvailabilityBlock>(this.blocksUrl, request);
  }

  deleteBlock(id: number): Observable<void> {
    return this.http.delete<void>(`${this.blocksUrl}/${id}`);
  }
}
