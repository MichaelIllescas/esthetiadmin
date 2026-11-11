import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { BookingService } from './booking.service';

describe('BookingService', () => {
  const url = `${environment.apiUrl}/public`;
  let service: BookingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(BookingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('reads the services, the days of a month and the free times of a day', () => {
    service.services().subscribe();
    http.expectOne(`${url}/services`);

    service.availableDays(10, '2026-10').subscribe();
    http.expectOne(`${url}/availability/days?serviceId=10&month=2026-10`);

    service.freeTimes(10, '2026-10-08').subscribe();
    http.expectOne(`${url}/availability?serviceId=10&date=2026-10-08`);
  });

  it('books', () => {
    const request = { serviceId: 10, startsAt: '2026-10-08T10:00:00', firstName: 'Carina', lastName: 'Diaz', phone: '3415551234', email: null };
    service.book(request).subscribe();

    const post = http.expectOne(`${url}/appointments`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(request);
  });
});
