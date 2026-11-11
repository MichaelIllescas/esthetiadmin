import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { ClientsService } from './clients.service';

describe('ClientsService', () => {
  let service: ClientsService;
  let http: HttpTestingController;
  const api = `${environment.apiUrl}/clients`;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ClientsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('searches with the page, the size and the trimmed text', () => {
    service.search('  carina  ', 2, 20).subscribe();

    const request = http.expectOne((r) => r.url === api);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('search')).toBe('carina');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('20');
    request.flush({ content: [], page: 2, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('does not send an empty search', () => {
    service.search('   ', 0, 20).subscribe();

    const request = http.expectOne((r) => r.url === api);
    expect(request.request.params.has('search')).toBe(false);
    request.flush({});
  });

  it('gets, creates, updates and deletes a client', () => {
    const body = { firstName: 'Carina', lastName: 'Diaz', phone: null, birthDate: null, notes: null, email: null };

    service.get(7).subscribe();
    http.expectOne(`${api}/7`).flush({});

    service.create(body).subscribe();
    const created = http.expectOne(api);
    expect(created.request.method).toBe('POST');
    expect(created.request.body).toEqual(body);
    created.flush({});

    service.update(7, body).subscribe();
    const updated = http.expectOne(`${api}/7`);
    expect(updated.request.method).toBe('PUT');
    updated.flush({});

    service.delete(7).subscribe();
    const deleted = http.expectOne(`${api}/7`);
    expect(deleted.request.method).toBe('DELETE');
    deleted.flush(null);
  });

  it('asks whether a name exists, excluding the client being edited when there is one', () => {
    let result: boolean | undefined;
    service.nameExists('Carina', 'Diaz', 7).subscribe((exists) => (result = exists));

    const request = http.expectOne((r) => r.url === `${api}/exists`);
    expect(request.request.params.get('firstName')).toBe('Carina');
    expect(request.request.params.get('lastName')).toBe('Diaz');
    expect(request.request.params.get('excludeId')).toBe('7');
    request.flush({ exists: true });
    expect(result).toBe(true);

    service.nameExists('Carina', 'Diaz', null).subscribe();
    const second = http.expectOne((r) => r.url === `${api}/exists`);
    expect(second.request.params.has('excludeId')).toBe(false);
    second.flush({ exists: false });
  });

  it('asks for the upcoming birthdays', () => {
    service.birthdays(30).subscribe();

    const request = http.expectOne((r) => r.url === `${api}/birthdays`);
    expect(request.request.params.get('days')).toBe('30');
    request.flush([]);
  });
});
