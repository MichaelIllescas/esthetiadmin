import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthTokenStore } from '../services/auth-token-store';
import { authInterceptor } from './auth.interceptor';

function tokenExpiringIn(seconds: number): string {
  const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + seconds }));
  return `header.${payload}.signature`;
}

describe('authInterceptor', () => {
  const navigate = vi.fn().mockResolvedValue(true);
  let http: HttpClient;
  let controller: HttpTestingController;
  let tokenStore: AuthTokenStore;

  beforeEach(() => {
    sessionStorage.clear();
    navigate.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigate } },
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    tokenStore = TestBed.inject(AuthTokenStore);
  });

  afterEach(() => controller.verify());

  it('adds the bearer token to API requests', () => {
    const token = tokenExpiringIn(600);
    tokenStore.set(token);

    http.get(`${environment.apiUrl}/clients`).subscribe();

    const request = controller.expectOne(`${environment.apiUrl}/clients`);
    expect(request.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    request.flush([]);
  });

  it('does not send the token to other hosts', () => {
    tokenStore.set(tokenExpiringIn(600));

    http.get('https://third-party.example.com/data').subscribe();

    const request = controller.expectOne('https://third-party.example.com/data');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });

  it('ends the session and goes to the login when the API answers 401', () => {
    tokenStore.set(tokenExpiringIn(600));

    http.get(`${environment.apiUrl}/clients`).subscribe({ error: () => undefined });
    controller
      .expectOne(`${environment.apiUrl}/clients`)
      .flush({ code: 'UNAUTHORIZED' }, { status: 401, statusText: 'Unauthorized' });

    expect(tokenStore.token()).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('leaves a failed login alone so the page can show its own message', () => {
    http.post(`${environment.apiUrl}/auth/login`, {}).subscribe({ error: () => undefined });
    controller
      .expectOne(`${environment.apiUrl}/auth/login`)
      .flush({ code: 'UNAUTHORIZED' }, { status: 401, statusText: 'Unauthorized' });

    expect(navigate).not.toHaveBeenCalled();
  });
  it('leaves the public booking API alone: no token and no trip to the login', () => {
    tokenStore.set(tokenExpiringIn(600));

    http.get(`${environment.apiUrl}/public/services`).subscribe({ error: () => undefined });
    const request = controller.expectOne(`${environment.apiUrl}/public/services`);
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(navigate).not.toHaveBeenCalled();
    expect(tokenStore.token()).not.toBeNull();
  });
});
