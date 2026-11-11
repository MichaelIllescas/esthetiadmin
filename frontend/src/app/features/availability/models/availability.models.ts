/** 1 = Monday ... 7 = Sunday, as the API sends it. */
export type DayOfWeek = 1 | 2 | 3 | 4 | 5 | 6 | 7;

/** A range of opening hours; times are "HH:mm" on the 30-minute grid. */
export interface AvailabilityRule {
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
}

export interface AffectedAppointment {
  id: number;
  startsAt: string;
  firstName: string;
  lastName: string;
}

/** From startDate to endDate, both included. Without hours it takes whole days; hours only on a single day. */
export interface AvailabilityBlock {
  id: number;
  startDate: string;
  endDate: string;
  startTime: string | null;
  endTime: string | null;
  reason: string | null;
  affectedAppointments: AffectedAppointment[];
}

export interface AvailabilityBlockRequest {
  startDate: string;
  endDate: string;
  startTime: string | null;
  endTime: string | null;
  reason: string | null;
}

export const WEEK_DAYS: readonly { day: DayOfWeek; name: string }[] = [
  { day: 1, name: 'Lunes' },
  { day: 2, name: 'Martes' },
  { day: 3, name: 'Miércoles' },
  { day: 4, name: 'Jueves' },
  { day: 5, name: 'Viernes' },
  { day: 6, name: 'Sábado' },
  { day: 7, name: 'Domingo' },
];
