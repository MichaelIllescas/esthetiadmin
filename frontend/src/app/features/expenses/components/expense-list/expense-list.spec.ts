import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Expense } from '../../models/expense.models';
import { ExpensesService } from '../../services/expenses.service';
import { ExpenseList } from './expense-list';

const types = [
  { id: 2, name: 'Servicios' },
  { id: 3, name: 'Insumos' },
];

const expense = (id: number, description: string, amount: number, expenseTypeId = 3): Expense => ({
  id,
  spentOn: '2026-09-25',
  description,
  amount,
  expenseTypeId,
  expenseTypeName: expenseTypeId === 3 ? 'Insumos' : 'Servicios',
});

describe('ExpenseList', () => {
  const expenses = {
    search: vi.fn(),
    getTypes: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  };
  let fixture: ComponentFixture<ExpenseList>;

  const root = () => fixture.nativeElement as HTMLElement;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ');
  const button = (label: string) =>
    (Array.from(root().querySelectorAll('button')) as HTMLButtonElement[]).find(
      (candidate) => candidate.textContent?.trim() === label,
    ) as HTMLButtonElement;
  const buttonByLabel = (label: string) =>
    (Array.from(root().querySelectorAll('button')) as HTMLButtonElement[]).find(
      (candidate) => candidate.getAttribute('aria-label') === label,
    ) as HTMLButtonElement;
  const dialogButton = (label: string) =>
    (Array.from(root().querySelectorAll('dialog button')) as HTMLButtonElement[]).find(
      (candidate) => candidate.textContent?.trim() === label,
    ) as HTMLButtonElement;
  const change = (id: string, value: string) => {
    const field = root().querySelector(`#${id}`) as HTMLInputElement | HTMLSelectElement;
    field.value = value;
    field.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };
  const start = async () => {
    await TestBed.configureTestingModule({
      imports: [ExpenseList],
      providers: [{ provide: ExpensesService, useValue: expenses }],
    }).compileComponents();
    fixture = TestBed.createComponent(ExpenseList);
    fixture.detectChanges();
  };

  beforeEach(() => {
    Object.values(expenses).forEach((mock) => mock.mockReset());
    expenses.getTypes.mockReturnValue(of(types));
    expenses.search.mockReturnValue(
      of({ content: [expense(8, 'Algodón', 1250.5), expense(9, 'Internet', 4500, 2)], totalAmount: 5750.5 }),
    );
  });

  it('shows expenses newest first with their period total', async () => {
    await start();

    expect(expenses.search).toHaveBeenCalledWith({ from: '', to: '', expenseTypeId: null });
    expect(text()).toContain('Algodón');
    expect(text()).toContain('Internet');
    expect(text()).toContain('Total del período');
    expect(text().replace(/\s/g, ' ')).toContain('$ 5.750,50');
    expect(text()).toContain('2 gastos');
  });

  it('filters by type and date range', async () => {
    await start();
    change('filter-expense-type', '3');
    change('filter-expense-from', '2026-09-01');
    change('filter-expense-to', '2026-09-30');

    expect(expenses.search).toHaveBeenLastCalledWith({
      from: '2026-09-01',
      to: '2026-09-30',
      expenseTypeId: 3,
    });
  });

  it('does not search with an inverted range and explains why', async () => {
    await start();
    change('filter-expense-from', '2026-09-30');
    expenses.search.mockClear();

    change('filter-expense-to', '2026-09-01');

    expect(expenses.search).not.toHaveBeenCalled();
    expect(root().querySelector('[role="alert"]')?.textContent).toContain('no puede ser posterior');
  });

  it('clears every filter', async () => {
    await start();
    change('filter-expense-type', '3');
    change('filter-expense-from', '2026-09-01');

    button('Limpiar filtros').click();
    fixture.detectChanges();

    expect(expenses.search).toHaveBeenLastCalledWith({ from: '', to: '', expenseTypeId: null });
  });

  it('opens the form for creating and editing', async () => {
    await start();
    button('+ Registrar gasto').click();
    fixture.detectChanges();
    expect(root().querySelector('dialog h2')?.textContent).toContain('Registrar gasto');

    (root().querySelector('dialog .dialog-close') as HTMLButtonElement).click();
    fixture.detectChanges();
    buttonByLabel('Editar el gasto Algodón del 25 de septiembre de 2026').click();
    fixture.detectChanges();
    expect(root().querySelector('dialog h2')?.textContent).toContain('Editar gasto');
  });

  it('deletes after confirmation and reloads the list', async () => {
    expenses.delete.mockReturnValue(of(undefined));
    await start();

    buttonByLabel('Eliminar el gasto Algodón del 25 de septiembre de 2026').click();
    fixture.detectChanges();
    expect(text()).toContain('deja de contarse en los reportes');
    expenses.search.mockClear();
    dialogButton('Eliminar').click();
    fixture.detectChanges();

    expect(expenses.delete).toHaveBeenCalledWith(8);
    expect(expenses.search).toHaveBeenCalledTimes(1);
    expect(root().querySelector('dialog')).toBeNull();
  });

  it('shows a useful empty state', async () => {
    expenses.search.mockReturnValueOnce(of({ content: [], totalAmount: 0 }));
    await start();

    expect(text()).toContain('Todavía no hay gastos registrados');
    expect(button('+ Registrar el primer gasto')).toBeTruthy();
  });

  it('shows a retry when loading expenses fails', async () => {
    expenses.search.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })));
    await start();

    expect(root().querySelector('.status-error')?.textContent).toContain('No pudimos cargar los gastos');
    button('Reintentar').click();
    fixture.detectChanges();
    expect(text()).toContain('Algodón');
  });

  it('keeps the delete dialog open when the API rejects the action', async () => {
    expenses.delete.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404, error: { message: 'El gasto no existe' } })),
    );
    await start();
    buttonByLabel('Eliminar el gasto Algodón del 25 de septiembre de 2026').click();
    fixture.detectChanges();

    dialogButton('Eliminar').click();
    fixture.detectChanges();

    expect(root().querySelector('dialog [role="alert"]')?.textContent).toContain('El gasto no existe');
  });
});
