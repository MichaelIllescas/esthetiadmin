import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { catchError, finalize, map, merge, of, switchMap, tap } from 'rxjs';
import { ModalDialog } from '../../../../shared/components/modal-dialog/modal-dialog';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { EMAIL_PATTERN } from '../../../../shared/utils/contact';
import { todayIso } from '../../../../shared/utils/format';
import { HALF_HOURS } from '../../../availability/utils/schedule-times';
import { Category } from '../../../catalog/models/catalog.models';
import { CatalogService } from '../../../catalog/services/catalog.service';
import { ClientPicker } from '../../../clients/components/client-picker/client-picker';
import { Client } from '../../../clients/models/client.models';
import { Appointment } from '../../models/appointment.models';
import { AppointmentsService } from '../../services/appointments.service';
import { dateOf, timeOf } from '../../utils/agenda-dates';

interface ServiceGroup {
  /** The category id; 0 for the inactive category of a service being kept when moving an appointment. */
  id: number;
  name: string;
  services: { id: number; name: string }[];
}

function notPast(control: AbstractControl<string>): ValidationErrors | null {
  return control.value && control.value < todayIso() ? { past: true } : null;
}

/** 8 to 15 digits once spaces, dashes and the like are left out, as the API keeps it. */
function phoneDigits(control: AbstractControl<string>): ValidationErrors | null {
  const digits = (control.value ?? '').replace(/\D/g, '').length;
  return digits >= 8 && digits <= 15 ? null : { phone: true };
}

/**
 * Loads an appointment or moves an existing one. The service is chosen in two steps (category, then service) so
 * the list stays short however large the catalog grows. The times offered are the free ones of that day for that service;
 * "Fuera del horario" shows every half hour, for when she decides to attend outside her usual hours.
 */
