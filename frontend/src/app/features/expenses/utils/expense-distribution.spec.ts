import { Expense } from '../models/expense.models';
import { distributionByType, distributionSpec, sliceColor } from './expense-distribution';

const expense = (id: number, typeId: number, typeName: string, amount: number): Expense => ({
  id,
  spentOn: '2026-09-10',
  description: `Gasto ${id}`,
  amount,
  expenseTypeId: typeId,
  expenseTypeName: typeName,
});

describe('expense distribution', () => {
  const expenses = [
    expense(1, 1, 'Alquiler', 300000),
    expense(2, 2, 'Insumos', 50000),
    expense(3, 2, 'Insumos', 50000),
    expense(4, 3, 'Servicios', 100000),
  ];

  it('adds the expenses of each type, biggest first, with their share', () => {
    const shares = distributionByType(expenses);

    expect(shares.map((s) => s.name)).toEqual(['Alquiler', 'Insumos', 'Servicios']);
    expect(shares.map((s) => s.total)).toEqual([300000, 100000, 100000]);
    expect(shares.map((s) => s.count)).toEqual([1, 2, 1]);
    expect(shares.map((s) => s.percentage)).toEqual([60, 20, 20]);
  });

  it('breaks a tie by name and rounds the share to two decimals', () => {
    const shares = distributionByType([expense(1, 1, 'B', 100), expense(2, 2, 'A', 100), expense(3, 3, 'C', 100)]);

    expect(shares.map((s) => s.name)).toEqual(['A', 'B', 'C']);
    expect(shares[0].percentage).toBe(33.33);
  });

  it('has nothing to show without expenses', () => {
    expect(distributionByType([])).toEqual([]);
  });

  it('draws one slice per type and explains it with its amount and share', () => {
    const shares = distributionByType(expenses);
    const spec = distributionSpec(shares);
    const tooltip = spec.options?.plugins?.tooltip?.callbacks?.label as (item: unknown) => string;

    expect(spec.type).toBe('doughnut');
    expect(spec.data.labels).toEqual(['Alquiler', 'Insumos', 'Servicios']);
    expect(spec.data.datasets[0].data).toEqual([300000, 100000, 100000]);
    expect(tooltip({ dataIndex: 0 }).replace(/\s/g, ' ')).toBe('Alquiler: $ 300.000 (60,00 %)');
  });

  it('repeats the colors when there are more types than colors', () => {
    expect(sliceColor(0)).toBe(sliceColor(8));
  });
});
