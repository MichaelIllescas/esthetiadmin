import { ChangeDetectionStrategy, Component, OnInit, output, signal } from '@angular/core';
import { formatDate } from '../../../../shared/utils/format';
import { Period } from '../../models/report.models';
import { PRESETS, PresetId, rangeProblem, resolvePreset } from '../../utils/periods';

type Selection = PresetId | 'custom';

/**
 * Chooses the period every report of the screen is about: a quick one (this month, this year...) or two dates.
 * It announces the starting period as soon as it is shown, and again every time a valid one is chosen.
 */
@Component({
  selector: 'app-period-picker',
  templateUrl: './period-picker.html',
  styleUrl: './period-picker.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PeriodPicker implements OnInit {
  readonly changed = output<Period>();

  protected readonly presets = PRESETS;
  protected readonly selected = signal<Selection>('this-month');
  protected readonly current = signal<Period>(resolvePreset('this-month', new Date()));
  protected readonly customFrom = signal('');
  protected readonly customTo = signal('');
  protected readonly problem = signal<string | null>(null);
  protected readonly formatDate = formatDate;

  ngOnInit(): void {
    this.changed.emit(this.current());
  }

  protected choose(id: PresetId): void {
    const period = resolvePreset(id, new Date());
    this.selected.set(id);
    this.problem.set(null);
    this.current.set(period);
    this.changed.emit(period);
  }

  /** Opens the two dates starting from the period that is being shown, without changing the reports yet. */
  protected chooseCustom(): void {
    this.selected.set('custom');
    this.customFrom.set(this.current().from);
    this.customTo.set(this.current().to);
    this.problem.set(null);
  }

  protected onCustomChange(field: 'from' | 'to', event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    if (field === 'from') {
      this.customFrom.set(value);
    } else {
      this.customTo.set(value);
    }

    const problem = rangeProblem(this.customFrom(), this.customTo());
    this.problem.set(problem);
    if (problem === null) {
      const period = { from: this.customFrom(), to: this.customTo() };
      this.current.set(period);
      this.changed.emit(period);
    }
  }
}
