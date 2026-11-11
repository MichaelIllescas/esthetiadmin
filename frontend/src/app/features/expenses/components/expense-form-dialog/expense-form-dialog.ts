import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { finalize, Observable } from 'rxjs';
import { ModalDialog } from '../../../../shared/components/modal-dialog/modal-dialog';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { todayIso } from '../../../../shared/utils/format';
import { Expense, ExpenseType } from '../../models/expense.models';
import { ExpensesService } from '../../services/expenses.service';

function notInTheFuture(control: AbstractControl<string>): ValidationErrors | null {
  return control.value && control.value > todayIso() ? { future: true } : null;
}

@Component({
  selector: 'app-expense-form-dialog',
  imports: [ReactiveFormsModule, ModalDialog],
  templateUrl: './expense-form-dialog.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExpenseFormDialog implements OnInit {
  private readonly expenses = inject(ExpensesService);

  readonly expense = input<Expense | null>(null);
  readonly expenseTypes = input.required<ExpenseType[]>();
  readonly saved = output<void>();
  readonly cancelled = output<void>();

  protected readonly heading = computed(() => (this.expense() ? 'Editar gasto' : 'Registrar gasto'));
  protected readonly today = todayIso();
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = new FormGroup({
    spentOn: new FormControl(todayIso(), {
      nonNullable: true,
      validators: [Validators.required, notInTheFuture],
    }),
    description: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(255)],
    }),
    amount: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0.01)],
    }),
    expenseTypeId: new FormControl<number | null>(null, { validators: [Validators.required] }),
  });

  ngOnInit(): void {
    const expense = this.expense();
    if (expense) {
      this.form.setValue({
        spentOn: expense.spentOn,
        description: expense.description,
        amount: expense.amount,
        expenseTypeId: expense.expenseTypeId,
      });
    }
  }

  protected isInvalid(control: keyof typeof this.form.controls): boolean {
    return this.form.controls[control].invalid && this.form.controls[control].touched;
  }

  protected submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { spentOn, description, amount, expenseTypeId } = this.form.getRawValue();
    const request = {
      spentOn,
      description: description.trim(),
      amount: Number(amount),
      expenseTypeId: Number(expenseTypeId),
    };
    const expense = this.expense();
    const request$: Observable<Expense> = expense
      ? this.expenses.update(expense.id, request)
      : this.expenses.create(request);

    this.isSubmitting.set(true);
    request$.pipe(finalize(() => this.isSubmitting.set(false))).subscribe({
      next: () => this.saved.emit(),
      error: (error: unknown) =>
        this.errorMessage.set(apiErrorMessage(error, 'No pudimos guardar el gasto. Intentá nuevamente.')),
    });
  }
}
