import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Period, TopClient } from '../../models/report.models';
import { ReportsService } from '../../services/reports.service';
import { TopClients } from './top-clients';

const byAmount: TopClient[] = [
  { clientId: 1, clientName: 'Diaz, Carina', treatments: 3, totalAmount: 7000 },
  { clientId: 2, clientName: 'Funes, Rosa', treatments: 5, totalAmount: 1800 },
];
const byTreatments: TopClient[] = [byAmount[1], byAmount[0]];

@Component({
  imports: [TopClients],
  template: `<app-top-clients [period]="period()" />`,
})
class Host {
  readonly period = signal<Period>({ from: '2026-09-01', to: '2026-09-30' });
}

describe('TopClients', () => {
  const reports = { topClients: vi.fn() };
  let fixture: ComponentFixture<Host>;

  const root = () => fixture.nativeElement as HTMLElement;
  const clean = (value: string | null | undefined) => (value ?? '').replace(/\s+/g, ' ').replace(/\s/g, ' ').trim();
  const names = () => Array.from(root().querySelectorAll('tbody td:nth-child(2)')).map((td) => clean(td.textContent));
  const chip = (label: string) =>
    (Array.from(root().querySelectorAll('.chip')) as HTMLButtonElement[]).find((b) => clean(b.textContent) === label) as HTMLButtonElement;
  const settle = async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();
  };
  const start = async () => {
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [provideRouter([]), { provide: ReportsService, useValue: reports }],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    await settle();
  };

  beforeEach(() => {
    reports.topClients.mockReset();
    reports.topClients.mockImplementation((_period: Period, sortBy: string) => of(sortBy === 'treatments' ? byTreatments : byAmount));
  });

  it('starts ranking by what each client paid, top ten', async () => {
    await start();

    expect(reports.topClients).toHaveBeenCalledWith({ from: '2026-09-01', to: '2026-09-30' }, 'amount', 10);
    expect(names()).toEqual(['Diaz, Carina', 'Funes, Rosa']);
    expect(chip('Importe cobrado').getAttribute('aria-pressed')).toBe('true');
    expect(chip('Cantidad de tratamientos').getAttribute('aria-pressed')).toBe('false');
  });

  it('shows the treatments and the amount of each client', async () => {
    await start();

    const first = Array.from(root().querySelectorAll('tbody tr')).map((tr) => clean(tr.textContent))[0];
    expect(first).toContain('Diaz, Carina');
    expect(first).toContain('3');
    expect(first).toContain('$ 7.000');
  });

  it('can be ranked by the number of treatments instead, asking the API again', async () => {
    await start();

    chip('Cantidad de tratamientos').click();
    await settle();

    expect(reports.topClients).toHaveBeenLastCalledWith({ from: '2026-09-01', to: '2026-09-30' }, 'treatments', 10);
    expect(names()).toEqual(['Funes, Rosa', 'Diaz, Carina']);
    expect(chip('Cantidad de tratamientos').getAttribute('aria-pressed')).toBe('true');
  });

  it('tells assistive technology which column the ranking follows', async () => {
    await start();
    const header = (label: string) =>
      (Array.from(root().querySelectorAll('th')) as HTMLElement[]).find((th) => clean(th.textContent) === label) as HTMLElement;
    expect(header('Importe cobrado').getAttribute('aria-sort')).toBe('descending');
    expect(header('Tratamientos').getAttribute('aria-sort')).toBe('none');

    chip('Cantidad de tratamientos').click();
    await settle();

    expect(header('Tratamientos').getAttribute('aria-sort')).toBe('descending');
    expect(header('Importe cobrado').getAttribute('aria-sort')).toBe('none');
  });

  it('links every client to the record', async () => {
    await start();

    const links = Array.from(root().querySelectorAll('tbody a')) as HTMLAnchorElement[];
    expect(links.map((a) => a.getAttribute('href'))).toEqual(['/clientes/1', '/clientes/2']);
  });

  it('keeps the chosen order when the period changes', async () => {
    await start();
    chip('Cantidad de tratamientos').click();
    await settle();

    fixture.componentInstance.period.set({ from: '2026-01-01', to: '2026-12-31' });
    await settle();

    expect(reports.topClients).toHaveBeenLastCalledWith({ from: '2026-01-01', to: '2026-12-31' }, 'treatments', 10);
  });

  it('says so when there were no treatments', async () => {
    reports.topClients.mockReturnValue(of([]));
    await start();

    expect(clean(root().textContent)).toContain('No hay tratamientos registrados en este período.');
  });

  it('shows the reason with a retry when it cannot load', async () => {
    reports.topClients.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })));
    await start();
    expect(root().querySelector('[role="alert"]')?.textContent).toContain('No pudimos cargar');

    (root().querySelector('button') as HTMLButtonElement).click();
    await settle();

    expect(names()).toHaveLength(2);
  });
});
