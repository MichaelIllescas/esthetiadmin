import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  Appointment,
  AppointmentRequest,
  AppointmentStatus,
  DayAvailability,
  NoticeType,
} from '../models/appointment.models';

@Injectable({ providedIn: 'root' })
export class AppointmentsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/appointments`;

  /** Both days included. */
  search(from: string, to: string): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(this.url, { params: { from, to } });
  }

  /** Free start times; excludeAppointmentId frees that appointment's own time, for moving it. */
  availability(serviceId: number, date: string, excludeAppointmentId: number | null = null): Observable<DayAvailability> {
    let params = new HttpParams().set('serviceId', serviceId).set('date', date);
    if (excludeAppointmentId !== null) {
      params = params.set('excludeAppointmentId', excludeAppointmentId);
    }
    return this.http.get<DayAvailability>(`${this.url}/availability`, { params });
  }

  create(request: AppointmentRequest): Observable<Appointment> {
    return this.http.post<Appointment>(this.url, request);
  }

  update(id: number, request: AppointmentRequest): Observable<Appointment> {
    return this.http.put<Appointment>(`${this.url}/${id}`, request);
  }

  changeStatus(id: number, status: Exclude<AppointmentStatus, 'CONFIRMED'>): Observable<Appointment> {
    return this.http.patch<Appointment>(`${this.url}/${id}/status`, { status });
  }

  linkClient(id: number, clientId: number): Observable<Appointment> {
    return this.http.patch<Appointment>(`${this.url}/${id}/client`, { clientId });
  }

  /** Emails the client; only when the owner asks for it (ADR 016). */
  sendNotice(id: number, type: NoticeType): Observable<void> {
    return this.http.post<void>(`${this.url}/${id}/notifications`, { type });
  }
}
