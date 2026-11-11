import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { ModalDialog } from '../../../../shared/components/modal-dialog/modal-dialog';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { Appointment, NoticeType } from '../../models/appointment.models';
import { AppointmentsService } from '../../services/appointments.service';
import { dateOf, formatWeekday, timeOf } from '../../utils/agenda-dates';

const WHAT: Record<NoticeType, string> = {
  BOOKED: 'la confirmación del turno',
  RESCHEDULED: 'el aviso del cambio de turno',
  CANCELLED: 'el aviso de la cancelación',
};

/**
 * Asks the owner whether to email the client about her appointment (ADR 016). Without an email the option is
 * shown disabled, so she knows why nothing can be sent.
 */
@Component({
  selector: 'app-notice-dialog',
  imports: [ModalDialog],
  templateUrl: './notice-dialog.html',
  styles: `
    p { margin: 0 0 12px; line-height: 1.5; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NoticeDialog {
  private readonly appointments = inject(AppointmentsService);

  readonly appointment = input.required<Appointment>();
  readonly type = input.required<NoticeType>();
  readonly sent = output<void>();
  readonly skipped = output<void>();

  protected readonly isSending = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly when = computed(() => {
    const startsAt = this.appointment().startsAt;
    return `${formatWeekday(dateOf(startsAt))} a las ${timeOf(startsAt)}`;
  });
  protected readonly what = computed(() => WHAT[this.type()]);

  protected send(): void {
    this.isSending.set(true);
    this.errorMessage.set(null);
    this.appointments
      .sendNotice(this.appointment().id, this.type())
      .pipe(finalize(() => this.isSending.set(false)))
      .subscribe({
        next: () => this.sent.emit(),
        error: (error: unknown) =>
          this.errorMessage.set(apiErrorMessage(error, 'No pudimos enviar el email. Intentá nuevamente.')),
      });
  }
}
