import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChartFactory } from '../../../../shared/components/chart/chart-factory';
import { Expense } from '../../models/expense.models';
import { ExpenseDistribution } from './expense-distribution';

const expense = (id: number, typeId: number, typeName: string, amount: number): Expense => ({
  id,
  spentOn: '2026-09-10',
  description: `Gasto ${id}`,
  amount,
  expenseTypeId: typeId,
  expenseTypeName: typeName,
});

@Component({
  imports: [ExpenseDistribution],
  template: `<app-expense-distribution [expenses]="expenses()" />`,
})
class Host {
  readonly expenses = signal<Expense[]>([]);
}

describe('ExpenseDistribution', () => {
  const factory = { create: vi.fn(() => ({ update: vi.fn(), destroy: vi.fn() })) };
  let fixture: ComponentFixture<Host>;
  const root = () => fixture.nativeElement as HTMLElement;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ').replace(/\s/g, ' ');
  const show = async (expenses: Expense[]) => {
    fixture.componentInstance.expenses.set(expenses);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  };

  beforeEach(async () => {
    factory.create.mockClear();
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [{ provide: ChartFactory, useValue: factory }],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
  });

  it('draws the chart and lists each type with its amount, share and number of expenses', async () => {
    await show([expense(1, 1, 'Alquiler', 300000), expense(2, 2, 'Insumos', 50000), expense(3, 2, 'Insumos', 50000)]);

    expect(text()).toContain('Distribución por tipo');
    expect(root().querySelectorAll('li')).toHaveLength(2);
    const items = Array.from(root().querySelectorAll('li')).map((li) => (li.textContent ?? '').replace(/\s/g, ' '));
    expect(items[0]).toContain('Alquiler');
    expect(items[0]).toContain('$ 300.000');
    expect(items[0]).toContain('75,00 % · 1 gasto');
    expect(items[1]).toContain('Insumos');
    expect(items[1]).toContain('$ 100.000');
    expect(items[1]).toContain('25,00 % · 2 gastos');
    const canvas = root().querySelector('canvas') as HTMLCanvasElement;
    expect(canvas.getAttribute('aria-label')).toContain('Alquiler 75,00 %');
    expect(factory.create).toHaveBeenCalledTimes(1);
  });

  it('stays hidden when everything is of a single type, since there is nothing to compare', async () => {
    await show([expense(1, 3, 'Insumos', 1000), expense(2, 3, 'Insumos', 2000)]);

    expect(root().querySelector('section')).toBeNull();
    expect(root().querySelector('canvas')).toBeNull();
  });
});
