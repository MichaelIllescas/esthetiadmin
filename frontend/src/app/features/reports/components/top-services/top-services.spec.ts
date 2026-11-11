import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ChartFactory } from '../../../../shared/components/chart/chart-factory';
import { Period, TopService } from '../../models/report.models';
import { ReportsService } from '../../services/reports.service';
import { TopServices } from './top-services';

const services: TopService[] = [
  { serviceId: 10, serviceName: 'Bozo', categoryId: 1, categoryName: 'Depilación', treatments: 6, income: 2800 },
  { serviceId: 20, serviceName: 'Limpieza', categoryId: 2, categoryName: 'Faciales', treatments: 1, income: 4000 },
];

@Component({
  imports: [TopServices],
  template: `<app-top-services [period]="period()" />`,
})
class Host {
  readonly period = signal<Period>({ from: '2026-09-01', to: '2026-09-30' });
}

describe('TopServices', () => {
  const reports = { topServices: vi.fn() };
  const factory = { create: vi.fn(() => ({ update: vi.fn(), destroy: vi.fn() })) };
  let fixture: ComponentFixture<Host>;

  const root = () => fixture.nativeElement as HTMLElement;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ');
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
    reports.topServices.mockReset();
    factory.create.mockClear();
    reports.topServices.mockReturnValue(of(services));
  });

  it('asks for the ten most performed services of the period', async () => {
    await start();

    expect(reports.topServices).toHaveBeenCalledWith({ from: '2026-09-01', to: '2026-09-30' }, 10);
  });

  it('lists them in order, with their category, how many were done and what they earned', async () => {
    await start();

    const rows = Array.from(root().querySelectorAll('tbody tr')).map((tr) => (tr.textContent ?? '').replace(/\s+/g, ' ').replace(/\s/g, ' ').trim());
    expect(rows).toHaveLength(2);
    expect(rows[0]).toContain('1');
    expect(rows[0]).toContain('Bozo');
    expect(rows[0]).toContain('Depilación');
    expect(rows[0]).toContain('6');
    expect(rows[0]).toContain('$ 2.800');
    expect(rows[1]).toContain('Limpieza');
  });

  it('draws the chart and describes it, naming the most performed service', async () => {
    await start();

    expect(factory.create).toHaveBeenCalledTimes(1);
    const label = (root().querySelector('canvas') as HTMLCanvasElement).getAttribute('aria-label') ?? '';
    expect(label).toContain('El más realizado es Bozo, con 6');
  });

  it('says so when there were no treatments', async () => {
    reports.topServices.mockReturnValue(of([]));
    await start();

    expect(text()).toContain('No hay tratamientos registrados en este período.');
    expect(root().querySelector('canvas')).toBeNull();
  });

  it('shows the reason with a retry when it cannot load', async () => {
    reports.topServices.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })));
    await start();
    expect(root().querySelector('[role="alert"]')?.textContent).toContain('No pudimos cargar');

    (root().querySelector('button') as HTMLButtonElement).click();
    await settle();

    expect(text()).toContain('Bozo');
  });

  it('asks again when the period changes', async () => {
    await start();

    fixture.componentInstance.period.set({ from: '2026-01-01', to: '2026-12-31' });
    await settle();

    expect(reports.topServices).toHaveBeenLastCalledWith({ from: '2026-01-01', to: '2026-12-31' }, 10);
  });
});
