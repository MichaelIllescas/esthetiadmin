import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ChartCanvas } from '../../../../shared/components/chart/chart-canvas';
import { formatPercent, formatPrice } from '../../../../shared/utils/format';
import { CategoryIncome, Period } from '../../models/report.models';
import { ReportsService } from '../../services/reports.service';
import { breakdownSpec, SLICE_COLORS } from '../../utils/chart-specs';
import { createReportLoader } from '../../utils/report-loader';

/** Income of the period by category (a doughnut), with each category opening into its services. */
@Component({
  selector: 'app-income-breakdown',
  imports: [ChartCanvas],
  templateUrl: './income-breakdown.html',
  styleUrl: './income-breakdown.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IncomeBreakdownView {
  private readonly reports = inject(ReportsService);

  readonly period = input.required<Period>();
  protected readonly report = createReportLoader(
    this.period,
    (period) => this.reports.incomeByService(period),
    'No pudimos cargar los ingresos por servicio. Intentá nuevamente.',
  );

  protected readonly categories = computed<CategoryIncome[]>(() => this.report.data()?.categories ?? []);
  protected readonly hasIncome = computed(() => (this.report.data()?.totalIncome ?? 0) > 0);
  protected readonly spec = computed(() => breakdownSpec(this.categories()));
  protected readonly label = computed(() => {
    const top = this.categories()[0];
    return top
      ? `Gráfico de anillo con el ingreso de cada categoría. La que más aportó es ${top.categoryName}, con el ${formatPercent(top.percentage)} del total. El detalle está en la lista de abajo.`
      : 'Sin datos';
  });

  protected readonly formatPrice = formatPrice;
  protected readonly formatPercent = formatPercent;

  protected color(index: number): string {
    return SLICE_COLORS[index % SLICE_COLORS.length];
  }
}
