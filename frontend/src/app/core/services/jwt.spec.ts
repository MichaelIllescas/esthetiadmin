import { readJwtPayload } from './jwt';

describe('readJwtPayload', () => {
  it('reads the claims of a token', () => {
    const payload = btoa(JSON.stringify({ sub: 'admin@esthetiadmin.test', exp: 123 }));

    expect(readJwtPayload(`header.${payload}.signature`)).toEqual({ sub: 'admin@esthetiadmin.test', exp: 123 });
  });

  it('supports the URL-safe base64 alphabet', () => {
    const payload = btoa(JSON.stringify({ sub: '???>>>' })).replace(/\+/g, '-').replace(/\//g, '_');

    expect(readJwtPayload(`header.${payload}.signature`)?.sub).toBe('???>>>');
  });

  it('returns null for something that is not a token', () => {
    expect(readJwtPayload('not-a-jwt')).toBeNull();
    expect(readJwtPayload('a.b.c')).toBeNull();
  });
});
