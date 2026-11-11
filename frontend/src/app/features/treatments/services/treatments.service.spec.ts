import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { TreatmentFilters } from '../models/treatment.models';
import { TreatmentsService } from './treatments.service';

describe('TreatmentsService', () => {
  let service: TreatmentsService;
  let http: HttpTestingController;
  const api = `${environment.apiUrl}/treatments`;
  const noFilters: TreatmentFilters = { clientId: null, categoryId: null, serviceId: null, from: '', to: '' };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TreatmentsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends only the page and the size when there are no filters', () => {
    service.search(noFilters, 0, 20).subscribe();

    const request = http.expectOne((r) => r.url === api);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys().sort()).toEqual(['page', 'size']);
    request.flush({});
  });

  it('sends every filter that is set', () => {
    service
      .search({ clientId: 7, categoryId: 1, serviceId: 10, from: '2026-09-01', to: '2026-09-30' }, 2, 10)
      .subscribe();

    const request = http.expectOne((r) => r.url === api);
    const params = request.request.params;
    expect(params.get('clientId')).toBe('7');
    expect(params.get('categoryId')).toBe('1');
    expect(params.get('serviceId')).toBe('10');
    expect(params.get('from')).toBe('2026-09-01');
    expect(params.get('to')).toBe('2026-09-30');
    expect(params.get('page')).toBe('2');
    expect(params.get('size')).toBe('10');
    request.flush({});
  });

  it('creates, updates and deletes a treatment', () => {
    const body = { clientId: 7, serviceId: 10, performedOn: '2026-09-25', amount: 0, notes: null };

    service.create(body).subscribe();
    const created = http.expectOne(api);
    expect(created.request.method).toBe('POST');
    expect(created.request.body).toEqual(body);
    created.flush({});

    const { clientId: _client, ...update } = body;
    service.update(31, update).subscribe();
    const updated = http.expectOne(`${api}/31`);
    expect(updated.request.method).toBe('PUT');
    expect(updated.request.body).toEqual(update);
    updated.flush({});

    service.delete(31).subscribe();
    const deleted = http.expectOne(`${api}/31`);
    expect(deleted.request.method).toBe('DELETE');
    deleted.flush(null);
  });
});
