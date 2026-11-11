import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { Icon } from '../../../../shared/components/icon/icon';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { formatDate } from '../../../../shared/utils/format';
import { AffectedAppointment, AvailabilityBlock } from '../../models/availability.models';
import { AvailabilityService } from '../../services/availability.service';
import { formatDateRange, formatRange } from '../../utils/schedule-times';
import { BlockFormDialog } from '../block-form-dialog/block-form-dialog';

type DialogState = { type: 'form' } | { type: 'delete'; block: AvailabilityBlock };

/** The upcoming dates taken out of the schedule, with the actions to add and remove them. */
@Component({
  selector: 'app-block-list',
  imports: [BlockFormDialog, ConfirmDialog, Icon],
  templateUrl: './block-list.html',
  styleUrl: './block-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BlockList implements OnInit {
  private readonly availability = inject(AvailabilityService);

  protected readonly blocks = signal<AvailabilityBlock[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly dialog = signal<DialogState | null>(null);
  protected readonly dialogBusy = signal(false);
  protected readonly dialogError = signal<string | null>(null);
  /** Appointments left inside the last block created, so she can decide what to do with them. */
  protected readonly affected = signal<AffectedAppointment[]>([]);

  protected readonly formatDateRange = formatDateRange;

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.isLoading.set(true);
    this.loadError.set(null);
    this.availability
      .getBlocks()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (blocks) => this.blocks.set(blocks),
        error: (error: unknown) =>
          this.loadError.set(apiErrorMessage(error, 'No pudimos cargar los bloqueos. Intentá nuevamente.')),
      });
  }

  protected hours(block: AvailabilityBlock): string {
    return block.startTime && block.endTime ? formatRange(block.startTime, block.endTime) : 'Todo el día';
  }

  protected openForm(): void {
    this.dialog.set({ type: 'form' });
  }

  protected askDelete(block: AvailabilityBlock): void {
    this.dialogError.set(null);
    this.dialog.set({ type: 'delete', block });
  }

  protected closeDialog(): void {
    this.dialog.set(null);
  }

  protected onCreated(block: AvailabilityBlock): void {
    this.dialog.set(null);
    this.affected.set(block.affectedAppointments);
    this.load();
  }

  protected confirmDelete(block: AvailabilityBlock): void {
    this.dialogBusy.set(true);
    this.dialogError.set(null);
    this.availability
      .deleteBlock(block.id)
      .pipe(finalize(() => this.dialogBusy.set(false)))
      .subscribe({
        next: () => {
          this.dialog.set(null);
          this.load();
        },
        error: (error: unknown) =>
          this.dialogError.set(apiErrorMessage(error, 'No pudimos eliminar el bloqueo. Intentá nuevamente.')),
      });
  }

  /** "12/10 10:30, Carina Diaz" */
  protected describe(appointment: AffectedAppointment): string {
    const [date, time] = appointment.startsAt.split('T');
    return `${formatDate(date)} ${time.slice(0, 5)}, ${appointment.firstName} ${appointment.lastName}`;
  }
}
