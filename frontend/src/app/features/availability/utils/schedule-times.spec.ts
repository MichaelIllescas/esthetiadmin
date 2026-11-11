import { AvailabilityRule } from '../models/availability.models';
import { dayRangesError, formatDateRange, formatRange, HALF_HOURS, rulesByDay } from './schedule-times';

describe('schedule-times', () => {
  it('offers every half hour of the day', () => {
    expect(HALF_HOURS).toHaveLength(48);
    expect(HALF_HOURS[0]).toBe('00:00');
    expect(HALF_HOURS[19]).toBe('09:30');
    expect(HALF_HOURS[47]).toBe('23:30');
  });

  it('accepts ranges that only touch', () => {
    expect(
      dayRangesError([
        { startTime: '09:00', endTime: '13:00' },
        { startTime: '13:00', endTime: '15:00' },
      ]),
    ).toBeNull();
    expect(dayRangesError([])).toBeNull();
  });

  it('rejects overlapping ranges in any order', () => {
    expect(
      dayRangesError([
        { startTime: '15:00', endTime: '19:00' },
        { startTime: '09:00', endTime: '15:30' },
      ]),
    ).toBe('Los rangos se superponen.');
  });

  it('rejects a range that does not move forward', () => {
    expect(dayRangesError([{ startTime: '13:00', endTime: '09:00' }])).toBe(
      'La hora de inicio tiene que ser anterior a la de fin.',
    );
  });

  it('formats a range', () => {
    expect(formatRange('09:00', '13:00')).toBe('09:00 a 13:00');
  });

  it('formats a block of days as short as it reads naturally', () => {
    expect(formatDateRange('2026-10-12', '2026-10-12')).toBe('12 de octubre de 2026');
    expect(formatDateRange('2027-01-04', '2027-01-15')).toBe('4 al 15 de enero de 2027');
    expect(formatDateRange('2026-10-28', '2026-11-03')).toBe('28 de octubre al 3 de noviembre de 2026');
    expect(formatDateRange('2026-12-28', '2027-01-03')).toBe('28 de diciembre de 2026 al 3 de enero de 2027');
  });

  it('groups the rules by day, keeping closed days and sorting each day', () => {
    const rules: AvailabilityRule[] = [
      { dayOfWeek: 1, startTime: '15:00', endTime: '19:00' },
      { dayOfWeek: 1, startTime: '09:00', endTime: '13:00' },
      { dayOfWeek: 6, startTime: '09:00', endTime: '12:00' },
    ];

    const byDay = rulesByDay(rules);

    expect(byDay.size).toBe(7);
    expect(byDay.get(1)?.map((rule) => rule.startTime)).toEqual(['09:00', '15:00']);
    expect(byDay.get(2)).toEqual([]);
    expect(byDay.get(6)).toHaveLength(1);
  });
});
