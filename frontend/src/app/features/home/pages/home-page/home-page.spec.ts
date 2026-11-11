import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ClientsService } from '../../../clients/services/clients.service';
import { ReportsService } from '../../../reports/services/reports.service';
import { DashboardService } from '../../services/dashboard.service';
import { HomePage } from './home-page';

describe('HomePage', () => {
  const summary = {
    from: '2026-09-01', to: '2026-09-30', income: 18000, expenses: 5000, balance: 13000,
    treatmentsCount: 4, expensesCount: 2,
  };
  const dashboard = {
    recentTreatments: vi.fn(() => of([
      { id: 1, clientId: 9, clientName: 'Pérez, Ana', serviceId: 2, serviceName: 'Limpieza facial', categoryId: 3,
        categoryName: 'Faciales', performedOn: '2026-09-25', amount: 4500, notes: null },
    ])),
    topServices: vi.fn(() => of([
      { serviceId: 2, serviceName: 'Limpieza facial', categoryId: 3, categoryName: 'Faciales', treatments: 4, income: 18000 },
    ])),
  };
  const reports = { summary: vi.fn(() => of(summary)) };
  const clients = { birthdays: vi.fn(() => of([])) };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [
        provideRouter([]),
        { provide: DashboardService, useValue: dashboard },
        { provide: ReportsService, useValue: reports },
        { provide: ClientsService, useValue: clients },
      ],
    }).compileComponents();
  });

  it('shows the operational dashboard with the current-month summary', async () => {
    const fixture = TestBed.createComponent(HomePage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = (fixture.nativeElement.textContent as string).replace(/\s+/g, ' ');
    expect(fixture.nativeElement.querySelector('h1').textContent).toContain('Resumen del mes');
    expect(text).toContain('Ingresos');
    expect(text).toContain('$ 18.000');
    expect(text).toContain('Pérez, Ana');
    expect(text).toContain('Limpieza facial');
  });

  it('offers direct access to the everyday modules', () => {
    const fixture = TestBed.createComponent(HomePage);
    fixture.detectChanges();

    const links = Array.from(fixture.nativeElement.querySelectorAll('.quick-action')) as HTMLAnchorElement[];
    expect(links.map((link) => link.getAttribute('href'))).toEqual(['/tratamientos', '/gastos', '/clientes']);
  });
});
