import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { todayIso } from '../../../../shared/utils/format';
import { TreatmentFormDialog } from '../../../treatments/components/treatment-form-dialog/treatment-form-dialog';
import { Appointment, NoticeType, STATUS_LABELS } from '../../models/appointment.models';
import { AppointmentsService } from '../../services/appointments.service';
import { addDays, dateOf, daysBetween, formatWeekday, mondayOf, timeOf } from '../../utils/agenda-dates';
import { AppointmentFormDialog } from '../appointment-form-dialog/appointment-form-dialog';
import { AttendDialog } from '../attend-dialog/attend-dialog';
import { NoticeDialog } from '../notice-dialog/notice-dialog';

type View = 'day' | 'week';
type Ending = 'CANCELLED' | 'NO_SHOW';

type DialogState =
  | { type: 'form'; appointment: Appointment | null }
  | { type: 'attend'; appointment: Appointment }
  | { type: 'end'; appointment: Appointment; status: Ending }
  | { type: 'treatment'; appointment: Appointment }
  | { type: 'notice'; appointment: Appointment; notice: NoticeType };

interface AgendaDay {
  date: string;
  label: string;
  isToday: boolean;
  appointments: Appointment[];
}

const ENDINGS: Record<Ending, { heading: string; verb: string; label: string }> = {
  CANCELLED: { heading: 'Cancelar turno', verb: 'cancelar', label: 'Cancelar turno' },
  NO_SHOW: { heading: 'Marcar que no vino', verb: 'marcar que no vino a', label: 'Marcar que no vino' },
};

/** The owner's agenda by day or by week, with every action on an appointment. */
@Component({
  selector: 'app-agenda',
  imports: [AppointmentFormDialog, AttendDialog, ConfirmDialog, TreatmentFormDialog, NoticeDialog],
  templateUrl: './agenda.html',
  styleUrl: './agenda.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Agenda implements OnInit {
  private readonly appointments = inject(AppointmentsService);

  protected readonly today = todayIso();
  protected readonly view = signal<View>('day');
  /** Any day inside the period shown. */
  protected readonly anchor = signal(todayIso());
  protected readonly items = signal<Appointment[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly dialog = signal<DialogState | null>(null);
  protected readonly dialogBusy = signal(false);
  protected readonly dialogError = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  protected readonly range = computed(() => {
    const anchor = this.anchor();
    if (this.view() === 'day') {
      return { from: anchor, to: anchor };
    }
    const monday = mondayOf(anchor);
    return { from: monday, to: addDays(monday, 6) };
  });

  protected readonly title = computed(() => {
    const { from, to } = this.range();
    return from === to ? formatWeekday(from) : `Semana del ${formatWeekday(from)} al ${formatWeekday(to)}`;
  });

  protected readonly days = computed<AgendaDay[]>(() => {
    const { from, to } = this.range();
    return daysBetween(from, to).map((date) => ({
      date,
      label: formatWeekday(date),
      isToday: date === this.today,
      appointments: this.items().filter((appointment) => dateOf(appointment.startsAt) === date),
    }));
  });

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly endings = ENDINGS;
  protected readonly timeOf = timeOf;

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    const { from, to } = this.range();
    this.isLoading.set(true);
    this.loadError.set(null);
    this.appointments
      .search(from, to)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (items) => this.items.set(items),
        error: (error: unknown) =>
          this.loadError.set(apiErrorMessage(error, 'No pudimos cargar la agenda. Intentá nuevamente.')),
      });
  }

  protected setView(view: View): void {
    this.view.set(view);
    this.load();
  }

  protected move(steps: number): void {
    this.anchor.update((anchor) => addDays(anchor, steps * (this.view() === 'day' ? 1 : 7)));
    this.load();
  }

  protected goToday(): void {
    this.anchor.set(this.today);
    this.load();
  }

  protected openForm(appointment: Appointment | null): void {
    this.notice.set(null);
    this.dialog.set({ type: 'form', appointment });
  }

  protected openAttend(appointment: Appointment): void {
    this.notice.set(null);
    this.dialog.set({ type: 'attend', appointment });
  }

  protected askEnd(appointment: Appointment, status: Ending): void {
    this.notice.set(null);
    this.dialogError.set(null);
    this.dialog.set({ type: 'end', appointment, status });
  }

  protected closeDialog(): void {
    this.dialog.set(null);
  }

  /** After saving, she is asked whether to email the client: a confirmation if new, a change notice if moved. */
  protected onSaved(appointment: Appointment): void {
    const current = this.dialog();
    const moved = current?.type === 'form' && current.appointment !== null;
    this.dialog.set({ type: 'notice', appointment, notice: moved ? 'RESCHEDULED' : 'BOOKED' });
    this.anchor.set(dateOf(appointment.startsAt));
    this.notice.set(`Turno guardado: ${formatWeekday(dateOf(appointment.startsAt))} a las ${timeOf(appointment.startsAt)}.`);
    this.load();
  }

  protected openNotice(appointment: Appointment, notice: NoticeType): void {
    this.notice.set(null);
    this.dialog.set({ type: 'notice', appointment, notice });
  }

  protected onNoticeSent(appointment: Appointment): void {
    this.dialog.set(null);
    this.notice.set(`Email enviado a ${appointment.email}.`);
  }

  /** Once attended, she is offered to register the treatment with the service and date already filled in. */
  protected onAttended(appointment: Appointment): void {
    this.dialog.set({ type: 'treatment', appointment });
    this.load();
  }

  protected onTreatmentSaved(): void {
    this.dialog.set(null);
    this.notice.set('Tratamiento registrado.');
  }

  protected confirmEnd(appointment: Appointment, status: Ending): void {
    this.dialogBusy.set(true);
    this.dialogError.set(null);
    this.appointments
      .changeStatus(appointment.id, status)
      .pipe(finalize(() => this.dialogBusy.set(false)))
      .subscribe({
        next: (updated) => {
          // A cancellation can be told to the client; a no-show is not.
          this.dialog.set(status === 'CANCELLED' ? { type: 'notice', appointment: updated, notice: 'CANCELLED' } : null);
          this.load();
        },
        error: (error: unknown) =>
          this.dialogError.set(apiErrorMessage(error, 'No pudimos actualizar el turno. Intentá nuevamente.')),
      });
  }

  protected fullName(appointment: Appointment): string {
    return `${appointment.firstName} ${appointment.lastName}`;
  }

  protected clientOf(appointment: Appointment): { id: number; name: string } {
    return { id: appointment.clientId as number, name: `${appointment.lastName}, ${appointment.firstName}` };
  }

  protected presetOf(appointment: Appointment): { serviceId: number; performedOn: string } {
    // A treatment cannot be dated in the future: one attended ahead of time is registered today.
    const date = dateOf(appointment.startsAt);
    return { serviceId: appointment.serviceId, performedOn: date > this.today ? this.today : date };
  }

  protected endMessage(appointment: Appointment, status: Ending): string {
    return `¿Querés ${ENDINGS[status].verb} el turno de ${this.fullName(appointment)} del ${formatWeekday(dateOf(appointment.startsAt))} a las ${timeOf(appointment.startsAt)}? El horario vuelve a quedar libre.`;
  }
}
