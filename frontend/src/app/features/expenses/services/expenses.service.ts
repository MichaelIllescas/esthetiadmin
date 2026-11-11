import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  Expense,
  ExpenseFilters,
  ExpenseListResponse,
  ExpenseRequest,
  ExpenseType,
} from '../models/expense.models';

@Injectable({ providedIn: 'root' })
export class ExpensesService {
  private readonly http = inject(HttpClient);
  private readonly expensesUrl = `${environment.apiUrl}/expenses`;
  private readonly typesUrl = `${environment.apiUrl}/expense-types`;

  search(filters: ExpenseFilters): Observable<ExpenseListResponse> {
    let params = new HttpParams();
    if (filters.from) {
      params = params.set('from', filters.from);
    }
    if (filters.to) {
      params = params.set('to', filters.to);
    }
    if (filters.expenseTypeId !== null) {
      params = params.set('expenseTypeId', filters.expenseTypeId);
    }
    return this.http.get<ExpenseListResponse>(this.expensesUrl, { params });
  }

  getTypes(): Observable<ExpenseType[]> {
    return this.http.get<ExpenseType[]>(this.typesUrl);
  }

  create(request: ExpenseRequest): Observable<Expense> {
    return this.http.post<Expense>(this.expensesUrl, request);
  }

  update(id: number, request: ExpenseRequest): Observable<Expense> {
    return this.http.put<Expense>(`${this.expensesUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.expensesUrl}/${id}`);
  }
}
