import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { ReportsService } from './reports.service';

describe('ReportsService', () => {
  let service: ReportsService;
  let http: HttpTestingController;
  const api = `${environment.apiUrl}/reports`;
  const period = { from: '2026-09-01', to: '2026-09-30' };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ReportsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function expectGet(path: string, params: Record<string, string>) {
    const request = http.expectOne((r) => r.url === `${api}/${path}`);
    expect(request.request.method).toBe('GET');
    for (const [key, value] of Object.entries(params)) {
      expect(request.request.params.get(key)).toBe(value);
    }
    request.flush(null);
  }

  it('asks for the summary and the monthly evolution of the period', () => {
    service.summary(period).subscribe();
    expectGet('summary', { from: '2026-09-01', to: '2026-09-30' });

    service.monthly(period).subscribe();
    expectGet('monthly', { from: '2026-09-01', to: '2026-09-30' });
  });

  it('asks for the top services with a limit of ten unless told otherwise', () => {
    service.topServices(period).subscribe();
    expectGet('top-services', { from: '2026-09-01', to: '2026-09-30', limit: '10' });

    service.topServices(period, 5).subscribe();
    expectGet('top-services', { limit: '5' });
  });

  it('asks for the income by service', () => {
    service.incomeByService(period).subscribe();
    expectGet('income-by-service', { from: '2026-09-01', to: '2026-09-30' });
  });

  it('asks for the client ranking with its order', () => {
    service.topClients(period, 'treatments').subscribe();
    expectGet('top-clients', { from: '2026-09-01', to: '2026-09-30', sortBy: 'treatments', limit: '10' });

    service.topClients(period, 'amount', 3).subscribe();
    expectGet('top-clients', { sortBy: 'amount', limit: '3' });
  });
});
