import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Expense, ExpenseType } from '../../models/expense.models';
import { ExpensesService } from '../../services/expenses.service';
import { ExpenseFormDialog } from './expense-form-dialog';

const types: ExpenseType[] = [
  { id: 2, name: 'Servicios' },
  { id: 3, name: 'Insumos' },
];

const expense: Expense = {
  id: 8,
  spentOn: '2026-09-10',
  description: 'Internet',
  amount: 4500,
  expenseTypeId: 2,
  expenseTypeName: 'Servicios',
};

@Component({
  imports: [ExpenseFormDialog],
  template: `
    <app-expense-form-dialog
      [expense]="expense()"
      [expenseTypes]="types"
      (saved)="events.push('saved')"
      (cancelled)="events.push('cancelled')"
    />
  `,
})
class Host {
  readonly expense = signal<Expense | null>(null);
  readonly types = types;
  readonly events: string[] = [];
}

describe('ExpenseFormDialog', () => {
  const expenses = { create: vi.fn(), update: vi.fn() };
  let fixture: ComponentFixture<Host>;

  const root = () => fixture.nativeElement as HTMLElement;
  const field = <T extends HTMLElement>(id: string) => root().querySelector(`#${id}`) as T;
  const setInput = (id: string, value: string) => {
    const input = field<HTMLInputElement>(id);
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };
  const selectType = (name: string) => {
    const select = field<HTMLSelectElement>('expense-type');
    const option = Array.from(select.options).find((candidate) => candidate.textContent?.trim() === name);
    select.value = option?.value ?? '';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };
  const submit = () => {
    root().querySelector('form')?.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };
  const start = async (editing = false) => {
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [{ provide: ExpensesService, useValue: expenses }],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    if (editing) {
      fixture.componentInstance.expense.set(expense);
    }
    fixture.detectChanges();
  };

  beforeEach(() => {
    expenses.create.mockReset();
    expenses.update.mockReset();
  });

  it('creates a valid expense with a trimmed description', async () => {
    expenses.create.mockReturnValue(of(expense));
    await start();
    setInput('expense-date', '2026-09-25');
    setInput('expense-description', '  Algodón  ');
    setInput('expense-amount', '1250.50');
    selectType('Insumos');

    submit();

    expect(expenses.create).toHaveBeenCalledWith({
      spentOn: '2026-09-25',
      description: 'Algodón',
      amount: 1250.5,
      expenseTypeId: 3,
    });
    expect(fixture.componentInstance.events).toContain('saved');
  });

  it('prefills and updates the selected expense', async () => {
    expenses.update.mockReturnValue(of(expense));
    await start(true);

    expect(field<HTMLInputElement>('expense-description').value).toBe('Internet');
    expect(field<HTMLInputElement>('expense-amount').valueAsNumber).toBe(4500);
    submit();

    expect(expenses.update).toHaveBeenCalledWith(8, {
      spentOn: '2026-09-10',
      description: 'Internet',
      amount: 4500,
      expenseTypeId: 2,
    });
  });

  it('rejects missing fields, zero amounts and future dates', async () => {
    await start();
    setInput('expense-date', '2999-01-01');
    setInput('expense-description', ' ');
    setInput('expense-amount', '0');

    submit();

    expect(expenses.create).not.toHaveBeenCalled();
    expect(root().textContent).toContain('fecha que no sea futura');
    expect(root().textContent).toContain('concepto del gasto');
    expect(root().textContent).toContain('importe mayor que cero');
    expect(root().textContent).toContain('tipo de gasto');
  });

  it('keeps the form open and shows the API error', async () => {
    expenses.create.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404, error: { message: 'El tipo de gasto no existe' } })),
    );
    await start();
    setInput('expense-date', '2026-09-25');
    setInput('expense-description', 'Algodón');
    setInput('expense-amount', '100');
    selectType('Insumos');

    submit();

    expect(root().querySelector('[role="alert"]')?.textContent).toContain('El tipo de gasto no existe');
    expect(fixture.componentInstance.events).not.toContain('saved');
  });
});
