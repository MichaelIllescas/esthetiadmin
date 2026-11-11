import { ChangeDetectionStrategy, Component, computed, inject, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ModalDialog } from '../../../../shared/components/modal-dialog/modal-dialog';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { todayIso } from '../../../../shared/utils/format';
import { AvailabilityBlock } from '../../models/availability.models';
import { AvailabilityService } from '../../services/availability.service';
import { HALF_HOURS } from '../../utils/schedule-times';

interface BlockFormValue {
  startDate: string;
  endDate: string;
  wholeDay: boolean;
  startTime: string;
  endTime: string;
}

function notPast(control: AbstractControl<string>): ValidationErrors | null {
  return control.value && control.value < todayIso() ? { past: true } : null;
}

/** Dates in order and, on a single day that is not whole, hours that move forward. */
function validBlock(group: AbstractControl): ValidationErrors | null {
  const { startDate, endDate, wholeDay, startTime, endTime } = group.value as BlockFormValue;
  if (startDate && endDate && endDate < startDate) {
    return { dates: true };
  }
  return startDate === endDate && !wholeDay && startTime >= endTime ? { range: true } : null;
}

/** Takes one or several whole days, or a range of hours of a single day, out of the appointment schedule. */
@Component({
  selector: 'app-block-form-dialog',
  imports: [ReactiveFormsModule, ModalDialog],
  templateUrl: './block-form-dialog.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BlockFormDialog {
  private readonly availability = inject(AvailabilityService);

  readonly saved = output<AvailabilityBlock>();
  readonly cancelled = output<void>();

  protected readonly today = todayIso();
  protected readonly times = HALF_HOURS;
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = new FormGroup(
    {
      startDate: new FormControl(todayIso(), { nonNullable: true, validators: [Validators.required, notPast] }),
      endDate: new FormControl(todayIso(), { nonNullable: true, validators: [Validators.required] }),
      wholeDay: new FormControl(true, { nonNullable: true }),
      startTime: new FormControl('09:00', { nonNullable: true }),
      endTime: new FormControl('13:00', { nonNullable: true }),
      reason: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(150)] }),
    },
    { validators: validBlock },
  );

  private readonly value = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });
  /** Hours only make sense on a single day: a longer block always takes whole days. */
  protected readonly singleDay = computed(() => this.value().startDate === this.value().endDate);
  protected readonly showHours = computed(() => this.singleDay() && !this.value().wholeDay);

  constructor() {
    // Moving the start past the end drags the end along, so a one-day block stays one click away.
    this.form.controls.startDate.valueChanges.subscribe((startDate) => {
      if (startDate && this.form.controls.endDate.value < startDate) {
        this.form.controls.endDate.setValue(startDate);
      }
    });
  }

  protected get startDateInvalid(): boolean {
    const control = this.form.controls.startDate;
    return control.invalid && control.touched;
  }

  protected get datesInvalid(): boolean {
    return this.form.hasError('dates');
  }

  protected get rangeInvalid(): boolean {
    return this.form.hasError('range');
  }

  protected submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { startDate, endDate, wholeDay, startTime, endTime, reason } = this.form.getRawValue();
    const withHours = startDate === endDate && !wholeDay;
    const request = {
      startDate,
      endDate,
      startTime: withHours ? startTime : null,
      endTime: withHours ? endTime : null,
      reason: reason.trim() ? reason.trim() : null,
    };

    this.isSubmitting.set(true);
    this.availability
      .createBlock(request)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (block) => this.saved.emit(block),
        error: (error: unknown) =>
          this.errorMessage.set(apiErrorMessage(error, 'No pudimos guardar el bloqueo. Intentá nuevamente.')),
      });
  }
}
