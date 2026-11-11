export type AppointmentStatus = 'CONFIRMED' | 'CANCELLED' | 'ATTENDED' | 'NO_SHOW';
export type AppointmentSource = 'PUBLIC' | 'ADMIN';

/** startsAt / endsAt come as "2026-10-08T10:30:00", in the center's time. */
export interface Appointment {
  id: number;
  serviceId: number;
  serviceName: string;
  categoryName: string;
  startsAt: string;
  endsAt: string;
  firstName: string;
  lastName: string;
  phone: string;
  clientId: number | null;
  status: AppointmentStatus;
  source: AppointmentSource;
  notes: string | null;
  /** Only for notices about this appointment (ADR 016). */
  email: string | null;
}

/** With a clientId, the API fills the missing name, phone and email from the client. */
export interface AppointmentRequest {
  serviceId: number;
  startsAt: string;
  clientId: number | null;
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  notes: string | null;
  email: string | null;
}

/** The emails the owner can send a client about her appointment. */
export type NoticeType = 'BOOKED' | 'RESCHEDULED' | 'CANCELLED';

export interface DayAvailability {
  date: string;
  durationMinutes: number;
  times: string[];
}

export const STATUS_LABELS: Record<AppointmentStatus, string> = {
  CONFIRMED: 'Confirmado',
  CANCELLED: 'Cancelado',
  ATTENDED: 'Atendido',
  NO_SHOW: 'No vino',
};
