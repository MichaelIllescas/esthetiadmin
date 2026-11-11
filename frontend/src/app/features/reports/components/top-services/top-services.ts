import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ChartCanvas } from '../../../../shared/components/chart/chart-canvas';
import { formatPrice } from '../../../../shared/utils/format';
import { Period, TopService } from '../../models/report.models';
import { ReportsService } from '../../services/reports.service';
import { barsHeight, topServicesSpec } from '../../utils/chart-specs';
import { createReportLoader } from '../../utils/report-loader';

/** The most performed services of the period, as bars and as a table. */
@Component({
  selector: 'app-top-services',
  imports: [ChartCanvas],
  templateUrl: './top-services.html',
  styleUrl: './top-services.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopServices {
  private readonly reports = inject(ReportsService);

  readonly period = input.required<Period>();
  protected readonly report = createReportLoader(
    this.period,
    (period) => this.reports.topServices(period, 10),
    'No pudimos cargar los servicios más realizados. Intentá nuevamente.',
  );

  protected readonly services = computed<TopService[]>(() => this.report.data() ?? []);
  protected readonly spec = computed(() => topServicesSpec(this.services()));
  protected readonly height = computed(() => barsHeight(this.services().length));
  protected readonly label = computed(() => {
    const top = this.services()[0];
    return top
      ? `Gráfico de barras con la cantidad de tratamientos de cada servicio. El más realizado es ${top.serviceName}, con ${top.treatments}. Los mismos datos están en la tabla.`
      : 'Sin datos';
  });

  protected readonly formatPrice = formatPrice;
}
