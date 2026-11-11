export interface Client {
  id: number;
  firstName: string;
  lastName: string;
  phone: string | null;
  birthDate: string | null;
  age: number | null;
  notes: string | null;
  /** Only for appointment notices (ADR 016). */
  email: string | null;
}

export interface ClientRequest {
  firstName: string;
  lastName: string;
  phone: string | null;
  birthDate: string | null;
  notes: string | null;
  email: string | null;
}

export interface Birthday {
  id: number;
  firstName: string;
  lastName: string;
  birthDate: string;
  nextBirthday: string;
  turningAge: number;
  daysUntil: number;
}
