import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Page } from '../../../shared/models/page.model';
import { Treatment, TreatmentFilters, TreatmentRequest, TreatmentUpdateRequest } from '../models/treatment.models';

@Injectable({ providedIn: 'root' })
export class TreatmentsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/treatments`;

  search(filters: TreatmentFilters, page: number, size: number): Observable<Page<Treatment>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters.clientId !== null) {
      params = params.set('clientId', filters.clientId);
    }
    if (filters.categoryId !== null) {
      params = params.set('categoryId', filters.categoryId);
    }
    if (filters.serviceId !== null) {
      params = params.set('serviceId', filters.serviceId);
    }
    if (filters.from) {
      params = params.set('from', filters.from);
    }
    if (filters.to) {
      params = params.set('to', filters.to);
    }
    return this.http.get<Page<Treatment>>(this.url, { params });
  }

  create(request: TreatmentRequest): Observable<Treatment> {
    return this.http.post<Treatment>(this.url, request);
  }

  update(id: number, request: TreatmentUpdateRequest): Observable<Treatment> {
    return this.http.put<Treatment>(`${this.url}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
