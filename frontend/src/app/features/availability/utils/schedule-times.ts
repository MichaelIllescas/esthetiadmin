import { formatDate, formatDayAndMonth } from '../../../shared/utils/format';
import { AvailabilityRule, DayOfWeek } from '../models/availability.models';

/** Every half hour of the day, "00:00" ... "23:30": the only times the schedule accepts. */
export const HALF_HOURS: readonly string[] = Array.from({ length: 48 }, (_, index) => {
  const hours = String(Math.floor(index / 2)).padStart(2, '0');
  return `${hours}:${index % 2 === 0 ? '00' : '30'}`;
});

/**
 * Checks one day's ranges the same way the API does: each start before its end and no overlaps.
 * Returns the problem to show, or null when the day is fine. Times are "HH:mm", so they compare as text.
 */
export function dayRangesError(ranges: readonly Pick<AvailabilityRule, 'startTime' | 'endTime'>[]): string | null {
  if (ranges.some((range) => range.startTime >= range.endTime)) {
    return 'La hora de inicio tiene que ser anterior a la de fin.';
  }
  const sorted = [...ranges].sort((a, b) => a.startTime.localeCompare(b.startTime));
  for (let index = 1; index < sorted.length; index++) {
    if (sorted[index].startTime < sorted[index - 1].endTime) {
      return 'Los rangos se superponen.';
    }
  }
  return null;
}

/** "09:00 a 13:00" */
export function formatRange(startTime: string, endTime: string): string {
  return `${startTime} a ${endTime}`;
}

/**
 * A block's days, as short as reads naturally: "12 de octubre de 2026", "4 al 15 de enero de 2027",
 * "28 de octubre al 3 de noviembre de 2026" or, across years, both full dates.
 */
export function formatDateRange(startDate: string, endDate: string): string {
  if (startDate === endDate) {
    return formatDate(startDate);
  }
  const [startYear, startMonth, startDay] = startDate.split('-');
  const [endYear, endMonth] = endDate.split('-');
  if (startYear !== endYear) {
    return `${formatDate(startDate)} al ${formatDate(endDate)}`;
  }
  const start = startMonth === endMonth ? String(Number(startDay)) : formatDayAndMonth(startDate);
  return `${start} al ${formatDate(endDate)}`;
}

/** Groups the API's flat list by weekday, every day present (empty = closed), ranges in order. */
export function rulesByDay(rules: readonly AvailabilityRule[]): Map<DayOfWeek, AvailabilityRule[]> {
  const byDay = new Map<DayOfWeek, AvailabilityRule[]>(
    ([1, 2, 3, 4, 5, 6, 7] as DayOfWeek[]).map((day) => [day, []]),
  );
  for (const rule of [...rules].sort((a, b) => a.startTime.localeCompare(b.startTime))) {
    byDay.get(rule.dayOfWeek)?.push(rule);
  }
  return byDay;
}
