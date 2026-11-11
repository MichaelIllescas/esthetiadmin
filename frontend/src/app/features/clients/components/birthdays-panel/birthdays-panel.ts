import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { formatDayAndMonth } from '../../../../shared/utils/format';
import { Birthday } from '../../models/client.models';
import { ClientsService } from '../../services/clients.service';

const WINDOW_DAYS = 30;

/** The birthdays of the next 30 days. It reloads whenever `refreshToken` changes. */
@Component({
  selector: 'app-birthdays-panel',
  imports: [RouterLink],
  templateUrl: './birthdays-panel.html',
  styleUrl: './birthdays-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BirthdaysPanel {
  private readonly clients = inject(ClientsService);

  readonly refreshToken = input(0);

  protected readonly windowDays = WINDOW_DAYS;
  protected readonly birthdays = signal<Birthday[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly collapsed = signal(false);
  protected readonly formatDayAndMonth = formatDayAndMonth;

  constructor() {
    effect(() => {
      this.refreshToken();
      this.load();
    });
  }

  protected load(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.clients.birthdays(WINDOW_DAYS).subscribe({
      next: (birthdays) => {
        this.birthdays.set(birthdays);
        this.isLoading.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(apiErrorMessage(error, 'No pudimos cargar los cumpleaños.'));
        this.isLoading.set(false);
      },
    });
  }

  protected toggle(): void {
    this.collapsed.update((value) => !value);
  }

  protected when(birthday: Birthday): string {
    if (birthday.daysUntil === 0) {
      return 'Hoy';
    }
    return birthday.daysUntil === 1 ? 'Mañana' : `En ${birthday.daysUntil} días`;
  }
}
