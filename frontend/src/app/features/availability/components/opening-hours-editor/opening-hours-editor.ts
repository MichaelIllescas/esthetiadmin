import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { Icon } from '../../../../shared/components/icon/icon';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { AvailabilityRule, DayOfWeek, WEEK_DAYS } from '../../models/availability.models';
import { AvailabilityService } from '../../services/availability.service';
import { dayRangesError, formatRange, HALF_HOURS, rulesByDay } from '../../utils/schedule-times';

type Range = Pick<AvailabilityRule, 'startTime' | 'endTime'>;
type Week = ReadonlyMap<DayOfWeek, readonly Range[]>;

/** A new range starts with the usual morning shift; she adjusts it from there. */
const NEW_RANGE: Range = { startTime: '09:00', endTime: '13:00' };

/**
 * Shows the week of opening hours as text and, on demand, edits it and saves it in one go (the API replaces
 * the week). With nothing configured yet it opens straight in edit mode.
 */
@Component({
  selector: 'app-opening-hours-editor',
  imports: [Icon],
  templateUrl: './opening-hours-editor.html',
  styleUrl: './opening-hours-editor.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OpeningHoursEditor implements OnInit {
  private readonly availability = inject(AvailabilityService);

  protected readonly days = WEEK_DAYS;
  protected readonly times = HALF_HOURS;
  protected readonly week = signal<Week>(rulesByDay([]));
  protected readonly isLoading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly isSaving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly saved = signal(false);
  protected readonly dirty = signal(false);
  protected readonly editing = signal(false);
  /** What the API last confirmed, to go back to when an edit is cancelled. */
  protected savedRules: readonly AvailabilityRule[] = [];

  protected readonly errors = computed(() => {
    const errors = new Map<DayOfWeek, string>();
    for (const [day, ranges] of this.week()) {
      const error = dayRangesError(ranges);
      if (error) {
        errors.set(day, error);
      }
    }
    return errors;
  });
  protected readonly canSave = computed(() => this.dirty() && this.errors().size === 0 && !this.isSaving());

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.isLoading.set(true);
    this.loadError.set(null);
    this.availability
      .getRules()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (rules) => {
          this.reset(rules);
          this.editing.set(rules.length === 0);
        },
        error: (error: unknown) =>
          this.loadError.set(apiErrorMessage(error, 'No pudimos cargar los horarios. Intentá nuevamente.')),
      });
  }

  protected rangesOf(day: DayOfWeek): readonly Range[] {
    return this.week().get(day) ?? [];
  }

  /** "09:00 a 13:00 y 15:00 a 18:30", or "Cerrado". */
  protected summaryOf(day: DayOfWeek): string {
    const ranges = this.rangesOf(day);
    return ranges.length === 0 ? 'Cerrado' : ranges.map((range) => formatRange(range.startTime, range.endTime)).join(' y ');
  }

  protected startEditing(): void {
    this.saved.set(false);
    this.saveError.set(null);
    this.editing.set(true);
  }

  protected cancelEditing(): void {
    this.reset(this.savedRules);
    this.saveError.set(null);
    this.editing.set(false);
  }

  protected addRange(day: DayOfWeek): void {
    const ranges = this.rangesOf(day);
    const last = ranges.at(-1);
    // After an existing range, propose the afternoon shift so the new one does not overlap.
    const next = last && last.endTime < '15:00' ? { startTime: '15:00', endTime: '19:00' } : NEW_RANGE;
    this.change(day, [...ranges, next]);
  }

  protected removeRange(day: DayOfWeek, index: number): void {
    this.change(day, this.rangesOf(day).filter((_, position) => position !== index));
  }

  protected setTime(day: DayOfWeek, index: number, field: keyof Range, event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.change(day, this.rangesOf(day).map((range, position) => (position === index ? { ...range, [field]: value } : range)));
  }

  protected save(): void {
    if (!this.canSave()) {
      return;
    }
    const rules: AvailabilityRule[] = [...this.week()].flatMap(([dayOfWeek, ranges]) =>
      ranges.map((range) => ({ dayOfWeek, ...range })),
    );
    this.isSaving.set(true);
    this.saveError.set(null);
    this.availability
      .saveRules(rules)
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: (savedRules) => {
          this.reset(savedRules);
          this.editing.set(false);
          this.saved.set(true);
        },
        error: (error: unknown) =>
          this.saveError.set(apiErrorMessage(error, 'No pudimos guardar los horarios. Intentá nuevamente.')),
      });
  }

  private change(day: DayOfWeek, ranges: readonly Range[]): void {
    this.week.update((week) => new Map(week).set(day, ranges));
    this.dirty.set(true);
    this.saved.set(false);
  }

  private reset(rules: readonly AvailabilityRule[]): void {
    this.savedRules = rules;
    this.week.set(rulesByDay(rules));
    this.dirty.set(false);
  }
}
