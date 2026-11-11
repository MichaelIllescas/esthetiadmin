import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { Treatment } from '../../treatments/models/treatment.models';
import { TreatmentsService } from '../../treatments/services/treatments.service';
import { Period, TopService } from '../../reports/models/report.models';
import { ReportsService } from '../../reports/services/reports.service';

const EMPTY_TREATMENT_FILTERS = {
  clientId: null,
  categoryId: null,
  serviceId: null,
  from: '',
  to: '',
};

/** Read-only data used by the operational home dashboard. */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly reports = inject(ReportsService);
  private readonly treatments = inject(TreatmentsService);

  recentTreatments(limit = 5): Observable<Treatment[]> {
    return this.treatments.search(EMPTY_TREATMENT_FILTERS, 0, limit).pipe(map((page) => page.content));
  }

  topServices(period: Period, limit = 4): Observable<TopService[]> {
    return this.reports.topServices(period, limit);
  }
}
