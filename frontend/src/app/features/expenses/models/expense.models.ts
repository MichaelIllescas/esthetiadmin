export interface ExpenseType {
  id: number;
  name: string;
}

export interface Expense {
  id: number;
  spentOn: string;
  description: string;
  amount: number;
  expenseTypeId: number;
  expenseTypeName: string;
}

export interface ExpenseRequest {
  spentOn: string;
  description: string;
  amount: number;
  expenseTypeId: number;
}

export interface ExpenseFilters {
  from: string;
  to: string;
  expenseTypeId: number | null;
}

export interface ExpenseListResponse {
  content: Expense[];
  totalAmount: number;
}
