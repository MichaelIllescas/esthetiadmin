import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { ExpenseRequest } from '../models/expense.models';
import { ExpensesService } from './expenses.service';

describe('ExpensesService', () => {
  let service: ExpensesService;
  let http: HttpTestingController;
  const api = `${environment.apiUrl}/expenses`;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ExpensesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('searches without query parameters when no filter is active', () => {
    service.search({ from: '', to: '', expenseTypeId: null }).subscribe();

    const request = http.expectOne(api);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual([]);
    request.flush({ content: [], totalAmount: 0 });
  });

  it('sends every active filter', () => {
    service.search({ from: '2026-09-01', to: '2026-09-30', expenseTypeId: 3 }).subscribe();

    const request = http.expectOne((candidate) => candidate.url === api);
    expect(request.request.params.get('from')).toBe('2026-09-01');
    expect(request.request.params.get('to')).toBe('2026-09-30');
    expect(request.request.params.get('expenseTypeId')).toBe('3');
    request.flush({ content: [], totalAmount: 0 });
  });

  it('loads types and creates, updates and deletes expenses', () => {
    const body: ExpenseRequest = {
      spentOn: '2026-09-25',
      description: 'Algodón',
      amount: 1250.5,
      expenseTypeId: 3,
    };

    service.getTypes().subscribe();
    const types = http.expectOne(`${environment.apiUrl}/expense-types`);
    expect(types.request.method).toBe('GET');
    types.flush([]);

    service.create(body).subscribe();
    const created = http.expectOne(api);
    expect(created.request.method).toBe('POST');
    expect(created.request.body).toEqual(body);
    created.flush({});

    service.update(8, body).subscribe();
    const updated = http.expectOne(`${api}/8`);
    expect(updated.request.method).toBe('PUT');
    expect(updated.request.body).toEqual(body);
    updated.flush({});

    service.delete(8).subscribe();
    const deleted = http.expectOne(`${api}/8`);
    expect(deleted.request.method).toBe('DELETE');
    deleted.flush(null);
  });
});
