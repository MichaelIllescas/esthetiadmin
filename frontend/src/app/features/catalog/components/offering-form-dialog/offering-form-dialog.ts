import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ModalDialog } from '../../../../shared/components/modal-dialog/modal-dialog';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { Category, Offering } from '../../models/catalog.models';
import { CatalogService } from '../../services/catalog.service';

/** Creates a service or, when one is given, edits it (including moving it to another category). */
@Component({
  selector: 'app-offering-form-dialog',
  imports: [ReactiveFormsModule, ModalDialog],
  templateUrl: './offering-form-dialog.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OfferingFormDialog implements OnInit {
  private readonly catalog = inject(CatalogService);

  readonly categories = input.required<readonly Category[]>();
  readonly offering = input<Offering | null>(null);
  readonly defaultCategoryId = input<number | null>(null);
  readonly saved = output<void>();
  readonly cancelled = output<void>();

  protected readonly heading = computed(() => (this.offering() ? 'Editar servicio' : 'Nuevo servicio'));
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = new FormGroup({
    categoryId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(150)],
    }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(2000)] }),
    price: new FormControl<number | null>(null, { validators: [Validators.required, Validators.min(0)] }),
    bookable: new FormControl(true, { nonNullable: true }),
  });

  ngOnInit(): void {
    const offering = this.offering();
    if (offering) {
      this.form.setValue({
        categoryId: offering.categoryId,
        name: offering.name,
        description: offering.description ?? '',
        price: offering.price,
        bookable: offering.bookable,
      });
    } else {
      this.form.controls.categoryId.setValue(this.defaultCategoryId());
    }
  }

  protected isInvalid(control: 'categoryId' | 'name' | 'price'): boolean {
    return this.form.controls[control].invalid && this.form.controls[control].touched;
  }

  protected submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { categoryId, name, description, price, bookable } = this.form.getRawValue();
    const request = {
      categoryId: Number(categoryId),
      name,
      description: description.trim() ? description : null,
      price: Number(price),
      bookable,
    };
    const offering = this.offering();
    const request$ = offering ? this.catalog.updateOffering(offering.id, request) : this.catalog.createOffering(request);

    this.isSubmitting.set(true);
    request$.pipe(finalize(() => this.isSubmitting.set(false))).subscribe({
      next: () => this.saved.emit(),
      error: (error: unknown) =>
        this.errorMessage.set(apiErrorMessage(error, 'No pudimos guardar el servicio. Intentá nuevamente.')),
    });
  }
}
