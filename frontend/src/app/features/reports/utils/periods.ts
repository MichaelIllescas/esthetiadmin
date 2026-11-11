import { Period } from '../models/report.models';

export type PresetId = 'this-month' | 'last-month' | 'this-year' | 'last-12-months';

export interface Preset {
  id: PresetId;
  label: string;
}

export const PRESETS: readonly Preset[] = [
  { id: 'this-month', label: 'Este mes' },
  { id: 'last-month', label: 'Mes pasado' },
  { id: 'this-year', label: 'Este año' },
  { id: 'last-12-months', label: 'Últimos 12 meses' },
];

/** The API does not accept a wider period (it would mean too many months and rows for no practical use). */
export const MAX_YEARS = 5;

const pad = (value: number) => String(value).padStart(2, '0');

export function toIso(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function fromIso(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

const firstDayOfMonth = (year: number, month: number) => new Date(year, month, 1);
const lastDayOfMonth = (year: number, month: number) => new Date(year, month + 1, 0);

/** The dates of a quick period, counted from `today`. Whole months: a partial one would understate its evolution. */
export function resolvePreset(id: PresetId, today: Date): Period {
  const year = today.getFullYear();
  const month = today.getMonth();
  switch (id) {
    case 'this-month':
      return { from: toIso(firstDayOfMonth(year, month)), to: toIso(lastDayOfMonth(year, month)) };
    case 'last-month':
      return { from: toIso(firstDayOfMonth(year, month - 1)), to: toIso(lastDayOfMonth(year, month - 1)) };
    case 'this-year':
      return { from: toIso(new Date(year, 0, 1)), to: toIso(new Date(year, 11, 31)) };
    case 'last-12-months':
      return { from: toIso(firstDayOfMonth(year, month - 11)), to: toIso(lastDayOfMonth(year, month)) };
  }
}

/** Why a custom range cannot be used, or null when it can. */
export function rangeProblem(from: string, to: string): string | null {
  if (!from || !to) {
    return 'Elegí las dos fechas.';
  }
  if (from > to) {
    return 'La fecha "Desde" no puede ser posterior a la fecha "Hasta".';
  }
  const limit = fromIso(from);
  limit.setFullYear(limit.getFullYear() + MAX_YEARS);
  if (fromIso(to) > limit) {
    return `El período no puede superar los ${MAX_YEARS} años.`;
  }
  return null;
}
