import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Category } from '../../../catalog/models/catalog.models';
import { CatalogService } from '../../../catalog/services/catalog.service';
import { ClientsService } from '../../../clients/services/clients.service';
import { Appointment } from '../../models/appointment.models';
import { AppointmentsService } from '../../services/appointments.service';
import { AppointmentFormDialog } from './appointment-form-dialog';

const DAY = '2099-10-08';

const categories: Category[] = [
  {
    id: 1, name: 'Depilación', active: true, appointmentMinutes: 30,
    services: [
      { id: 10, categoryId: 1, name: 'Bozo', description: null, price: 1000, active: true, bookable: true },
      { id: 11, categoryId: 1, name: 'Axilas', description: null, price: 1000, active: false, bookable: true },
    ],
  },
  { id: 2, name: 'Masajes', active: false, appointmentMinutes: 60, services: [{ id: 20, categoryId: 2, name: 'Drenaje', description: null, price: 1, active: true, bookable: true }] },
];

const existing: Appointment = {
  id: 52, serviceId: 10, serviceName: 'Bozo', categoryName: 'Depilación', startsAt: `${DAY}T10:00:00`,
  endsAt: `${DAY}T10:30:00`, firstName: 'Carina', lastName: 'Diaz', phone: '3415551234', clientId: null,
  status: 'CONFIRMED', source: 'PUBLIC', notes: null, email: null,
};

@Component({
  imports: [AppointmentFormDialog],
  template: `<app-appointment-form-dialog [appointment]="appointment()" [defaultDate]="defaultDate()"
    (saved)="log.push($event)" (cancelled)="log.push('cancelled')" />`,
})
class Host {
  readonly appointment = signal<Appointment | null>(null);
  readonly defaultDate = signal<string | null>(DAY);
  readonly log: (Appointment | string)[] = [];
}

