import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ChartCanvas } from '../../../../shared/components/chart/chart-canvas';
import { formatMonth, formatPrice } from '../../../../shared/utils/format';
import { MonthlyRow, Period } from '../../models/report.models';
import { ReportsService } from '../../services/reports.service';
import { monthlySpec } from '../../utils/chart-specs';
import { createReportLoader } from '../../utils/report-loader';

/** Income, expenses and result month by month as lines, with the same data available as a table. */
@Component({
  selector: 'app-monthly-chart',
  imports: [ChartCanvas],
  templateUrl: './monthly-chart.html',
  styleUrl: './monthly-chart.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonthlyChart {
  private readonly reports = inject(ReportsService);

  readonly period = input.required<Period>();
  protected readonly report = createReportLoader(
    this.period,
    (period) => this.reports.monthly(period),
    'No pudimos cargar la evolución mensual. Intentá nuevamente.',
  );

  protected readonly months = computed<MonthlyRow[]>(() => this.report.data() ?? []);
  protected readonly hasMovement = computed(() => this.months().some((row) => row.income !== 0 || row.expenses !== 0));
  protected readonly spec = computed(() => monthlySpec(this.months()));
  protected readonly label = computed(() => {
    const rows = this.months();
    if (rows.length === 0) {
      return 'Sin datos';
    }
    return `Gráfico de líneas con los ingresos, los gastos y el resultado de cada mes, de ${formatMonth(rows[0].month)} a ${formatMonth(rows[rows.length - 1].month)}. Los mismos datos están en la tabla de abajo.`;
  });

  protected readonly formatMonth = formatMonth;
  protected readonly formatPrice = formatPrice;
}
