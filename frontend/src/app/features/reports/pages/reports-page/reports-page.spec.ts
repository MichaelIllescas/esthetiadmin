import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ChartFactory } from '../../../../shared/components/chart/chart-factory';
import { ReportsService } from '../../services/reports.service';
import { resolvePreset } from '../../utils/periods';
import { ReportsPage } from './reports-page';

describe('ReportsPage', () => {
  const reports = {
    summary: vi.fn(() => of({ from: '', to: '', income: 0, expenses: 0, balance: 0, treatmentsCount: 0, expensesCount: 0 })),
    monthly: vi.fn(() => of([])),
    topServices: vi.fn(() => of([])),
    incomeByService: vi.fn(() => of({ totalIncome: 0, categories: [] })),
    topClients: vi.fn(() => of([])),
  };
  const factory = { create: vi.fn(() => ({ update: vi.fn(), destroy: vi.fn() })) };

  beforeEach(async () => {
    Object.values(reports).forEach((fn) => fn.mockClear());
    await TestBed.configureTestingModule({
      imports: [ReportsPage],
      providers: [
        provideRouter([]),
        { provide: ReportsService, useValue: reports },
        { provide: ChartFactory, useValue: factory },
      ],
    }).compileComponents();
  });

  it('shows the title, the period picker and the five reports', async () => {
    const fixture = TestBed.createComponent(ReportsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h1')?.textContent).toContain('Reportes');
    expect(element.querySelector('app-period-picker')).not.toBeNull();
    const headings = Array.from(element.querySelectorAll('h2')).map((h) => h.textContent?.trim());
    expect(headings).toEqual([
      'Evolución de los últimos 12 meses',
      'Servicios más realizados',
      'Ingresos por categoría y servicio',
      'Ranking de clientes',
    ]);
    expect(element.querySelector('app-summary-cards')).not.toBeNull();
  });

  it('asks every report for this month, the period the picker starts with', async () => {
    const fixture = TestBed.createComponent(ReportsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();

    const thisMonth = resolvePreset('this-month', new Date());
    expect(reports.summary).toHaveBeenCalledWith(thisMonth);
    expect(reports.monthly).toHaveBeenCalledWith(resolvePreset('last-12-months', new Date()));
    expect(reports.topServices).toHaveBeenCalledWith(thisMonth, 10);
    expect(reports.incomeByService).toHaveBeenCalledWith(thisMonth);
    expect(reports.topClients).toHaveBeenCalledWith(thisMonth, 'amount', 10);
  });

  it('asks every report again when another period is chosen', async () => {
    const fixture = TestBed.createComponent(ReportsPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();

    const chip = (Array.from(fixture.nativeElement.querySelectorAll('.chip')) as HTMLButtonElement[]).find(
      (b) => b.textContent?.trim() === 'Este año',
    ) as HTMLButtonElement;
    chip.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();

    const thisYear = resolvePreset('this-year', new Date());
    expect(reports.summary).toHaveBeenLastCalledWith(thisYear);
    expect(reports.monthly).toHaveBeenLastCalledWith(resolvePreset('last-12-months', new Date()));
    expect(reports.monthly).toHaveBeenCalledTimes(1);
    expect(reports.topServices).toHaveBeenLastCalledWith(thisYear, 10);
    expect(reports.incomeByService).toHaveBeenLastCalledWith(thisYear);
    expect(reports.topClients).toHaveBeenLastCalledWith(thisYear, 'amount', 10);
  });
});
