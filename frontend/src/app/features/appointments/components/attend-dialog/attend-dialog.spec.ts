import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ClientsService } from '../../../clients/services/clients.service';
import { Appointment } from '../../models/appointment.models';
import { AppointmentsService } from '../../services/appointments.service';
import { AttendDialog } from './attend-dialog';

const fromWeb: Appointment = {
  id: 52, serviceId: 10, serviceName: 'Bozo', categoryName: 'Depilación', startsAt: '2026-10-08T10:00:00',
  endsAt: '2026-10-08T10:30:00', firstName: 'Carina', lastName: 'Diaz', phone: '3415551234', clientId: null,
  status: 'CONFIRMED', source: 'PUBLIC', notes: null, email: null,
};

@Component({
  imports: [AttendDialog],
  template: `<app-attend-dialog [appointment]="appointment()" (attended)="log.push($event)" (cancelled)="log.push('cancelled')" />`,
})
class Host {
  readonly appointment = signal<Appointment>(fromWeb);
  readonly log: (Appointment | string)[] = [];
}

describe('AttendDialog', () => {
  const appointments = { linkClient: vi.fn(), changeStatus: vi.fn() };
  const clients = { create: vi.fn(), search: vi.fn() };
  let fixture: ComponentFixture<Host>;
  const attended = { ...fromWeb, clientId: 7, status: 'ATTENDED' as const };

  const root = () => fixture.nativeElement as HTMLElement;
  const confirm = () => {
    (Array.from(root().querySelectorAll('button')).find((b) => b.textContent?.includes('Marcar atendido')) as HTMLButtonElement).click();
    fixture.detectChanges();
  };
  const start = async (appointment: Appointment = fromWeb) => {
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [
        { provide: AppointmentsService, useValue: appointments },
        { provide: ClientsService, useValue: clients },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    fixture.componentInstance.appointment.set(appointment);
    fixture.detectChanges();
  };

  beforeEach(() => {
    Object.values(appointments).forEach((mock) => mock.mockReset());
    Object.values(clients).forEach((mock) => mock.mockReset());
    appointments.linkClient.mockReturnValue(of({ ...fromWeb, clientId: 7 }));
    appointments.changeStatus.mockReturnValue(of(attended));
  });

  it('marks an appointment already linked to a client right away', async () => {
    await start({ ...fromWeb, clientId: 7 });
    expect(root().querySelector('fieldset')).toBeNull();

    confirm();

    expect(appointments.linkClient).not.toHaveBeenCalled();
    expect(appointments.changeStatus).toHaveBeenCalledWith(52, 'ATTENDED');
    expect(fixture.componentInstance.log).toEqual([attended]);
  });

  it('asks which registered client it is before marking it', async () => {
    await start();

    confirm();

    expect(root().textContent).toContain('Elegí el cliente.');
    expect(appointments.changeStatus).not.toHaveBeenCalled();
  });

  it('registers a new client with the appointment data, links it and marks it attended', async () => {
    clients.create.mockReturnValue(of({ id: 7 }));
    await start();

    (root().querySelector('input[value="new"]') as HTMLInputElement).click();
    fixture.detectChanges();
    expect(root().textContent).toContain('teléfono 3415551234');
    confirm();

    expect(clients.create).toHaveBeenCalledWith({
      firstName: 'Carina', lastName: 'Diaz', phone: '3415551234', birthDate: null, notes: null, email: null,
    });
    expect(appointments.linkClient).toHaveBeenCalledWith(52, 7);
    expect(appointments.changeStatus).toHaveBeenCalledWith(52, 'ATTENDED');
  });

  it('shows why it could not be marked', async () => {
    appointments.changeStatus.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { message: 'El turno ya no está confirmado' } })),
    );
    await start({ ...fromWeb, clientId: 7 });

    confirm();

    expect(root().querySelector('[role="alert"]')?.textContent).toContain('El turno ya no está confirmado');
    expect(fixture.componentInstance.log).toEqual([]);
  });
});
