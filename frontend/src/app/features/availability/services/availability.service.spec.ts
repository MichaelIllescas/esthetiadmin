import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { AvailabilityService } from './availability.service';

describe('AvailabilityService', () => {
  const api = environment.apiUrl;
  let service: AvailabilityService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AvailabilityService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('reads and replaces the weekly schedule', () => {
    service.getRules().subscribe();
    expect(http.expectOne(`${api}/availability-rules`).request.method).toBe('GET');

    const week = [{ dayOfWeek: 1 as const, startTime: '09:00', endTime: '13:00' }];
    service.saveRules(week).subscribe();
    const put = http.expectOne(`${api}/availability-rules`);
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual(week);
  });

  it('lists blocks with or without a range', () => {
    service.getBlocks().subscribe();
    expect(http.expectOne(`${api}/availability-blocks`).request.params.keys()).toEqual([]);

    service.getBlocks('2026-10-01', '2026-10-31').subscribe();
    http.expectOne(`${api}/availability-blocks?from=2026-10-01&to=2026-10-31`);
  });

  it('creates and deletes blocks', () => {
    const request = { startDate: '2026-10-12', endDate: '2026-10-12', startTime: null, endTime: null, reason: 'Feriado' };
    service.createBlock(request).subscribe();
    const post = http.expectOne(`${api}/availability-blocks`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(request);

    service.deleteBlock(3).subscribe();
    expect(http.expectOne(`${api}/availability-blocks/3`).request.method).toBe('DELETE');
  });
});