@Component({
  selector: 'app-appointment-form-dialog',
  imports: [ReactiveFormsModule, ModalDialog, ClientPicker],
  templateUrl: './appointment-form-dialog.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppointmentFormDialog implements OnInit {
  private readonly appointments = inject(AppointmentsService);
  private readonly catalog = inject(CatalogService);
  private readonly destroyRef = inject(DestroyRef);

  readonly appointment = input<Appointment | null>(null);
  /** Day the agenda was showing, to start a new appointment on it. */
  readonly defaultDate = input<string | null>(null);
  readonly saved = output<Appointment>();
  readonly cancelled = output<void>();

  protected readonly heading = computed(() => (this.appointment() ? 'Reprogramar turno' : 'Nuevo turno'));
  protected readonly today = todayIso();
  protected readonly serviceGroups = signal<ServiceGroup[]>([]);
  protected readonly selectedCategoryId = signal<number | null>(null);
  protected readonly serviceChoices = computed(
    () => this.serviceGroups().find((group) => group.id === this.selectedCategoryId())?.services ?? [],
  );
  protected readonly servicesError = signal<string | null>(null);
  protected readonly freeTimes = signal<string[]>([]);
  protected readonly timesLoading = signal(false);
  protected readonly timesError = signal<string | null>(null);
  protected readonly durationMinutes = signal<number | null>(null);
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly linkedClient = signal<Client | null>(null);

  protected readonly form = new FormGroup({
    categoryId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    serviceId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    date: new FormControl(todayIso(), { nonNullable: true, validators: [Validators.required, notPast] }),
    outsideHours: new FormControl(false, { nonNullable: true }),
    time: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    firstName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)] }),
    lastName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)] }),
    phone: new FormControl('', { nonNullable: true, validators: [Validators.required, phoneDigits, Validators.maxLength(30)] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.pattern(EMAIL_PATTERN), Validators.maxLength(254)] }),
    notes: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(2000)] }),
  });

  private readonly showAllTimes = signal(false);
  /** The times to choose from: the free ones, or every half hour when going outside the usual hours. */
  protected readonly timeOptions = computed(() => (this.showAllTimes() ? [...HALF_HOURS] : this.freeTimes()));

  protected get isEditing(): boolean {
    return this.appointment() !== null;
  }

  ngOnInit(): void {
    const appointment = this.appointment();
    if (appointment) {
      this.form.patchValue({
        serviceId: appointment.serviceId,
        date: dateOf(appointment.startsAt),
        time: timeOf(appointment.startsAt),
        firstName: appointment.firstName,
        lastName: appointment.lastName,
        phone: appointment.phone,
        email: appointment.email ?? '',
        notes: appointment.notes ?? '',
      });
    } else if (this.defaultDate() && this.defaultDate()! >= this.today) {
      this.form.controls.date.setValue(this.defaultDate()!);
    }

    // Choosing another category clears the service, which would otherwise belong to a different one.
    this.form.controls.categoryId.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((categoryId) => {
      this.selectedCategoryId.set(categoryId);
      this.form.controls.serviceId.setValue(null);
    });

    this.form.controls.outsideHours.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((outside) => {
        this.showAllTimes.set(outside);
        this.keepTimeIfStillOffered();
      });

    // Each change of service or day asks again for the free times of that combination.
    merge(this.form.controls.serviceId.valueChanges, this.form.controls.date.valueChanges, of(null))
      .pipe(
        map(() => this.form.getRawValue()),
        tap(() => this.timesError.set(null)),
        switchMap(({ serviceId, date }) => {
          if (serviceId === null || !date) {
            return of(null);
          }
          this.timesLoading.set(true);
          return this.appointments.availability(Number(serviceId), date, this.appointment()?.id ?? null).pipe(
            catchError((error: unknown) => {
              this.timesError.set(apiErrorMessage(error, 'No pudimos cargar los horarios libres.'));
              return of(null);
            }),
            finalize(() => this.timesLoading.set(false)),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((availability) => {
        this.freeTimes.set(availability?.times ?? []);
        this.durationMinutes.set(availability?.durationMinutes ?? null);
        this.keepTimeIfStillOffered();
      });

    this.catalog.getCategories().subscribe({
      next: (categories) => {
        this.serviceGroups.set(this.buildGroups(categories));
        this.selectCategoryOfCurrentService();
      },
      error: (error: unknown) =>
        this.servicesError.set(apiErrorMessage(error, 'No pudimos cargar los servicios. Intentá nuevamente.')),
    });
  }

  /** Taking a registered client fills her name, phone and email, which can still be corrected. */
  protected onClientPicked(client: Client | null): void {
    this.linkedClient.set(client);
    if (client) {
      this.form.patchValue({
        firstName: client.firstName,
        lastName: client.lastName,
        phone: client.phone ?? '',
        email: client.email ?? '',
      });
    }
  }

  protected isInvalid(
    control: 'categoryId' | 'serviceId' | 'date' | 'time' | 'firstName' | 'lastName' | 'phone' | 'email',
  ): boolean {
    return this.form.controls[control].invalid && this.form.controls[control].touched;
  }

  protected submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { serviceId, date, time, firstName, lastName, phone, email, notes } = this.form.getRawValue();
    const appointment = this.appointment();
    const request = {
      serviceId: Number(serviceId),
      startsAt: `${date}T${time}:00`,
      clientId: this.linkedClient()?.id ?? appointment?.clientId ?? null,
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      phone: phone.trim(),
      notes: notes.trim() || null,
      email: email.trim() || null,
    };
    const request$ = appointment
      ? this.appointments.update(appointment.id, request)
      : this.appointments.create(request);

    this.isSubmitting.set(true);
    request$.pipe(finalize(() => this.isSubmitting.set(false))).subscribe({
      next: (saved) => this.saved.emit(saved),
      error: (error: unknown) =>
        this.errorMessage.set(apiErrorMessage(error, 'No pudimos guardar el turno. Intentá nuevamente.')),
    });
  }

  /**
   * A chosen time that is no longer offered (another day, another service) is cleared, not silently kept. The one
   * exception is the appointment's own time when it was booked outside the usual hours: that switch is turned on.
   */
  private keepTimeIfStillOffered(): void {
    const time = this.form.controls.time;
    if (!time.value || this.timeOptions().includes(time.value)) {
      return;
    }
    const current = this.appointment();
    const { serviceId, date } = this.form.getRawValue();
    if (current && serviceId === current.serviceId && date === dateOf(current.startsAt) && time.value === timeOf(current.startsAt)) {
      this.form.controls.outsideHours.setValue(true);
      return;
    }
    time.setValue('');
  }

  /** What can be booked today, plus the appointment's own service when moving one whose service was turned off. */
  private buildGroups(categories: Category[]): ServiceGroup[] {
    const groups = categories
      .filter((category) => category.active)
      .map((category) => ({
        id: category.id,
        name: category.name,
        services: category.services.filter((service) => service.active).map(({ id, name }) => ({ id, name })),
      }))
      .filter((group) => group.services.length > 0);
    const current = this.appointment();
    if (current && !groups.some((group) => group.services.some((service) => service.id === current.serviceId))) {
      groups.unshift({ id: 0, name: `${current.categoryName} (inactivo)`, services: [{ id: current.serviceId, name: current.serviceName }] });
    }
    return groups;
  }

  /** When moving an appointment, opens the category of its service so the service select shows its choice. */
  private selectCategoryOfCurrentService(): void {
    const current = this.appointment();
    const owner = current && this.serviceGroups().find((group) => group.services.some((service) => service.id === current.serviceId));
    if (owner) {
      this.selectedCategoryId.set(owner.id);
      this.form.controls.categoryId.setValue(owner.id, { emitEvent: false });
    }
  }
}
