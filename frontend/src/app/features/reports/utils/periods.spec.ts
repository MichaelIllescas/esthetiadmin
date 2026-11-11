import { rangeProblem, resolvePreset, toIso } from './periods';

describe('periods', () => {
  const today = new Date(2026, 8, 26); // 26 September 2026

  it('writes a date as yyyy-MM-dd using the local day', () => {
    expect(toIso(new Date(2026, 0, 5))).toBe('2026-01-05');
    expect(toIso(new Date(2026, 11, 31))).toBe('2026-12-31');
  });

  it('resolves this month as the whole month', () => {
    expect(resolvePreset('this-month', today)).toEqual({ from: '2026-09-01', to: '2026-09-30' });
    expect(resolvePreset('this-month', new Date(2028, 1, 10))).toEqual({ from: '2028-02-01', to: '2028-02-29' });
  });

  it('resolves last month, also when it belongs to the previous year', () => {
    expect(resolvePreset('last-month', today)).toEqual({ from: '2026-08-01', to: '2026-08-31' });
    expect(resolvePreset('last-month', new Date(2026, 0, 15))).toEqual({ from: '2025-12-01', to: '2025-12-31' });
  });

  it('resolves this year as the whole year', () => {
    expect(resolvePreset('this-year', today)).toEqual({ from: '2026-01-01', to: '2026-12-31' });
  });

  it('resolves the last 12 months as twelve whole months ending with the current one', () => {
    expect(resolvePreset('last-12-months', today)).toEqual({ from: '2025-10-01', to: '2026-09-30' });
    expect(resolvePreset('last-12-months', new Date(2026, 1, 3))).toEqual({ from: '2025-03-01', to: '2026-02-28' });
  });

  it('accepts a valid range, including a single day', () => {
    expect(rangeProblem('2026-09-01', '2026-09-30')).toBeNull();
    expect(rangeProblem('2026-09-10', '2026-09-10')).toBeNull();
  });

  it('asks for both dates', () => {
    expect(rangeProblem('', '2026-09-30')).toContain('las dos fechas');
    expect(rangeProblem('2026-09-01', '')).toContain('las dos fechas');
  });

  it('rejects an inverted range', () => {
    expect(rangeProblem('2026-09-30', '2026-09-01')).toContain('no puede ser posterior');
  });

  it('accepts exactly five years and rejects one day more, like the API does', () => {
    expect(rangeProblem('2021-09-26', '2026-09-26')).toBeNull();
    expect(rangeProblem('2021-09-26', '2026-09-27')).toContain('5 años');
  });
});
