import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { Period } from '../../models/report.models';
import { resolvePreset } from '../../utils/periods';
import { PeriodPicker } from './period-picker';

@Component({
  imports: [PeriodPicker],
  template: `<app-period-picker (changed)="log.push($event)" />`,
})
class Host {
  readonly log: Period[] = [];
}

describe('PeriodPicker', () => {
  let fixture: ComponentFixture<Host>;

  const root = () => fixture.nativeElement as HTMLElement;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ');
  const chip = (label: string) =>
    (Array.from(root().querySelectorAll('.chip')) as HTMLButtonElement[]).find((b) => b.textContent?.trim() === label) as HTMLButtonElement;
  const setDate = (id: string, value: string) => {
    const input = root().querySelector(`#${id}`) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Host] }).compileComponents();
    fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
  });

  it('announces this month as soon as it is shown', () => {
    expect(fixture.componentInstance.log).toEqual([resolvePreset('this-month', new Date())]);
    expect(chip('Este mes').getAttribute('aria-pressed')).toBe('true');
    expect(chip('Este año').getAttribute('aria-pressed')).toBe('false');
  });

  it('says which days it is showing', () => {
    expect(text()).toContain('Mostrando del');
    expect(text()).toContain('de 20');
  });

  it('announces the period of a quick choice and marks it as the chosen one', () => {
    chip('Este año').click();
    fixture.detectChanges();

    const expected = resolvePreset('this-year', new Date());
    expect(fixture.componentInstance.log.at(-1)).toEqual(expected);
    expect(chip('Este año').getAttribute('aria-pressed')).toBe('true');
    expect(chip('Este mes').getAttribute('aria-pressed')).toBe('false');
  });

  it('offers the last month and the last twelve months', () => {
    chip('Mes pasado').click();
    expect(fixture.componentInstance.log.at(-1)).toEqual(resolvePreset('last-month', new Date()));

    chip('Últimos 12 meses').click();
    expect(fixture.componentInstance.log.at(-1)).toEqual(resolvePreset('last-12-months', new Date()));
  });

  it('opens two dates that start from the period being shown, without changing the reports yet', () => {
    const before = fixture.componentInstance.log.length;

    chip('Personalizado').click();
    fixture.detectChanges();

    const current = fixture.componentInstance.log[0];
    expect((root().querySelector('#period-from') as HTMLInputElement).value).toBe(current.from);
    expect((root().querySelector('#period-to') as HTMLInputElement).value).toBe(current.to);
    expect(fixture.componentInstance.log).toHaveLength(before);
  });

  it('announces a valid custom range', () => {
    chip('Personalizado').click();
    fixture.detectChanges();

    setDate('period-from', '2026-03-01');
    setDate('period-to', '2026-05-31');

    expect(fixture.componentInstance.log.at(-1)).toEqual({ from: '2026-03-01', to: '2026-05-31' });
    expect(root().querySelector('[role="alert"]')).toBeNull();
  });

  it('does not announce an inverted range and says why', () => {
    chip('Personalizado').click();
    fixture.detectChanges();
    setDate('period-from', '2026-06-30'); // still valid against the current end: it is announced
    const before = fixture.componentInstance.log.length;

    setDate('period-to', '2026-06-01');

    expect(fixture.componentInstance.log).toHaveLength(before);
    expect(root().querySelector('[role="alert"]')?.textContent).toContain('no puede ser posterior');
    expect(root().querySelector('#period-to')?.getAttribute('aria-invalid')).toBe('true');
  });

  it('does not announce a range wider than five years', () => {
    chip('Personalizado').click();
    fixture.detectChanges();
    const before = fixture.componentInstance.log.length;

    setDate('period-from', '2015-01-01');
    setDate('period-to', '2026-01-01');

    expect(fixture.componentInstance.log).toHaveLength(before);
    expect(root().querySelector('[role="alert"]')?.textContent).toContain('5 años');
  });

  it('goes back to a quick choice and hides the dates', () => {
    chip('Personalizado').click();
    fixture.detectChanges();

    chip('Mes pasado').click();
    fixture.detectChanges();

    expect(root().querySelector('#period-from')).toBeNull();
  });
});
