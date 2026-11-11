import { TestBed } from '@angular/core/testing';
import { AuthTokenStore } from './auth-token-store';

const STORAGE_KEY = 'esthetiadmin.auth.token';

function tokenExpiringIn(seconds: number): string {
  const payload = btoa(JSON.stringify({ sub: 'admin', exp: Math.floor(Date.now() / 1000) + seconds }));
  return `header.${payload}.signature`;
}

describe('AuthTokenStore', () => {
  beforeEach(() => sessionStorage.clear());

  it('stores and clears a token', () => {
    const store = TestBed.inject(AuthTokenStore);
    const token = tokenExpiringIn(600);

    store.set(token);
    expect(store.token()).toBe(token);
    expect(sessionStorage.getItem(STORAGE_KEY)).toBe(token);

    store.clear();
    expect(store.token()).toBeNull();
    expect(sessionStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it('restores a valid token after a reload', () => {
    const token = tokenExpiringIn(600);
    sessionStorage.setItem(STORAGE_KEY, token);

    expect(TestBed.inject(AuthTokenStore).token()).toBe(token);
  });

  it('discards an expired token on startup', () => {
    sessionStorage.setItem(STORAGE_KEY, tokenExpiringIn(-60));

    expect(TestBed.inject(AuthTokenStore).token()).toBeNull();
    expect(sessionStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it('discards a malformed token on startup', () => {
    sessionStorage.setItem(STORAGE_KEY, 'not-a-jwt');

    expect(TestBed.inject(AuthTokenStore).token()).toBeNull();
  });
});
