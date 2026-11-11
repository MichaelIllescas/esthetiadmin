import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { PageHelp } from '../../../../shared/components/page-help/page-help';
import { IncomeBreakdownView } from '../../components/income-breakdown/income-breakdown';
import { MonthlyChart } from '../../components/monthly-chart/monthly-chart';
import { PeriodPicker } from '../../components/period-picker/period-picker';
import { SummaryCards } from '../../components/summary-cards/summary-cards';
import { TopClients } from '../../components/top-clients/top-clients';
import { TopServices } from '../../components/top-services/top-services';
import { Period } from '../../models/report.models';
import { resolvePreset } from '../../utils/periods';

@Component({
  selector: 'app-reports-page',
  imports: [PageHelp, PeriodPicker, SummaryCards, MonthlyChart, TopServices, IncomeBreakdownView, TopClients],
  templateUrl: './reports-page.html',
  styleUrl: './reports-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportsPage {
  /** Null until the period picker announces its starting period, so no report asks for anything before that. */
  protected readonly period = signal<Period | null>(null);

  /** The evolution always compares the last 12 whole months, whatever period the rest of the page shows. */
  protected readonly lastYear = resolvePreset('last-12-months', new Date());
}
