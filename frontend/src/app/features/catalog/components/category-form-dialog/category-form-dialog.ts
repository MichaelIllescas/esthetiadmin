import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ModalDialog } from '../../../../shared/components/modal-dialog/modal-dialog';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { APPOINTMENT_DURATIONS, AppointmentMinutes, Category } from '../../models/catalog.models';
import { CatalogService } from '../../services/catalog.service';

/** Creates a category or, when one is given, edits its name and appointment length. */
@Component({
  selector: 'app-category-form-dialog',
  imports: [ReactiveFormsModule, ModalDialog],
  templateUrl: './category-form-dialog.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategoryFormDialog implements OnInit {
  private readonly catalog = inject(CatalogService);

  readonly category = input<Category | null>(null);
  readonly saved = output<void>();
  readonly cancelled = output<void>();

  protected readonly heading = computed(() => (this.category() ? 'Editar categoría' : 'Nueva categoría'));
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly durations = APPOINTMENT_DURATIONS;

  protected readonly form = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)],
    }),
    appointmentMinutes: new FormControl<AppointmentMinutes>(60, { nonNullable: true }),
  });

  ngOnInit(): void {
    const category = this.category();
    if (category) {
      this.form.setValue({ name: category.name, appointmentMinutes: category.appointmentMinutes });
    }
  }

  protected submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request = this.form.getRawValue();
    const category = this.category();
    const request$ = category
      ? this.catalog.updateCategory(category.id, request)
      : this.catalog.createCategory(request);

    this.isSubmitting.set(true);
    request$.pipe(finalize(() => this.isSubmitting.set(false))).subscribe({
      next: () => this.saved.emit(),
      error: (error: unknown) =>
        this.errorMessage.set(apiErrorMessage(error, 'No pudimos guardar la categoría. Intentá nuevamente.')),
    });
  }
}
