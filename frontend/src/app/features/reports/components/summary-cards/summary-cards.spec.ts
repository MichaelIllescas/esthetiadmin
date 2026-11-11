import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Period, Summary } from '../../models/report.models';
import { ReportsService } from '../../services/reports.service';
import { SummaryCards } from './summary-cards';

const summary = (income: number, expenses: number, treatments = 4, expensesCount = 2): Summary => ({
  from: '2026-09-01', to: '2026-09-30', income, expenses, balance: income - expenses,
  treatmentsCount: treatments, expensesCount,
});

@Component({
  imports: [SummaryCards],
  template: `<app-summary-cards [period]="period()" />`,
})
class Host {
  readonly period = signal<Period>({ from: '2026-09-01', to: '2026-09-30' });
}

describe('SummaryCards', () => {
  const reports = { summary: vi.fn() };
  let fixture: ComponentFixture<Host>;

  const text = () => (fixture.nativeElement.textContent as string).replace(/\s+/g, ' ').replace(/\s/g, ' ');
  const card = (title: string) =>
    (Array.from(fixture.nativeElement.querySelectorAll('.card')) as HTMLElement[])
      .find((element) => element.querySelector('h3')?.textContent?.trim() === title) as HTMLElement;
  const part = (title: string, selector: string) => (card(title).querySelector(selector)?.textContent ?? '').replace(/\s/g, ' ').trim();
  const settle = async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  };
  const start = async () => {
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [{ provide: ReportsService, useValue: reports }],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    await settle();
  };

  beforeEach(() => reports.summary.mockReset());

  it('shows income, expenses and the result with how many treatments and expenses they come from', async () => {
    reports.summary.mockReturnValue(of(summary(8800, 6100, 8, 3)));
    await start();

    expect(reports.summary).toHaveBeenCalledWith({ from: '2026-09-01', to: '2026-09-30' });
    expect(part('Ingresos', '.amount')).toBe('$ 8.800');
    expect(part('Ingresos', '.caption')).toBe('8 tratamientos');
    expect(part('Gastos', '.amount')).toBe('$ 6.100');
    expect(part('Gastos', '.caption')).toBe('3 gastos');
    expect(part('Resultado', '.amount')).toBe('$ 2.700');
  });

  it('uses the singular for a single treatment or expense', async () => {
    reports.summary.mockReturnValue(of(summary(1000, 500, 1, 1)));
    await start();

    expect(text()).toContain('1 tratamiento');
    expect(text()).not.toContain('1 tratamientos');
    expect(text()).toContain('1 gasto');
  });

  it('marks a positive result and says it in words, not only with a color', async () => {
    reports.summary.mockReturnValue(of(summary(5000, 1000)));
    await start();

    expect(fixture.nativeElement.querySelector('.card-positive')).not.toBeNull();
    expect(text()).toContain('Ingresaron más de lo que se gastó');
  });

  it('marks a negative result, with its minus sign, and says it in words', async () => {
    reports.summary.mockReturnValue(of(summary(1000, 4000)));
    await start();

    expect(fixture.nativeElement.querySelector('.card-negative')).not.toBeNull();
    expect(text()).toContain('Se gastó más de lo que ingresó');
    expect(part('Resultado', '.amount')).toBe('-$ 3.000');
  });

  it('shows a neutral result when income and expenses are equal or there is nothing', async () => {
    reports.summary.mockReturnValue(of(summary(0, 0, 0, 0)));
    await start();

    expect(fixture.nativeElement.querySelector('.card-positive')).toBeNull();
    expect(fixture.nativeElement.querySelector('.card-negative')).toBeNull();
    expect(text()).toContain('Ingresos y gastos iguales');
    expect(text()).toContain('0 tratamientos');
  });

  it('asks again when the period changes', async () => {
    reports.summary.mockReturnValue(of(summary(100, 50)));
    await start();

    fixture.componentInstance.period.set({ from: '2026-10-01', to: '2026-10-31' });
    await settle();

    expect(reports.summary).toHaveBeenLastCalledWith({ from: '2026-10-01', to: '2026-10-31' });
  });

  it('shows the reason with a retry when it cannot load', async () => {
    reports.summary.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 400, error: { message: 'El período no puede superar los 5 años' } })),
    );
    reports.summary.mockReturnValue(of(summary(100, 50)));
    await start();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('no puede superar los 5 años');

    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
    await settle();

    expect(text()).toContain('Ingresos');
  });
});
