import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthTokenStore } from './auth-token-store';
import { SessionService } from './session.service';

function tokenFor(username: string): string {
  const payload = btoa(JSON.stringify({ sub: username, exp: Math.floor(Date.now() / 1000) + 600 }));
  return `header.${payload}.signature`;
}

describe('SessionService', () => {
  const navigate = vi.fn().mockResolvedValue(true);

  beforeEach(() => {
    sessionStorage.clear();
    navigate.mockClear();
    TestBed.configureTestingModule({ providers: [{ provide: Router, useValue: { navigate } }] });
  });

  it('exposes the signed-in user from the token', () => {
    const service = TestBed.inject(SessionService);
    expect(service.username()).toBeNull();

    TestBed.inject(AuthTokenStore).set(tokenFor('admin@esthetiadmin.test'));

    expect(service.username()).toBe('admin@esthetiadmin.test');
  });

  it('clears the session and goes back to the login on logout', () => {
    const service = TestBed.inject(SessionService);
    const tokenStore = TestBed.inject(AuthTokenStore);
    tokenStore.set(tokenFor('admin@esthetiadmin.test'));

    service.logout();

    expect(tokenStore.token()).toBeNull();
    expect(service.username()).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });
});
