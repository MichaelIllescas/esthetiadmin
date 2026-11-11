import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ReportsService } from '../../reports/services/reports.service';
import { TreatmentsService } from '../../treatments/services/treatments.service';
import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  const reports = { topServices: vi.fn() };
  const treatments = { search: vi.fn() };
  let service: DashboardService;

  beforeEach(() => {
    reports.topServices.mockReset();
    treatments.search.mockReset();
    TestBed.configureTestingModule({
      providers: [
        DashboardService,
        { provide: ReportsService, useValue: reports },
        { provide: TreatmentsService, useValue: treatments },
      ],
    });
    service = TestBed.inject(DashboardService);
  });

  it('loads the newest treatments without filters and respects the requested limit', () => {
    treatments.search.mockReturnValue(of({ content: [{ id: 7 }], page: 0, size: 3, totalElements: 1, totalPages: 1 }));

    service.recentTreatments(3).subscribe((rows) => expect(rows).toEqual([{ id: 7 }]));

    expect(treatments.search).toHaveBeenCalledWith(
      { clientId: null, categoryId: null, serviceId: null, from: '', to: '' },
      0,
      3,
    );
  });

  it('loads only the requested number of top services for the month', () => {
    const period = { from: '2026-09-01', to: '2026-09-30' };
    reports.topServices.mockReturnValue(of([]));

    service.topServices(period, 4).subscribe();

    expect(reports.topServices).toHaveBeenCalledWith(period, 4);
  });
});
