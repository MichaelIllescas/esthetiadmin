import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { finalize, Observable, of, switchMap } from 'rxjs';
import { ModalDialog } from '../../../../shared/components/modal-dialog/modal-dialog';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { ClientPicker } from '../../../clients/components/client-picker/client-picker';
import { Client } from '../../../clients/models/client.models';
import { ClientsService } from '../../../clients/services/clients.service';
import { Appointment } from '../../models/appointment.models';
import { AppointmentsService } from '../../services/appointments.service';

type ClientChoice = 'existing' | 'new';

/**
 * Marks an appointment as attended. An attended appointment belongs to a client, so one booked from the public page
 * is first linked to a registered client or turned into a new one with the data she left.
 */
@Component({
  selector: 'app-attend-dialog',
  imports: [ModalDialog, ClientPicker],
  templateUrl: './attend-dialog.html',
  styles: `
    .summary { margin: 0; }
    .choice { display: grid; gap: 8px; margin: 0; padding: 0; border: 0; }
    .choice legend { margin-bottom: 8px; font-weight: 700; }
    .option { display: flex; align-items: center; gap: 10px; min-height: 44px; cursor: pointer; }
    .option input { width: 20px; height: 20px; margin: 0; accent-color: var(--rose-deep); }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AttendDialog {
  private readonly appointments = inject(AppointmentsService);
  private readonly clients = inject(ClientsService);

  readonly appointment = input.required<Appointment>();
  /** The appointment, now attended and linked to its client. */
  readonly attended = output<Appointment>();
  readonly cancelled = output<void>();

  protected readonly needsClient = computed(() => this.appointment().clientId === null);
  protected readonly choice = signal<ClientChoice>('existing');
  protected readonly picked = signal<Client | null>(null);
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly missingClient = signal(false);

  protected choose(choice: ClientChoice): void {
    this.choice.set(choice);
    this.missingClient.set(false);
  }

  protected onPicked(client: Client | null): void {
    this.picked.set(client);
    this.missingClient.set(false);
  }

  protected confirm(): void {
    this.errorMessage.set(null);
    const appointment = this.appointment();
    if (this.needsClient() && this.choice() === 'existing' && !this.picked()) {
      this.missingClient.set(true);
      return;
    }

    this.isSubmitting.set(true);
    this.linkClient(appointment)
      .pipe(
        switchMap(() => this.appointments.changeStatus(appointment.id, 'ATTENDED')),
        finalize(() => this.isSubmitting.set(false)),
      )
      .subscribe({
        next: (attended) => this.attended.emit(attended),
        error: (error: unknown) =>
          this.errorMessage.set(apiErrorMessage(error, 'No pudimos marcar el turno como atendido. Intentá nuevamente.')),
      });
  }

  private linkClient(appointment: Appointment): Observable<unknown> {
    if (!this.needsClient()) {
      return of(null);
    }
    const client$: Observable<Client> =
      this.choice() === 'existing'
        ? of(this.picked() as Client)
        : this.clients.create({
            firstName: appointment.firstName,
            lastName: appointment.lastName,
            phone: appointment.phone,
            birthDate: null,
            notes: null,
            email: appointment.email,
          });
    return client$.pipe(switchMap((client) => this.appointments.linkClient(appointment.id, client.id)));
  }
}
