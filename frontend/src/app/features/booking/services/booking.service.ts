import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  AvailableDays,
  BookableCategory,
  BookingConfirmation,
  BookingRequest,
  FreeTimes,
  PublicContact,
} from '../models/booking.models';

/** The public API: no token is needed (ADR 014). */
@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/public`;

  services(): Observable<BookableCategory[]> {
    return this.http.get<BookableCategory[]>(`${this.url}/services`);
  }

  /** month as "2026-10" */
  availableDays(serviceId: number, month: string): Observable<AvailableDays> {
    return this.http.get<AvailableDays>(`${this.url}/availability/days`, { params: { serviceId, month } });
  }

  freeTimes(serviceId: number, date: string): Observable<FreeTimes> {
    return this.http.get<FreeTimes>(`${this.url}/availability`, { params: { serviceId, date } });
  }

  book(request: BookingRequest): Observable<BookingConfirmation> {
    return this.http.post<BookingConfirmation>(`${this.url}/appointments`, request);
  }

  contact(): Observable<PublicContact> {
    return this.http.get<PublicContact>(`${this.url}/contact`);
  }
}
