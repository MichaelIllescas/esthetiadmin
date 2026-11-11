import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ChartFactory } from '../../../../shared/components/chart/chart-factory';
import { IncomeBreakdown, Period } from '../../models/report.models';
import { ReportsService } from '../../services/reports.service';
import { IncomeBreakdownView } from './income-breakdown';

const breakdown: IncomeBreakdown = {
  totalIncome: 8800,
  categories: [
    {
      categoryId: 1, categoryName: 'Depilación', income: 4800, percentage: 54.55, treatments: 7,
      services: [
        { serviceId: 10, serviceName: 'Bozo', income: 2800, percentage: 31.82, treatments: 6 },
        { serviceId: 11, serviceName: 'Axilas', income: 2000, percentage: 22.73, treatments: 1 },
      ],
    },
    {
      categoryId: 2, categoryName: 'Faciales', income: 4000, percentage: 45.45, treatments: 1,
      services: [{ serviceId: 20, serviceName: 'Limpieza', income: 4000, percentage: 45.45, treatments: 1 }],
    },
  ],
};

@Component({
  imports: [IncomeBreakdownView],
  template: `<app-income-breakdown [period]="period()" />`,
})
class Host {
  readonly period = signal<Period>({ from: '2026-09-01', to: '2026-09-30' });
}

describe('IncomeBreakdownView', () => {
  const reports = { incomeByService: vi.fn() };
  const factory = { create: vi.fn(() => ({ update: vi.fn(), destroy: vi.fn() })) };
  let fixture: ComponentFixture<Host>;

  const root = () => fixture.nativeElement as HTMLElement;
  const clean = (value: string | null | undefined) => (value ?? '').replace(/\s+/g, ' ').replace(/\s/g, ' ').trim();
  const settle = async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();
  };
  const start = async () => {
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [
        { provide: ReportsService, useValue: reports },
        { provide: ChartFactory, useValue: factory },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    await settle();
  };

  beforeEach(() => {
    reports.incomeByService.mockReset();
    factory.create.mockClear();
    reports.incomeByService.mockReturnValue(of(breakdown));
  });

  it('shows the total income and every category with its amount and its share', async () => {
    await start();

    expect(clean(root().querySelector('.total')?.textContent)).toBe('Ingresos del período: $ 8.800');
    const names = Array.from(root().querySelectorAll('summary .name')).map((n) => clean(n.textContent));
    const figures = Array.from(root().querySelectorAll('summary .figures')).map((n) => clean(n.textContent));
    expect(names).toEqual(['Depilación', 'Faciales']);
    expect(figures).toEqual(['$ 4.800 · 54,55 %', '$ 4.000 · 45,45 %']);
  });

  it('opens each category into its services, with their amount and share of the total', async () => {
    await start();

    const first = root().querySelector('details') as HTMLDetailsElement;
    const rows = Array.from(first.querySelectorAll('tbody tr')).map((tr) => clean(tr.textContent));
    expect(rows).toHaveLength(2);
    expect(rows[0]).toContain('Bozo');
    expect(rows[0]).toContain('$ 2.800');
    expect(rows[0]).toContain('31,82 %');
    expect(rows[1]).toContain('Axilas');
  });

  it('draws the doughnut and describes it, naming the category that contributed the most', async () => {
    await start();

    expect(factory.create).toHaveBeenCalledTimes(1);
    const label = (root().querySelector('canvas') as HTMLCanvasElement).getAttribute('aria-label') ?? '';
    expect(label).toContain('Depilación');
    expect(label.replace(/\s/g, ' ')).toContain('54,55 %');
  });

  it('gives each category the color of its slice', async () => {
    await start();

    const swatches = Array.from(root().querySelectorAll('.swatch')) as HTMLElement[];
    expect(swatches).toHaveLength(2);
    expect(swatches[0].style.background).not.toBe('');
    expect(swatches[0].style.background).not.toBe(swatches[1].style.background);
  });

  it('lists the treatments without drawing a chart when they were all free', async () => {
    reports.incomeByService.mockReturnValue(of({ totalIncome: 0, categories: [{ ...breakdown.categories[0], income: 0, percentage: 0 }] }));
    await start();

    expect(root().querySelector('canvas')).toBeNull();
    expect(root().querySelectorAll('summary')).toHaveLength(1);
  });

  it('says so when there were no treatments', async () => {
    reports.incomeByService.mockReturnValue(of({ totalIncome: 0, categories: [] }));
    await start();

    expect(clean(root().textContent)).toContain('No hay tratamientos registrados en este período.');
  });

  it('shows the reason with a retry when it cannot load', async () => {
    reports.incomeByService.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })));
    await start();
    expect(root().querySelector('[role="alert"]')?.textContent).toContain('No pudimos cargar');

    (root().querySelector('button') as HTMLButtonElement).click();
    await settle();

    expect(root().querySelectorAll('summary')).toHaveLength(2);
  });

  it('asks again when the period changes', async () => {
    await start();

    fixture.componentInstance.period.set({ from: '2025-01-01', to: '2025-12-31' });
    await settle();

    expect(reports.incomeByService).toHaveBeenLastCalledWith({ from: '2025-01-01', to: '2025-12-31' });
  });
});
