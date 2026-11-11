import { ExpenseListResponse, ExpenseRequest } from './expense.models';

describe('expense models', () => {
  it('represent the request and its list total as numbers', () => {
    const request: ExpenseRequest = {
      spentOn: '2026-09-25',
      description: 'Algodón',
      amount: 1250.5,
      expenseTypeId: 3,
    };
    const response: ExpenseListResponse = { content: [], totalAmount: request.amount };

    expect(response.totalAmount).toBe(1250.5);
  });
});
