import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { AuthTokenStore } from '../../../core/services/auth-token-store';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
  });

  it('logs in against the API and keeps the token', () => {
    const service = TestBed.inject(AuthService);
    const tokenStore = TestBed.inject(AuthTokenStore);
    const http = TestBed.inject(HttpTestingController);

    service.login({ username: 'admin@esthetiadmin.test', password: 'secret' }).subscribe();

    const request = http.expectOne(`${environment.apiUrl}/auth/login`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ username: 'admin@esthetiadmin.test', password: 'secret' });
    request.flush({ token: 'jwt-token', expiresAt: '2026-09-26T00:00:00Z' });

    expect(tokenStore.token()).toBe('jwt-token');
    http.verify();
  });
});
