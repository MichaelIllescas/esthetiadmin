export interface Offering {
  id: number;
  categoryId: number;
  name: string;
  description: string | null;
  price: number;
  active: boolean;
  /** Offered on the public booking page. */
  bookable: boolean;
}

export interface Category {
  id: number;
  name: string;
  active: boolean;
  /** Length of the appointments of its services. */
  appointmentMinutes: AppointmentMinutes;
  services: Offering[];
}

export type AppointmentMinutes = 30 | 60;

export const APPOINTMENT_DURATIONS: readonly { minutes: AppointmentMinutes; label: string }[] = [
  { minutes: 30, label: '30 minutos' },
  { minutes: 60, label: '1 hora' },
];

export interface CategoryRequest {
  name: string;
  appointmentMinutes: AppointmentMinutes;
}

export interface OfferingRequest {
  categoryId: number;
  name: string;
  description: string | null;
  price: number;
  bookable: boolean;
}
