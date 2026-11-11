import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  ClientRanking,
  IncomeBreakdown,
  MonthlyRow,
  Period,
  Summary,
  TopClient,
  TopService,
} from '../models/report.models';

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/reports`;

  summary(period: Period): Observable<Summary> {
    return this.http.get<Summary>(`${this.url}/summary`, { params: this.range(period) });
  }

  monthly(period: Period): Observable<MonthlyRow[]> {
    return this.http.get<MonthlyRow[]>(`${this.url}/monthly`, { params: this.range(period) });
  }

  topServices(period: Period, limit = 10): Observable<TopService[]> {
    return this.http.get<TopService[]>(`${this.url}/top-services`, { params: this.range(period).set('limit', limit) });
  }

  incomeByService(period: Period): Observable<IncomeBreakdown> {
    return this.http.get<IncomeBreakdown>(`${this.url}/income-by-service`, { params: this.range(period) });
  }

  topClients(period: Period, sortBy: ClientRanking, limit = 10): Observable<TopClient[]> {
    return this.http.get<TopClient[]>(`${this.url}/top-clients`, {
      params: this.range(period).set('sortBy', sortBy).set('limit', limit),
    });
  }

  private range(period: Period): HttpParams {
    return new HttpParams().set('from', period.from).set('to', period.to);
  }
}
