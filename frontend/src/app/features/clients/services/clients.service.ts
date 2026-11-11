import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Page } from '../../../shared/models/page.model';
import { Birthday, Client, ClientRequest } from '../models/client.models';

@Injectable({ providedIn: 'root' })
export class ClientsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/clients`;

  search(search: string, page: number, size: number): Observable<Page<Client>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<Page<Client>>(this.url, { params });
  }

  get(id: number): Observable<Client> {
    return this.http.get<Client>(`${this.url}/${id}`);
  }

  create(request: ClientRequest): Observable<Client> {
    return this.http.post<Client>(this.url, request);
  }

  update(id: number, request: ClientRequest): Observable<Client> {
    return this.http.put<Client>(`${this.url}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  /** Whether another client already has this first and last name. */
  nameExists(firstName: string, lastName: string, excludeId: number | null): Observable<boolean> {
    let params = new HttpParams().set('firstName', firstName).set('lastName', lastName);
    if (excludeId !== null) {
      params = params.set('excludeId', excludeId);
    }
    return this.http.get<{ exists: boolean }>(`${this.url}/exists`, { params }).pipe(map((response) => response.exists));
  }

  birthdays(days = 30): Observable<Birthday[]> {
    return this.http.get<Birthday[]>(`${this.url}/birthdays`, { params: new HttpParams().set('days', days) });
  }
}
