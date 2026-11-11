import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { PageHelp } from '../../../../shared/components/page-help/page-help';
import { RouterLink } from '@angular/router';
import { BirthdaysPanel } from '../../../clients/components/birthdays-panel/birthdays-panel';
import { SummaryCards } from '../../../reports/components/summary-cards/summary-cards';
import { resolvePreset } from '../../../reports/utils/periods';
import { createReportLoader } from '../../../reports/utils/report-loader';
import { formatDate, formatPrice } from '../../../../shared/utils/format';
import { DashboardService } from '../../services/dashboard.service';

/** Operational overview for the current month, with links to the detailed modules. */
@Component({
  selector: 'app-home-page',
  imports: [PageHelp, RouterLink, BirthdaysPanel, SummaryCards],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
})
export class HomePage {
  private readonly dashboard = inject(DashboardService);

  protected readonly currentMonth = resolvePreset('this-month', new Date());
  private readonly month = signal(this.currentMonth);
  private readonly recentLimit = signal(5);

  protected readonly recent = createReportLoader(
    this.recentLimit,
    (limit) => this.dashboard.recentTreatments(limit),
    'No pudimos cargar los últimos tratamientos.',
  );
  protected readonly popular = createReportLoader(
    this.month,
    (period) => this.dashboard.topServices(period, 4),
    'No pudimos cargar los servicios más realizados.',
  );

  protected readonly formatDate = formatDate;
  protected readonly formatPrice = formatPrice;
}
