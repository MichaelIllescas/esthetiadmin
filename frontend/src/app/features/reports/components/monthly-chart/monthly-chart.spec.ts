import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ChartFactory } from '../../../../shared/components/chart/chart-factory';
import { MonthlyRow, Period } from '../../models/report.models';
import { ReportsService } from '../../services/reports.service';
import { MonthlyChart } from './monthly-chart';

const rows: MonthlyRow[] = [
  { month: '2026-07', income: 4500, expenses: 800, balance: 3700 },
  { month: '2026-08', income: 0, expenses: 300, balance: -300 },
  { month: '2026-09', income: 4000, expenses: 0, balance: 4000 },
];

@Component({
  imports: [MonthlyChart],
  template: `<app-monthly-chart [period]="period()" />`,
})
class Host {
  readonly period = signal<Period>({ from: '2026-07-01', to: '2026-09-30' });
}

describe('MonthlyChart', () => {
  const reports = { monthly: vi.fn() };
  const handle = { update: vi.fn(), destroy: vi.fn() };
  const factory = { create: vi.fn(() => handle) };
  let fixture: ComponentFixture<Host>;

  const root = () => fixture.nativeElement as HTMLElement;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ').replace(/\s/g, ' ');
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
    reports.monthly.mockReset();
    factory.create.mockClear();
    reports.monthly.mockReturnValue(of(rows));
  });

  it('draws the chart as lines, with a description for screen readers', async () => {
    await start();

    expect(reports.monthly).toHaveBeenCalledWith({ from: '2026-07-01', to: '2026-09-30' });
    expect(factory.create).toHaveBeenCalledTimes(1);
    const canvas = root().querySelector('canvas') as HTMLCanvasElement;
    expect(canvas.getAttribute('aria-label')).toContain('ingresos, los gastos y el resultado de cada mes');
    expect(canvas.getAttribute('aria-label')).toContain('tabla de abajo');
  });

  it('offers the same data as a table, with the months and the amounts', async () => {
    await start();

    const rowsText = Array.from(root().querySelectorAll('tbody tr')).map((tr) => (tr.textContent ?? '').replace(/\s+/g, ' ').trim());
    expect(rowsText).toHaveLength(3);
    expect(rowsText[0]).toContain('$ 4.500');
    expect(rowsText[1]).toMatch(/-\$ 300/);
    expect(root().querySelector('details summary')?.textContent).toContain('Ver los datos en una tabla');
  });

  it('marks a negative result in the table', async () => {
    await start();

    expect(root().querySelectorAll('td.negative')).toHaveLength(1);
  });

  it('says so instead of drawing an empty chart when there was no movement', async () => {
    reports.monthly.mockReturnValue(of([{ month: '2026-09', income: 0, expenses: 0, balance: 0 }]));
    await start();

    expect(text()).toContain('No hubo ingresos ni gastos en los últimos 12 meses.');
    expect(root().querySelector('canvas')).toBeNull();
  });

  it('shows the reason with a retry when it cannot load', async () => {
    reports.monthly.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 400, error: { message: 'El período no puede superar los 5 años' } })),
    );
    await start();
    expect(root().querySelector('[role="alert"]')?.textContent).toContain('no puede superar los 5 años');

    (root().querySelector('button') as HTMLButtonElement).click();
    await settle();

    expect(root().querySelector('canvas')).not.toBeNull();
  });

  it('asks again when the period changes', async () => {
    await start();

    fixture.componentInstance.period.set({ from: '2025-01-01', to: '2025-12-31' });
    await settle();

    expect(reports.monthly).toHaveBeenLastCalledWith({ from: '2025-01-01', to: '2025-12-31' });
  });
});
