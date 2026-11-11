import { EMAIL_PATTERN, whatsappUrl } from './contact';

describe('contact utils', () => {
  it('adds the country code to local mobiles for WhatsApp', () => {
    expect(whatsappUrl('3415559876')).toBe('https://wa.me/5493415559876');
    expect(whatsappUrl('549 341 555-9876')).toBe('https://wa.me/5493415559876');
  });

  it('accepts a plain email address only', () => {
    expect(EMAIL_PATTERN.test('carina@mail.com')).toBe(true);
    expect(EMAIL_PATTERN.test('carina@mail')).toBe(false);
    expect(EMAIL_PATTERN.test('carina mail.com')).toBe(false);
  });
});
