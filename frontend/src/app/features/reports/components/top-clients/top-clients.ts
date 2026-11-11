import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { formatPrice } from '../../../../shared/utils/format';
import { ClientRanking, Period, TopClient } from '../../models/report.models';
import { ReportsService } from '../../services/reports.service';
import { createReportLoader } from '../../utils/report-loader';

/** The clients of the period ranked by what they paid or by how many treatments they had; the user picks. */
@Component({
  selector: 'app-top-clients',
  imports: [RouterLink],
  templateUrl: './top-clients.html',
  styleUrl: './top-clients.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopClients {
  private readonly reports = inject(ReportsService);

  readonly period = input.required<Period>();
  protected readonly sortBy = signal<ClientRanking>('amount');

  private readonly params = computed(() => ({ period: this.period(), sortBy: this.sortBy() }));
  protected readonly report = createReportLoader(
    this.params,
    ({ period, sortBy }) => this.reports.topClients(period, sortBy, 10),
    'No pudimos cargar el ranking de clientes. Intentá nuevamente.',
  );

  protected readonly clients = computed<TopClient[]>(() => this.report.data() ?? []);
  protected readonly formatPrice = formatPrice;

  protected setSort(order: ClientRanking): void {
    this.sortBy.set(order);
  }

  protected ariaSort(order: ClientRanking): 'descending' | 'none' {
    return this.sortBy() === order ? 'descending' : 'none';
  }
}
