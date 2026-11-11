import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { todayIso } from '../../../../shared/utils/format';
import { BookingService } from '../../services/booking.service';
import { monthOf } from '../../utils/month-calendar';
import { BookingPage } from './booking-page';

const DAY = todayIso();

describe('BookingPage', () => {
  const booking = { services: vi.fn(), availableDays: vi.fn(), freeTimes: vi.fn(), book: vi.fn(), contact: vi.fn() };
  let fixture: ComponentFixture<BookingPage>;

  const root = () => fixture.nativeElement as HTMLElement;
  const heading = () => root().querySelector('h1')?.textContent?.trim();
  const click = (element: Element | null | undefined) => {
    (element as HTMLElement).click();
    fixture.detectChanges();
  };
  const buttonWith = (text: string) =>
    Array.from(root().querySelectorAll('button')).find((button) => button.textContent?.trim() === text);
  const fill = (id: string, value: string) => {
    const input = root().querySelector(`#${id}`) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  };
  const goToContact = () => {
    click(root().querySelector('button.service'));
    click(buttonWith(String(Number(DAY.slice(8)))));
    click(buttonWith('10:00'));
  };
  const sendContact = () => {
    fill('booking-first-name', 'Carina');
    fill('booking-last-name', 'Diaz');
    fill('booking-phone', '3415551234');
    root().querySelector('app-contact-form form')?.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };

  beforeEach(async () => {
    Object.values(booking).forEach((mock) => mock.mockReset());
    booking.services.mockReturnValue(of([
      { categoryId: 1, categoryName: 'Depilación', durationMinutes: 30, services: [{ id: 10, name: 'Bozo', description: null }] },
    ]));
    booking.availableDays.mockReturnValue(of({ month: monthOf(DAY), days: [DAY] }));
    booking.freeTimes.mockReturnValue(of({ date: DAY, durationMinutes: 30, times: ['10:00'] }));
    booking.contact.mockReturnValue(of({ phone: null }));

    await TestBed.configureTestingModule({
      imports: [BookingPage],
      providers: [provideRouter([]), { provide: BookingService, useValue: booking }],
    }).compileComponents();
    fixture = TestBed.createComponent(BookingPage);
    fixture.detectChanges();
  });

  it('starts with the services and a discreet link to the panel login', () => {
    expect(heading()).toBe('Sacá tu turno');
    expect(root().querySelector('app-service-picker')).not.toBeNull();
    expect(root().querySelector('footer a')?.getAttribute('href')).toBe('/login');
    expect(root().textContent).not.toContain('$');
  });

  it('goes through service, day and time, and data to the confirmation', () => {
    booking.book.mockReturnValue(of({
      serviceName: 'Bozo', startsAt: `${DAY}T10:00:00`, endsAt: `${DAY}T10:30:00`, firstName: 'Carina', emailSent: false,
    }));

    click(root().querySelector('button.service'));
    expect(heading()).toBe('Elegí el día y la hora');
    expect(root().textContent).toContain('Bozo');

    click(buttonWith(String(Number(DAY.slice(8)))));
    click(buttonWith('10:00'));
    expect(heading()).toBe('Completá tus datos');
    expect(root().textContent).toContain('a las 10:00 hs');

    sendContact();

    expect(booking.book).toHaveBeenCalledWith({
      serviceId: 10, startsAt: `${DAY}T10:00:00`, firstName: 'Carina', lastName: 'Diaz', phone: '3415551234',
      email: null,
    });
    expect(heading()).toBe('¡Listo, Carina!');
    expect(root().textContent).toContain('de 10:00 a 10:30 hs');
    expect(root().querySelector('.steps')).toBeNull();

    click(buttonWith('Sacar otro turno'));
    expect(heading()).toBe('Sacá tu turno');
  });

  it('sends the optional email and shows how to reach the center on WhatsApp', () => {
    booking.contact.mockReturnValue(of({ phone: '3415559876' }));
    booking.book.mockReturnValue(of({
      serviceName: 'Bozo', startsAt: `${DAY}T10:00:00`, endsAt: `${DAY}T10:30:00`, firstName: 'Carina', emailSent: true,
    }));
    fixture = TestBed.createComponent(BookingPage);
    fixture.detectChanges();

    goToContact();
    fill('booking-email', ' carina@mail.com ');
    sendContact();

    expect(booking.book).toHaveBeenCalledWith(expect.objectContaining({ email: 'carina@mail.com' }));
    expect(root().textContent).toContain('Te enviamos la confirmación por email');
    expect(root().textContent).toContain('3415559876');
    expect(root().querySelector('a.whatsapp')?.getAttribute('href')).toBe('https://wa.me/5493415559876');
  });

  it('sends her back to the times of that day when hers was taken meanwhile', () => {
    booking.book.mockReturnValue(throwError(() =>
      new HttpErrorResponse({ status: 409, error: { message: 'Ese horario acaba de ocuparse, elegí otro' } })));
    goToContact();

    sendContact();

    expect(heading()).toBe('Elegí el día y la hora');
    expect(root().querySelector('[role="alert"]')?.textContent).toContain('acaba de ocuparse');
    expect(booking.freeTimes).toHaveBeenLastCalledWith(10, DAY);
  });

  it('stays on the data step for other refusals, like the per-phone limit', () => {
    booking.book.mockReturnValue(throwError(() =>
      new HttpErrorResponse({ status: 409, error: { message: 'Ya tenés 2 turnos reservados. Para otro, comunicate con el centro' } })));
    goToContact();

    sendContact();

    expect(heading()).toBe('Completá tus datos');
    expect(root().querySelector('[role="alert"]')?.textContent).toContain('Ya tenés 2 turnos reservados');
  });

  it('lets her change the service from the second step', () => {
    click(root().querySelector('button.service'));

    click(buttonWith('Cambiar servicio'));

    expect(heading()).toBe('Sacá tu turno');
  });

  it('offers to retry when the services cannot be loaded', async () => {
    booking.services.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    fixture = TestBed.createComponent(BookingPage);
    fixture.detectChanges();

    expect(root().querySelector('[role="alert"]')?.textContent).toContain('No pudimos cargar los servicios');
  });
});
