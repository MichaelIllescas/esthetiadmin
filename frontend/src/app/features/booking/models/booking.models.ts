/** What the public page offers: never prices. */
export interface BookableService {
  id: number;
  name: string;
  description: string | null;
}

export interface BookableCategory {
  categoryId: number;
  categoryName: string;
  durationMinutes: number;
  services: BookableService[];
}

/** A chosen service with the category data the next steps show. */
export interface ChosenService extends BookableService {
  categoryName: string;
  durationMinutes: number;
}

export interface AvailableDays {
  month: string;
  days: string[];
}

export interface FreeTimes {
  date: string;
  durationMinutes: number;
  times: string[];
}

export interface BookingRequest {
  serviceId: number;
  startsAt: string;
  firstName: string;
  lastName: string;
  phone: string;
  /** Optional, only to get the confirmation (ADR 016). */
  email: string | null;
}

export interface BookingConfirmation {
  serviceName: string;
  startsAt: string;
  endsAt: string;
  firstName: string;
  /** The confirmation email was asked for; it does not mean it arrived. */
  emailSent: boolean;
}

/** The center's phone to cancel or change a booking; null when the owner has not set it. */
export interface PublicContact {
  phone: string | null;
}

/** "30 minutos" / "1 hora" */
export function durationLabel(minutes: number): string {
  return minutes === 60 ? '1 hora' : `${minutes} minutos`;
}
