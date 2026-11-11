import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { todayIso } from '../../../../shared/utils/format';
import { addDays, formatWeekday } from '../../../appointments/utils/agenda-dates';
import { ChosenService } from '../../models/booking.models';
import { BookingService } from '../../services/booking.service';
import { addMonths, buildMonth, formatMonth, monthOf } from '../../utils/month-calendar';

/** How far ahead the public page lets people book (ADR 013). */
const DAYS_AHEAD = 60;

export interface PickedSlot {
  date: string;
  time: string;
}

/**
 * Step 2: a month calendar where only the days with a free time can be chosen, and then the free times of that
 * day. It can open on a day already chosen, to pick another time when the first one was taken meanwhile.
 */
@Component({
  selector: 'app-slot-picker',
  templateUrl: './slot-picker.html',
  styleUrl: './slot-picker.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SlotPicker implements OnInit {
  private readonly booking = inject(BookingService);

  readonly service = input.required<ChosenService>();
  readonly initialDate = input<string | null>(null);
  readonly picked = output<PickedSlot>();

  protected readonly weekdays = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
  protected readonly today = todayIso();
  private readonly firstMonth = monthOf(this.today);
  private readonly lastMonth = monthOf(addDays(this.today, DAYS_AHEAD));

  protected readonly month = signal(this.firstMonth);
  protected readonly availableDays = signal<ReadonlySet<string>>(new Set());
  protected readonly daysLoading = signal(true);
  protected readonly daysError = signal<string | null>(null);
  protected readonly selectedDate = signal<string | null>(null);
  protected readonly times = signal<string[]>([]);
  protected readonly timesLoading = signal(false);
  protected readonly timesError = signal<string | null>(null);

  protected readonly weeks = computed(() => buildMonth(this.month()));
  protected readonly monthLabel = computed(() => formatMonth(this.month()));
  protected readonly canGoBack = computed(() => this.month() > this.firstMonth);
  protected readonly canGoForward = computed(() => this.month() < this.lastMonth);
  protected readonly formatWeekday = formatWeekday;

  ngOnInit(): void {
    const initial = this.initialDate();
    if (initial) {
      this.month.set(monthOf(initial));
      this.selectDate(initial);
    }
    this.loadDays();
  }

  protected changeMonth(steps: number): void {
    this.month.update((month) => addMonths(month, steps));
    this.loadDays();
  }

  protected loadDays(): void {
    this.daysLoading.set(true);
    this.daysError.set(null);
    this.booking
      .availableDays(this.service().id, this.month())
      .pipe(finalize(() => this.daysLoading.set(false)))
      .subscribe({
        next: (result) => this.availableDays.set(new Set(result.days)),
        error: (error: unknown) =>
          this.daysError.set(apiErrorMessage(error, 'No pudimos cargar los días disponibles. Intentá nuevamente.')),
      });
  }

  protected isAvailable(date: string): boolean {
    return this.availableDays().has(date);
  }

  protected selectDate(date: string): void {
    this.selectedDate.set(date);
    this.times.set([]);
    this.timesLoading.set(true);
    this.timesError.set(null);
    this.booking
      .freeTimes(this.service().id, date)
      .pipe(finalize(() => this.timesLoading.set(false)))
      .subscribe({
        next: (result) => this.times.set(result.times),
        error: (error: unknown) =>
          this.timesError.set(apiErrorMessage(error, 'No pudimos cargar los horarios. Intentá nuevamente.')),
      });
  }

  protected dayLabel(date: string): string {
    return `${formatWeekday(date)}${this.isAvailable(date) ? '' : ', sin turnos'}`;
  }

  protected dayNumber(date: string): number {
    return Number(date.slice(8));
  }
}
