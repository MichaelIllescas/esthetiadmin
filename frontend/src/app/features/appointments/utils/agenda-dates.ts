/** Date helpers for the agenda. Dates are "yyyy-MM-dd" and date-times "yyyy-MM-ddTHH:mm:ss", in the center's time. */

const weekdayAndDate = new Intl.DateTimeFormat('es-AR', { weekday: 'long', day: 'numeric', month: 'long' });

/** Builds the date from its parts: parsing "2026-10-08" directly would shift it by the time zone. */
function fromIso(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function toIso(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function addDays(iso: string, days: number): string {
  const date = fromIso(iso);
  date.setDate(date.getDate() + days);
  return toIso(date);
}

/** The Monday of the week of {@code iso}: the agenda's weeks go from Monday to Sunday. */
export function mondayOf(iso: string): string {
  const weekday = fromIso(iso).getDay();
  return addDays(iso, weekday === 0 ? -6 : 1 - weekday);
}

/** Every day from {@code from} to {@code to}, both included. */
export function daysBetween(from: string, to: string): string[] {
  const days: string[] = [];
  for (let day = from; day <= to; day = addDays(day, 1)) {
    days.push(day);
  }
  return days;
}

/** "jueves, 8 de octubre" */
export function formatWeekday(iso: string): string {
  return weekdayAndDate.format(fromIso(iso));
}

/** "2026-10-08T10:30:00" → "10:30" */
export function timeOf(dateTime: string): string {
  return dateTime.slice(11, 16);
}

/** "2026-10-08T10:30:00" → "2026-10-08" */
export function dateOf(dateTime: string): string {
  return dateTime.slice(0, 10);
}
