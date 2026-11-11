import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { formatPrice } from '../../../../shared/utils/format';
import { Period } from '../../models/report.models';
import { ReportsService } from '../../services/reports.service';
import { createReportLoader } from '../../utils/report-loader';

/** Income, expenses and the difference of the period. */
@Component({
  selector: 'app-summary-cards',
  templateUrl: './summary-cards.html',
  styleUrl: './summary-cards.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SummaryCards {
  private readonly reports = inject(ReportsService);

  readonly period = input.required<Period>();
  protected readonly report = createReportLoader(
    this.period,
    (period) => this.reports.summary(period),
    'No pudimos cargar el resumen. Intentá nuevamente.',
  );

  protected readonly formatPrice = formatPrice;

  protected plural(count: number, one: string, many: string): string {
    return `${count} ${count === 1 ? one : many}`;
  }

  protected balanceKind(balance: number): 'positive' | 'negative' | 'even' {
    return balance > 0 ? 'positive' : balance < 0 ? 'negative' : 'even';
  }

  protected balanceCaption(balance: number): string {
    switch (this.balanceKind(balance)) {
      case 'positive':
        return 'Ingresaron más de lo que se gastó';
      case 'negative':
        return 'Se gastó más de lo que ingresó';
      default:
        return 'Ingresos y gastos iguales';
    }
  }
}
