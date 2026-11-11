import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { todayIso } from '../../../../shared/utils/format';
import { AvailabilityBlock } from '../../models/availability.models';
import { AvailabilityService } from '../../services/availability.service';
import { BlockFormDialog } from './block-form-dialog';

@Component({
  imports: [BlockFormDialog],
  template: `<app-block-form-dialog (saved)="log.push($event)" (cancelled)="log.push('cancelled')" />`,
})
class Host {
  readonly log: (AvailabilityBlock | string)[] = [];
}

const created: AvailabilityBlock = {
  id: 3,
  startDate: '2099-10-12',
  endDate: '2099-10-12',
  startTime: null,
  endTime: null,
  reason: 'Feriado',
  affectedAppointments: [],
};

describe('BlockFormDialog', () => {
  const availability = { createBlock: vi.fn() };
  let fixture: ComponentFixture<Host>;

  const root = () => fixture.nativeElement as HTMLElement;
  const field = <T extends HTMLElement>(id: string) => root().querySelector(`#${id}`) as T;
  const fill = (id: string, value: string) => {
    const input = field<HTMLInputElement>(id);
    input.value = value;
    input.dispatchEvent(new Event(input.tagName === 'SELECT' ? 'change' : 'input'));
    fixture.detectChanges();
  };
  const submit = () => {
    root().querySelector('form')?.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };
  const wholeDaySwitch = () => root().querySelector<HTMLInputElement>('input[role="switch"]');

  beforeEach(async () => {
    availability.createBlock.mockReset();
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [{ provide: AvailabilityService, useValue: availability }],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
  });

  it('blocks a whole single day by default, starting today', () => {
    availability.createBlock.mockReturnValue(of(created));
    expect(field<HTMLInputElement>('block-start-date').value).toBe(todayIso());
    expect(field<HTMLInputElement>('block-end-date').value).toBe(todayIso());
    expect(field('block-start')).toBeNull();

    fill('block-start-date', '2099-10-12');
    fill('block-reason', '  Feriado ');
    submit();

    expect(field<HTMLInputElement>('block-end-date').value).toBe('2099-10-12');
    expect(availability.createBlock).toHaveBeenCalledWith({
      startDate: '2099-10-12',
      endDate: '2099-10-12',
      startTime: null,
      endTime: null,
      reason: 'Feriado',
    });
    expect(fixture.componentInstance.log).toEqual([created]);
  });

  it('blocks several whole days as one block, without offering hours', () => {
    availability.createBlock.mockReturnValue(of(created));

    fill('block-start-date', '2099-01-04');
    fill('block-end-date', '2099-01-15');

    expect(wholeDaySwitch()).toBeNull();
    expect(root().textContent).toContain('Un bloqueo de varios días toma los días completos.');

    submit();

    expect(availability.createBlock).toHaveBeenCalledWith(
      expect.objectContaining({ startDate: '2099-01-04', endDate: '2099-01-15', startTime: null, endTime: null }),
    );
  });

  it('blocks only a range of a single day when "Todo el día" is turned off', () => {
    availability.createBlock.mockReturnValue(of(created));

    wholeDaySwitch()?.click();
    fixture.detectChanges();
    fill('block-start', '15:00');
    fill('block-end', '19:00');
    submit();

    expect(availability.createBlock).toHaveBeenCalledWith(
      expect.objectContaining({ startTime: '15:00', endTime: '19:00', reason: null }),
    );
  });

  it('does not send a range that ends before it starts', () => {
    wholeDaySwitch()?.click();
    fixture.detectChanges();
    fill('block-start', '15:00');
    fill('block-end', '10:00');
    submit();

    expect(root().textContent).toContain('La hora de inicio tiene que ser anterior a la de fin.');
    expect(availability.createBlock).not.toHaveBeenCalled();
  });

  it('does not send an end date before the start', () => {
    fill('block-start-date', '2099-01-10');
    fill('block-end-date', '2099-01-05');
    submit();

    expect(root().textContent).toContain('La fecha "Hasta" no puede ser anterior a "Desde".');
    expect(availability.createBlock).not.toHaveBeenCalled();
  });

  it('does not send a past date', () => {
    fill('block-start-date', '2000-01-01');
    submit();

    expect(root().textContent).toContain('Elegí una fecha de hoy en adelante.');
    expect(availability.createBlock).not.toHaveBeenCalled();
  });

  it('shows the reason when the API refuses the block', () => {
    availability.createBlock.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: { message: 'No se puede bloquear una fecha pasada' } })),
    );

    submit();

    expect(root().querySelector('[role="alert"]')?.textContent).toContain('No se puede bloquear una fecha pasada');
    expect(fixture.componentInstance.log).toEqual([]);
  });
});
