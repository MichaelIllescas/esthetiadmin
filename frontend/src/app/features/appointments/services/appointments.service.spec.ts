import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { AppointmentsService } from './appointments.service';

describe('AppointmentsService', () => {
  const url = `${environment.apiUrl}/appointments`;
  let service: AppointmentsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AppointmentsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists a range of the agenda', () => {
    service.search('2026-10-05', '2026-10-11').subscribe();
    http.expectOne(`${url}?from=2026-10-05&to=2026-10-11`);
  });

  it('asks for the free times, optionally ignoring one appointment', () => {
    service.availability(10, '2026-10-08').subscribe();
    http.expectOne(`${url}/availability?serviceId=10&date=2026-10-08`);

    service.availability(10, '2026-10-08', 52).subscribe();
    http.expectOne(`${url}/availability?serviceId=10&date=2026-10-08&excludeAppointmentId=52`);
  });

  it('creates, updates, changes the status and links a client', () => {
    const request = {
      serviceId: 10,
      startsAt: '2026-10-08T10:30:00',
      clientId: null,
      firstName: 'Carina',
      lastName: 'Diaz',
      phone: '3415551234',
      notes: null, email: null,
    };
    service.create(request).subscribe();
    expect(http.expectOne(url).request.body).toEqual(request);

    service.update(52, request).subscribe();
    expect(http.expectOne(`${url}/52`).request.method).toBe('PUT');

    service.changeStatus(52, 'NO_SHOW').subscribe();
    const status = http.expectOne(`${url}/52/status`);
    expect(status.request.method).toBe('PATCH');
    expect(status.request.body).toEqual({ status: 'NO_SHOW' });

    service.linkClient(52, 7).subscribe();
    expect(http.expectOne(`${url}/52/client`).request.body).toEqual({ clientId: 7 });
  });
});
