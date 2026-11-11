import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { finalize, Observable } from 'rxjs';
import { ModalDialog } from '../../../../shared/components/modal-dialog/modal-dialog';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { formatPrice, todayIso } from '../../../../shared/utils/format';
import { Category } from '../../../catalog/models/catalog.models';
import { CatalogService } from '../../../catalog/services/catalog.service';
import { ClientPicker } from '../../../clients/components/client-picker/client-picker';
import { Client } from '../../../clients/models/client.models';
import { Treatment } from '../../models/treatment.models';
import { TreatmentsService } from '../../services/treatments.service';

interface ServiceOption {
  id: number;
  name: string;
  price: number;
  inactive: boolean;
}

interface CategoryOption {
  id: number;
  name: string;
  services: ServiceOption[];
}

function notInTheFuture(control: AbstractControl<string>): ValidationErrors | null {
  return control.value && control.value > todayIso() ? { future: true } : null;
}

/**
 * Registers a treatment or edits one. The service is chosen in two steps (category first, then a service of
 * that category) so the list stays short however large the catalog grows. The client comes fixed when the form
 * is opened from a client record and is chosen with the picker otherwise; when editing it cannot be changed.
 */
@Component({
  selector: 'app-treatment-form-dialog',
  imports: [ReactiveFormsModule, ModalDialog, ClientPicker],
  templateUrl: './treatment-form-dialog.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TreatmentFormDialog implements OnInit {
  private readonly treatments = inject(TreatmentsService);
  private readonly catalog = inject(CatalogService);
  private readonly destroyRef = inject(DestroyRef);

  /** Client already decided by the screen that opened the form (a client record). */
  readonly fixedClient = input<{ id: number; name: string } | null>(null);
  readonly treatment = input<Treatment | null>(null);
  /** Service and date to start a new treatment with, e.g. from an attended appointment. */
  readonly preset = input<{ serviceId: number; performedOn: string } | null>(null);
  readonly saved = output<void>();
  readonly cancelled = output<void>();

  protected readonly heading = computed(() => (this.treatment() ? 'Editar tratamiento' : 'Registrar tratamiento'));
  protected readonly today = todayIso();
  protected readonly categoryOptions = signal<CategoryOption[]>([]);
  protected readonly selectedCategoryId = signal<number | null>(null);
  protected readonly serviceChoices = computed(
    () => this.categoryOptions().find((category) => category.id === this.selectedCategoryId())?.services ?? [],
  );
  protected readonly isLoadingServices = signal(true);
  protected readonly servicesError = signal<string | null>(null);
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly priceHint = signal<string | null>(null);
  protected readonly formatPrice = formatPrice;

  protected readonly form = new FormGroup({
    clientId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    categoryId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    serviceId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    performedOn: new FormControl(todayIso(), { nonNullable: true, validators: [Validators.required, notInTheFuture] }),
    amount: new FormControl<number | null>(null, { validators: [Validators.required, Validators.min(0)] }),
    notes: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(2000)] }),
  });

  protected get isEditing(): boolean {
    return this.treatment() !== null;
  }

  protected get clientLabel(): string | null {
    return this.treatment()?.clientName ?? this.fixedClient()?.name ?? null;
  }

  ngOnInit(): void {
    const treatment = this.treatment();
    const fixed = this.fixedClient();
    if (treatment) {
      this.form.patchValue(
        { clientId: treatment.clientId, serviceId: treatment.serviceId, performedOn: treatment.performedOn, amount: treatment.amount, notes: treatment.notes ?? '' },
        { emitEvent: false },
      );
    } else if (fixed) {
      this.form.controls.clientId.setValue(fixed.id);
    }
    const preset = this.preset();
    if (!treatment && preset) {
      this.form.controls.performedOn.setValue(preset.performedOn);
    }

    // Choosing another category clears the service, which would otherwise belong to a different one.
    this.form.controls.categoryId.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((categoryId) => {
      this.selectedCategoryId.set(categoryId);
      this.form.controls.serviceId.reset(null);
      this.priceHint.set(null);
    });

    // A new treatment starts with the price of the chosen service, which the user can then change.
    this.form.controls.serviceId.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((serviceId) => {
      const option = this.serviceChoices().find((service) => service.id === serviceId);
      if (!option) {
        return;
      }
      this.priceHint.set(`Precio del servicio: ${formatPrice(option.price)}`);
      if (!this.isEditing && !this.form.controls.amount.dirty) {
        this.form.controls.amount.setValue(option.price);
      }
    });

    this.loadServices();
  }

  protected loadServices(): void {
    this.isLoadingServices.set(true);
    this.servicesError.set(null);
    this.catalog
      .getCategories()
      .pipe(finalize(() => this.isLoadingServices.set(false)))
      .subscribe({
        next: (categories) => {
          this.categoryOptions.set(this.buildCategories(categories));
          this.selectCategoryOfCurrentService();
          this.applyPresetService();
        },
        error: (error: unknown) =>
          this.servicesError.set(apiErrorMessage(error, 'No pudimos cargar los servicios. Intentá nuevamente.')),
      });
  }

  protected onClientPicked(client: Client | null): void {
    this.form.controls.clientId.setValue(client?.id ?? null);
    this.form.controls.clientId.markAsTouched();
  }

  protected isInvalid(control: 'clientId' | 'categoryId' | 'serviceId' | 'performedOn' | 'amount'): boolean {
    return this.form.controls[control].invalid && this.form.controls[control].touched;
  }

  protected submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { clientId, serviceId, performedOn, amount, notes } = this.form.getRawValue();
    const common = {
      serviceId: Number(serviceId),
      performedOn,
      amount: Number(amount),
      notes: notes.trim() || null,
    };
    const treatment = this.treatment();
    const request$: Observable<Treatment> = treatment
      ? this.treatments.update(treatment.id, common)
      : this.treatments.create({ clientId: Number(clientId), ...common });

    this.isSubmitting.set(true);
    request$.pipe(finalize(() => this.isSubmitting.set(false))).subscribe({
      next: () => this.saved.emit(),
      error: (error: unknown) =>
        this.errorMessage.set(apiErrorMessage(error, 'No pudimos guardar el tratamiento. Intentá nuevamente.')),
    });
  }

  /** Only what can be offered today, plus the current service when editing an older treatment. */
  private buildCategories(categories: Category[]): CategoryOption[] {
    const options: CategoryOption[] = categories
      .filter((category) => category.active)
      .map((category) => ({
        id: category.id,
        name: category.name,
        services: category.services
          .filter((service) => service.active)
          .map((service) => ({ id: service.id, name: service.name, price: service.price, inactive: false })),
      }))
      .filter((category) => category.services.length > 0);

    const current = this.treatment();
    if (current && !options.some((category) => category.services.some((service) => service.id === current.serviceId))) {
      const currentService: ServiceOption = { id: current.serviceId, name: current.serviceName, price: current.amount, inactive: true };
      const owner = categories.find((category) => category.services.some((service) => service.id === current.serviceId));
      const existing = options.find((category) => category.id === owner?.id);
      if (existing) {
        existing.services.push(currentService);
      } else {
        options.unshift({ id: owner?.id ?? current.categoryId ?? 0, name: `${current.categoryName} (inactiva)`, services: [currentService] });
      }
    }
    return options;
  }

  /** Selects the preset service (and its category) so its price is filled in as when choosing it by hand. */
  private applyPresetService(): void {
    const preset = this.preset();
    if (this.treatment() || !preset) {
      return;
    }
    const owner = this.categoryOptions().find((category) => category.services.some((service) => service.id === preset.serviceId));
    if (owner) {
      this.selectedCategoryId.set(owner.id);
      this.form.controls.categoryId.setValue(owner.id, { emitEvent: false });
      this.form.controls.serviceId.setValue(preset.serviceId);
    }
  }

  /** When editing, opens the category of the treatment's service so the service select shows its current choice. */
  private selectCategoryOfCurrentService(): void {
    const current = this.treatment();
    if (!current) {
      return;
    }
    const owner = this.categoryOptions().find((category) => category.services.some((service) => service.id === current.serviceId));
    if (owner) {
      this.selectedCategoryId.set(owner.id);
      this.form.controls.categoryId.setValue(owner.id, { emitEvent: false });
    }
  }
}
