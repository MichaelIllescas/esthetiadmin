/** Inclusive range of days, as yyyy-MM-dd strings (the format of the API and of the date inputs). */
export interface Period {
  from: string;
  to: string;
}

export interface Summary {
  from: string;
  to: string;
  income: number;
  expenses: number;
  balance: number;
  treatmentsCount: number;
  expensesCount: number;
}

export interface MonthlyRow {
  month: string;
  income: number;
  expenses: number;
  balance: number;
}

export interface TopService {
  serviceId: number;
  serviceName: string;
  categoryId: number;
  categoryName: string;
  treatments: number;
  income: number;
}

export interface ServiceIncome {
  serviceId: number;
  serviceName: string;
  income: number;
  percentage: number;
  treatments: number;
}

export interface CategoryIncome {
  categoryId: number;
  categoryName: string;
  income: number;
  percentage: number;
  treatments: number;
  services: ServiceIncome[];
}

export interface IncomeBreakdown {
  totalIncome: number;
  categories: CategoryIncome[];
}

export interface TopClient {
  clientId: number;
  clientName: string;
  treatments: number;
  totalAmount: number;
}

export type ClientRanking = 'amount' | 'treatments';
