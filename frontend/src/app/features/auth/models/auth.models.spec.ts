import type { LoginCredentials, LoginResponse } from './auth.models';

describe('auth models', () => {
  it('represent the login API contract', () => {
    const credentials: LoginCredentials = { username: 'admin', password: 'secret' };
    const response: LoginResponse = {
      token: 'token',
      expiresAt: '2026-09-26T00:00:00Z',
    };

    expect(credentials.username).toBe('admin');
    expect(response.token).toBe('token');
  });
});
