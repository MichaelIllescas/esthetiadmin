import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot } from '@angular/router';
import { AuthTokenStore } from '../services/auth-token-store';
import { authGuard, guestGuard } from './auth.guard';

function tokenExpiringIn(seconds: number): string {
  const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + seconds }));
  return `header.${payload}.signature`;
}

describe('route guards', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = {} as RouterStateSnapshot;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  it('authGuard redirects to the login when there is no session', () => {
    const result = TestBed.runInInjectionContext(() => authGuard(route, state));

    expect(TestBed.inject(Router).serializeUrl(result as never)).toBe('/login');
  });

  it('authGuard allows a signed-in user', () => {
    TestBed.inject(AuthTokenStore).set(tokenExpiringIn(600));

    expect(TestBed.runInInjectionContext(() => authGuard(route, state))).toBe(true);
  });

  it('guestGuard allows an anonymous user to see the login', () => {
    expect(TestBed.runInInjectionContext(() => guestGuard(route, state))).toBe(true);
  });

  it('guestGuard sends a signed-in user to the home page', () => {
    TestBed.inject(AuthTokenStore).set(tokenExpiringIn(600));

    const result = TestBed.runInInjectionContext(() => guestGuard(route, state));

    expect(TestBed.inject(Router).serializeUrl(result as never)).toBe('/');
  });
});
