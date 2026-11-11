import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, map, of, Subject, switchMap, tap } from 'rxjs';
import { Icon } from '../../../../shared/components/icon/icon';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { formatDate, formatPrice, formatShortDate } from '../../../../shared/utils/format';
import { Expense, ExpenseFilters, ExpenseListResponse, ExpenseType } from '../../models/expense.models';
import { ExpensesService } from '../../services/expenses.service';
import { ExpenseDistribution } from '../expense-distribution/expense-distribution';
import { ExpenseFormDialog } from '../expense-form-dialog/expense-form-dialog';

const NO_FILTERS: ExpenseFilters = { from: '', to: '', expenseTypeId: null };

type Outcome = { ok: true; result: ExpenseListResponse } | { ok: false; message: string };
type DialogState = { type: 'form'; expense: Expense | null } | { type: 'delete'; expense: Expense };

@Component({
  selector: 'app-expense-list',
  imports: [ExpenseFormDialog, ExpenseDistribution, ConfirmDialog, Icon],
  templateUrl: './expense-list.html',
  styleUrl: './expense-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExpenseList implements OnInit {
  private readonly expenses = inject(ExpensesService);
  private readonly load$ = new Subject<void>();

  protected readonly filters = signal<ExpenseFilters>(NO_FILTERS);
  protected readonly result = signal<ExpenseListResponse | null>(null);
  protected readonly expenseTypes = signal<ExpenseType[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly typesLoading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly typesError = signal<string | null>(null);
  protected readonly dialog = signal<DialogState | null>(null);
  protected readonly dialogBusy = signal(false);
  protected readonly dialogError = signal<string | null>(null);

  protected readonly rows = computed(() => this.result()?.content ?? []);
  protected readonly hasFilters = computed(() => {
    const filters = this.filters();
    return filters.from !== '' || filters.to !== '' || filters.expenseTypeId !== null;
  });
  protected readonly rangeInvalid = computed(() => {
    const { from, to } = this.filters();
    return from !== '' && to !== '' && from > to;
  });

  protected readonly formatDate = formatDate;
  protected readonly formatShortDate = formatShortDate;
  protected readonly formatPrice = formatPrice;

  constructor() {
    this.load$
      .pipe(
        tap(() => {
          this.isLoading.set(true);
          this.loadError.set(null);
        }),
        switchMap(() =>
          this.expenses.search(this.filters()).pipe(
            map((result): Outcome => ({ ok: true, result })),
            catchError((error: unknown) =>
              of<Outcome>({
                ok: false,
                message: apiErrorMessage(error, 'No pudimos cargar los gastos. Intentá nuevamente.'),
              }),
            ),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((outcome) => {
        this.isLoading.set(false);
        if (outcome.ok) {
          this.result.set(outcome.result);
        } else {
          this.loadError.set(outcome.message);
        }
      });
  }

  ngOnInit(): void {
    this.loadTypes();
    this.reload();
  }

  protected reload(): void {
    this.load$.next();
  }

  protected loadTypes(): void {
    this.typesLoading.set(true);
    this.typesError.set(null);
    this.expenses
      .getTypes()
      .pipe(finalize(() => this.typesLoading.set(false)))
      .subscribe({
        next: (types) => this.expenseTypes.set(types),
        error: (error: unknown) =>
          this.typesError.set(apiErrorMessage(error, 'No pudimos cargar los tipos de gasto. Intentá nuevamente.')),
      });
  }

  protected onDateChange(field: 'from' | 'to', event: Event): void {
    this.setFilter({ [field]: (event.target as HTMLInputElement).value });
  }

  protected onTypeChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.setFilter({ expenseTypeId: value === '' ? null : Number(value) });
  }

  protected clearFilters(): void {
    this.filters.set(NO_FILTERS);
    this.reload();
  }

  protected openForm(expense: Expense | null): void {
    this.dialogError.set(null);
    this.dialog.set({ type: 'form', expense });
  }

  protected requestDelete(expense: Expense): void {
    this.dialogError.set(null);
    this.dialog.set({ type: 'delete', expense });
  }

  protected closeDialog(): void {
    this.dialog.set(null);
    this.dialogError.set(null);
  }

  protected onSaved(): void {
    this.closeDialog();
    this.reload();
  }

  protected confirmDelete(expense: Expense): void {
    this.dialogBusy.set(true);
    this.dialogError.set(null);
    this.expenses
      .delete(expense.id)
      .pipe(finalize(() => this.dialogBusy.set(false)))
      .subscribe({
        next: () => this.onSaved(),
        error: (error: unknown) =>
          this.dialogError.set(apiErrorMessage(error, 'No pudimos eliminar el gasto. Intentá nuevamente.')),
      });
  }

  protected deleteMessage(expense: Expense): string {
    return `¿Eliminar el gasto "${expense.description}" del ${formatDate(expense.spentOn)} por ${formatPrice(expense.amount)}? Esta acción no se puede deshacer y el importe deja de contarse en los reportes.`;
  }

  private setFilter(change: Partial<ExpenseFilters>): void {
    this.filters.update((current) => ({ ...current, ...change }));
    if (!this.rangeInvalid()) {
      this.reload();
    }
  }
}
