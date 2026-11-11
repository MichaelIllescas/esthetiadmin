import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { finalize, merge, Observable } from 'rxjs';
import { ModalDialog } from '../../../../shared/components/modal-dialog/modal-dialog';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { EMAIL_PATTERN } from '../../../../shared/utils/contact';
import { todayIso } from '../../../../shared/utils/format';
import { Client, ClientRequest } from '../../models/client.models';
import { ClientsService } from '../../services/clients.service';

function notInTheFuture(control: AbstractControl<string>): ValidationErrors | null {
  return control.value && control.value > todayIso() ? { future: true } : null;
}

/** Creates a client or, when one is given, edits it. Warns before saving a repeated first and last name. */
@Component({
  selector: 'app-client-form-dialog',
  imports: [ReactiveFormsModule, ModalDialog],
  templateUrl: './client-form-dialog.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientFormDialog implements OnInit {
  private readonly clients = inject(ClientsService);
  private readonly destroyRef = inject(DestroyRef);

  readonly client = input<Client | null>(null);
  readonly saved = output<void>();
  readonly cancelled = output<void>();

  protected readonly heading = computed(() => (this.client() ? 'Editar cliente' : 'Nuevo cliente'));
  protected readonly today = todayIso();
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly duplicateWarning = signal(false);

  protected readonly form = new FormGroup({
    firstName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)],
    }),
    lastName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)],
    }),
    phone: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(30)] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.pattern(EMAIL_PATTERN), Validators.maxLength(254)] }),
    birthDate: new FormControl('', { nonNullable: true, validators: [notInTheFuture] }),
    notes: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(2000)] }),
  });

  ngOnInit(): void {
    const client = this.client();
    if (client) {
      this.form.setValue({
        firstName: client.firstName,
        lastName: client.lastName,
        phone: client.phone ?? '',
        email: client.email ?? '',
        birthDate: client.birthDate ?? '',
        notes: client.notes ?? '',
      });
    }
    // Changing the name after the warning makes it obsolete: check again on the next save.
    merge(this.form.controls.firstName.valueChanges, this.form.controls.lastName.valueChanges)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.duplicateWarning.set(false));
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

    const { firstName, lastName } = this.form.getRawValue();
    this.isSubmitting.set(true);
    this.clients
      .nameExists(firstName.trim(), lastName.trim(), this.client()?.id ?? null)
      .subscribe({
        next: (exists) => {
          if (exists) {
            this.duplicateWarning.set(true);
            this.isSubmitting.set(false);
          } else {
            this.save();
          }
        },
        error: (error: unknown) => this.fail(error),
      });
  }

  protected saveAnyway(): void {
    this.errorMessage.set(null);
    this.isSubmitting.set(true);
    this.save();
  }

  protected reviewData(): void {
    this.duplicateWarning.set(false);
  }

  private save(): void {
    const { firstName, lastName, phone, email, birthDate, notes } = this.form.getRawValue();
    const request: ClientRequest = {
      firstName,
      lastName,
      phone: phone.trim() || null,
      birthDate: birthDate || null,
      notes: notes.trim() || null,
      email: email.trim() || null,
    };
    const client = this.client();
    const request$: Observable<Client> = client ? this.clients.update(client.id, request) : this.clients.create(request);

    request$.pipe(finalize(() => this.isSubmitting.set(false))).subscribe({
      next: () => this.saved.emit(),
      error: (error: unknown) => this.fail(error),
    });
  }

  private fail(error: unknown): void {
    this.isSubmitting.set(false);
    this.errorMessage.set(apiErrorMessage(error, 'No pudimos guardar el cliente. Intentá nuevamente.'));
  }
}