describe('AppointmentFormDialog', () => {
  const appointments = { availability: vi.fn(), create: vi.fn(), update: vi.fn() };
  const catalog = { getCategories: vi.fn() };
  const clients = { search: vi.fn() };
  let fixture: ComponentFixture<Host>;

  const root = () => fixture.nativeElement as HTMLElement;
  const field = <T extends HTMLElement>(id: string) => root().querySelector(`#${id}`) as T;
  const fill = (id: string, value: string) => {
    const element = field<HTMLInputElement>(id);
    element.value = value;
    element.dispatchEvent(new Event(element.tagName === 'SELECT' ? 'change' : 'input'));
    fixture.detectChanges();
  };
  const choose = (id: string, label: string) => {
    const select = field<HTMLSelectElement>(id);
    select.selectedIndex = Array.from(select.options).findIndex((option) => option.textContent?.trim() === label);
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };
  const chooseService = (name: string) => {
    choose('appointment-category', 'Depilación');
    choose('appointment-service', name);
  };
  const options = (id: string) =>
    Array.from(field<HTMLSelectElement>(id).options).map((option) => option.textContent?.trim());
  const submit = () => {
    root().querySelector('form')?.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };
  const start = async (setup?: (host: Host) => void) => {
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [
        { provide: AppointmentsService, useValue: appointments },
        { provide: CatalogService, useValue: catalog },
        { provide: ClientsService, useValue: clients },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    setup?.(fixture.componentInstance);
    fixture.detectChanges();
  };

  beforeEach(() => {
    Object.values(appointments).forEach((mock) => mock.mockReset());
    catalog.getCategories.mockReturnValue(of(categories));
    appointments.availability.mockReturnValue(of({ date: DAY, durationMinutes: 30, times: ['09:00', '09:30'] }));
  });

  it('asks for the category first and then offers only its services that can be booked', async () => {
    await start();

    expect(options('appointment-category')).toEqual(['Elegí una categoría', 'Depilación']);
    expect(field('appointment-service')).toBeNull();
    expect(root().textContent).toContain('Primero elegí una categoría.');

    choose('appointment-category', 'Depilación');

    expect(options('appointment-service')).toEqual(['Elegí un servicio', 'Bozo']);
  });

  it('shows the free times of the chosen service and day, and books one', async () => {
    appointments.create.mockReturnValue(of(existing));
    await start();
    expect(root().textContent).toContain('Elegí el servicio para ver los horarios libres.');

    chooseService('Bozo');

    expect(appointments.availability).toHaveBeenLastCalledWith(10, DAY, null);
    expect(options('appointment-time')).toEqual(['Elegí una hora', '09:00', '09:30']);
    expect(root().textContent).toContain('turno de 30 minutos');

    fill('appointment-time', '09:30');
    fill('appointment-first-name', ' Carina ');
    fill('appointment-last-name', 'Diaz');
    fill('appointment-phone', '341 555-1234');
    submit();

    expect(appointments.create).toHaveBeenCalledWith({
      serviceId: 10,
      startsAt: `${DAY}T09:30:00`,
      clientId: null,
      firstName: 'Carina',
      lastName: 'Diaz',
      phone: '341 555-1234',
      notes: null, email: null,
    });
    expect(fixture.componentInstance.log).toEqual([existing]);
  });

  it('offers every half hour when booking outside the usual hours', async () => {
    appointments.availability.mockReturnValue(of({ date: DAY, durationMinutes: 30, times: [] }));
    await start();
    chooseService('Bozo');
    expect(root().textContent).toContain('No quedan horarios libres ese día');

    (root().querySelector('input[role="switch"]') as HTMLInputElement).click();
    fixture.detectChanges();

    expect(options('appointment-time')).toHaveLength(49);
  });

  it('asks for the missing data and a valid phone', async () => {
    await start();

    fill('appointment-phone', '123');
    submit();

    const text = root().textContent;
    expect(text).toContain('Elegí una categoría.');
    expect(text).toContain('Elegí una hora.');
    expect(text).toContain('Ingresá el nombre.');
    expect(text).toContain('Ingresá un teléfono de 8 a 15 números.');
    expect(appointments.create).not.toHaveBeenCalled();
  });

  it('moves an appointment keeping its data and freeing its own time', async () => {
    appointments.update.mockReturnValue(of(existing));
    appointments.availability.mockReturnValue(of({ date: DAY, durationMinutes: 30, times: ['09:30', '10:00'] }));
    await start((host) => host.appointment.set(existing));

    expect(root().querySelector('h2')?.textContent).toContain('Reprogramar turno');
    expect(appointments.availability).toHaveBeenLastCalledWith(10, DAY, 52);
    expect(field<HTMLSelectElement>('appointment-category').selectedOptions[0].textContent?.trim()).toBe('Depilación');
    expect(field<HTMLSelectElement>('appointment-service').selectedOptions[0].textContent?.trim()).toBe('Bozo');
    expect(field<HTMLSelectElement>('appointment-time').value).toBe('10:00');
    expect(root().querySelector('app-client-picker')).toBeNull();

    fill('appointment-time', '09:30');
    submit();

    expect(appointments.update).toHaveBeenCalledWith(52, expect.objectContaining({ startsAt: `${DAY}T09:30:00`, firstName: 'Carina' }));
  });

  it('keeps the own time of an appointment booked outside the usual hours', async () => {
    appointments.availability.mockReturnValue(of({ date: DAY, durationMinutes: 30, times: ['09:30'] }));
    await start((host) => host.appointment.set(existing));

    expect(field<HTMLSelectElement>('appointment-time').value).toBe('10:00');
    expect((root().querySelector('input[role="switch"]') as HTMLInputElement).checked).toBe(true);
  });

  it('shows why the appointment could not be saved', async () => {
    appointments.create.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { message: 'Ese horario ya está ocupado' } })),
    );
    await start();
    chooseService('Bozo');
    fill('appointment-time', '09:00');
    fill('appointment-first-name', 'Carina');
    fill('appointment-last-name', 'Diaz');
    fill('appointment-phone', '3415551234');

    submit();

    expect(root().querySelector('[role="alert"]')?.textContent).toContain('Ese horario ya está ocupado');
    expect(fixture.componentInstance.log).toEqual([]);
  });
});
