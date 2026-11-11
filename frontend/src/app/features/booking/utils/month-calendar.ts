/** A month laid out in weeks from Monday to Sunday; null fills the days of the neighbouring months. */
export type MonthGrid = (string | null)[][];

const monthName = new Intl.DateTimeFormat('es-AR', { month: 'long', year: 'numeric' });

function pad(value: number): string {
  return String(value).padStart(2, '0');
}

/** "2026-10" of a "yyyy-MM-dd" date. */
export function monthOf(iso: string): string {
  return iso.slice(0, 7);
}

/** The month {@code steps} months away from "2026-10". */
export function addMonths(month: string, steps: number): string {
  const [year, number] = month.split('-').map(Number);
  const date = new Date(year, number - 1 + steps, 1);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`;
}

/** "octubre de 2026" */
export function formatMonth(month: string): string {
  const [year, number] = month.split('-').map(Number);
  return monthName.format(new Date(year, number - 1, 1));
}

export function buildMonth(month: string): MonthGrid {
  const [year, number] = month.split('-').map(Number);
  const daysInMonth = new Date(year, number, 0).getDate();
  // getDay() is 0 for Sunday: shift it so the week starts on Monday.
  const leading = (new Date(year, number - 1, 1).getDay() + 6) % 7;
  const cells: (string | null)[] = Array.from({ length: leading }, () => null);
  for (let day = 1; day <= daysInMonth; day++) {
    cells.push(`${month}-${pad(day)}`);
  }
  while (cells.length % 7 !== 0) {
    cells.push(null);
  }
  const weeks: MonthGrid = [];
  for (let index = 0; index < cells.length; index += 7) {
    weeks.push(cells.slice(index, index + 7));
  }
  return weeks;
}
