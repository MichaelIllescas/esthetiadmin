import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { todayIso } from '../../../../shared/utils/format';
import { addDays } from '../../../appointments/utils/agenda-dates';
import { ChosenService } from '../../models/booking.models';
import { BookingService } from '../../services/booking.service';
import { monthOf } from '../../utils/month-calendar';
import { PickedSlot, SlotPicker } from './slot-picker';

const service: ChosenService = { id: 10, name: 'Bozo', description: null, categoryName: 'Depilación', durationMinutes: 30 };
const TODAY = todayIso();
/** The API is mocked, so today always works and stays inside the month shown first. */
const DAY = TODAY;

@Component({
  imports: [SlotPicker],
  template: `<app-slot-picker [service]="service" [initialDate]="initialDate()" (picked)="log.push($event)" />`,
})
class Host {
  readonly service = service;
  readonly initialDate = signal<string | null>(null);
  readonly log: PickedSlot[] = [];
}

describe('SlotPicker', () => {
  const booking = { availableDays: vi.fn(), freeTimes: vi.fn() };
  let fixture: ComponentFixture<Host>;

  const root = () => fixture.nativeElement as HTMLElement;
  const dayButton = (date: string) =>
    Array.from(root().querySelectorAll<HTMLButtonElement>('button.day')).find(
      (button) => button.textContent?.trim() === String(Number(date.slice(8))),
    ) as HTMLButtonElement;
  const start = async (initialDate: string | null = null) => {
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [{ provide: BookingService, useValue: booking }],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    fixture.componentInstance.initialDate.set(initialDate);
    fixture.detectChanges();
  };

  beforeEach(() => {
    booking.availableDays.mockReset();
    booking.freeTimes.mockReset();
    booking.availableDays.mockImplementation((_: number, month: string) =>
      of({ month, days: month === monthOf(DAY) ? [DAY] : [] }),
    );
    booking.freeTimes.mockReturnValue(of({ date: DAY, durationMinutes: 30, times: ['09:00', '09:30'] }));
  });

  it('opens on the current month and only lets days with room be chosen', async () => {
    await start();

    expect(booking.availableDays).toHaveBeenCalledWith(10, monthOf(TODAY));
    expect(dayButton(DAY).disabled).toBe(false);
    expect(root().querySelector<HTMLButtonElement>('[aria-label="Mes anterior"]')?.disabled).toBe(true);
  });

  it('shows the free times of the chosen day and emits the chosen time', async () => {
    await start();

    dayButton(DAY).click();
    fixture.detectChanges();

    expect(booking.freeTimes).toHaveBeenCalledWith(10, DAY);
    const times = Array.from(root().querySelectorAll<HTMLButtonElement>('button.time'));
    expect(times.map((button) => button.textContent?.trim())).toEqual(['09:00', '09:30']);

    times[1].click();

    expect(fixture.componentInstance.log).toEqual([{ date: DAY, time: '09:30' }]);
  });

  it('can reopen on a day already chosen', async () => {
    await start(DAY);

    expect(booking.freeTimes).toHaveBeenCalledWith(10, DAY);
    expect(root().querySelectorAll('button.time')).toHaveLength(2);
  });

  it('moves to the next month but not beyond 60 days ahead', async () => {
    await start();
    const next = root().querySelector<HTMLButtonElement>('[aria-label="Mes siguiente"]') as HTMLButtonElement;

    for (let click = 0; click < 4 && !next.disabled; click++) {
      next.click();
      fixture.detectChanges();
    }

    expect(next.disabled).toBe(true);
    expect(booking.availableDays).toHaveBeenLastCalledWith(10, monthOf(addDays(TODAY, 60)));
  });

  it('says when the chosen day has no times left', async () => {
    booking.freeTimes.mockReturnValue(of({ date: DAY, durationMinutes: 30, times: [] }));
    await start(DAY);

    expect(root().textContent).toContain('Ya no quedan horarios ese día.');
  });

  it('offers to retry when the days cannot be loaded', async () => {
    booking.availableDays.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await start();

    expect(root().querySelector('[role="alert"]')?.textContent).toContain('No pudimos cargar los días disponibles');
  });
});
