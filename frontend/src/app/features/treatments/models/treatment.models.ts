export interface Treatment {
  id: number;
  clientId: number;
  clientName: string;
  serviceId: number;
  serviceName: string;
  categoryId: number | null;
  categoryName: string;
  performedOn: string;
  amount: number;
  notes: string | null;
}

export interface TreatmentRequest {
  clientId: number;
  serviceId: number;
  performedOn: string;
  amount: number;
  notes: string | null;
}

/** The client is not sent when editing: a treatment never moves to another person. */
export type TreatmentUpdateRequest = Omit<TreatmentRequest, 'clientId'>;

export interface TreatmentFilters {
  clientId: number | null;
  categoryId: number | null;
  serviceId: number | null;
  from: string;
  to: string;
}
