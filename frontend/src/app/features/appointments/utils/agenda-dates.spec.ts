import { addDays, dateOf, daysBetween, formatWeekday, mondayOf, timeOf } from './agenda-dates';

describe('agenda-dates', () => {
  it('adds days across months and years', () => {
    expect(addDays('2026-10-31', 1)).toBe('2026-11-01');
    expect(addDays('2027-01-01', -1)).toBe('2026-12-31');
  });

  it('finds the Monday of any day of the week, Sunday included', () => {
    expect(mondayOf('2026-10-08')).toBe('2026-10-05');
    expect(mondayOf('2026-10-05')).toBe('2026-10-05');
    expect(mondayOf('2026-10-11')).toBe('2026-10-05');
  });

  it('lists every day of a range', () => {
    expect(daysBetween('2026-10-05', '2026-10-07')).toEqual(['2026-10-05', '2026-10-06', '2026-10-07']);
  });

  it('formats a day with its weekday', () => {
    expect(formatWeekday('2026-10-08')).toBe('jueves, 8 de octubre');
  });

  it('splits a date-time', () => {
    expect(timeOf('2026-10-08T10:30:00')).toBe('10:30');
    expect(dateOf('2026-10-08T10:30:00')).toBe('2026-10-08');
  });
});
