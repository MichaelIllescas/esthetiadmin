import { addMonths, buildMonth, formatMonth, monthOf } from './month-calendar';

describe('month-calendar', () => {
  it('lays out a month in weeks that start on Monday', () => {
    // October 2026 starts on a Thursday and has 31 days.
    const weeks = buildMonth('2026-10');

    expect(weeks).toHaveLength(5);
    expect(weeks[0]).toEqual([null, null, null, '2026-10-01', '2026-10-02', '2026-10-03', '2026-10-04']);
    expect(weeks[4]).toEqual(['2026-10-26', '2026-10-27', '2026-10-28', '2026-10-29', '2026-10-30', '2026-10-31', null]);
  });

  it('handles a month that starts on Sunday', () => {
    // November 2026 starts on a Sunday.
    expect(buildMonth('2026-11')[0]).toEqual([null, null, null, null, null, null, '2026-11-01']);
  });

  it('moves between months and years', () => {
    expect(addMonths('2026-12', 1)).toBe('2027-01');
    expect(addMonths('2026-01', -1)).toBe('2025-12');
    expect(monthOf('2026-10-08')).toBe('2026-10');
  });

  it('names a month', () => {
    expect(formatMonth('2026-10')).toBe('octubre de 2026');
  });
});
