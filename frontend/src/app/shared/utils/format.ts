const format = (fractionDigits: number) =>
  new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: 2,
  });

const wholeAmount = format(0);
const withCents = format(2);

/** Whole amounts show no decimals ($ 15.000); anything with cents shows both ($ 1.500,50). */
export function formatPrice(value: number): string {
  return Number.isInteger(value) ? wholeAmount.format(value) : withCents.format(value);
}

const fullDate = new Intl.DateTimeFormat('es-AR', { day: 'numeric', month: 'long', year: 'numeric' });
const dayAndMonth = new Intl.DateTimeFormat('es-AR', { day: 'numeric', month: 'long' });
const shortDate = new Intl.DateTimeFormat('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' });

/** Builds the date from its parts: parsing "2026-09-25" directly would shift it by the time zone. */
function fromIso(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

/** "3 de noviembre de 1985" */
export function formatDate(iso: string): string {
  return fullDate.format(fromIso(iso));
}

/** "03/11/1985" */
export function formatShortDate(iso: string): string {
  return shortDate.format(fromIso(iso));
}

/** "3 de noviembre" */
export function formatDayAndMonth(iso: string): string {
  return dayAndMonth.format(fromIso(iso));
}

/** Today in the user's time zone as yyyy-MM-dd, the format of the date inputs and the API. */
export function todayIso(): string {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

const monthYear = new Intl.DateTimeFormat('es-AR', { month: 'short', year: 'numeric' });
const percent = new Intl.NumberFormat('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const compact = new Intl.NumberFormat('es-AR', { notation: 'compact', maximumFractionDigits: 1 });

/** "2026-09" → "sep 2026" */
export function formatMonth(month: string): string {
  const [year, number] = month.split('-').map(Number);
  return monthYear.format(new Date(year, number - 1, 1)).replace('.', '');
}

/** 54.545 → "54,55 %" */
export function formatPercent(value: number): string {
  return `${percent.format(value)} %`;
}

/** 1500000 → "$ 1,5 M": for the axes of a chart, where the full amount would not fit. */
export function formatCompactPrice(value: number): string {
  return `${value < 0 ? '-' : ''}$ ${compact.format(Math.abs(value))}`;
}
