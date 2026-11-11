import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { todayIso } from '../../../../shared/utils/format';
import { CatalogService } from '../../../catalog/services/catalog.service';
import { ClientsService } from '../../../clients/services/clients.service';
import { TreatmentsService } from '../../../treatments/services/treatments.service';
import { Appointment } from '../../models/appointment.models';
import { AppointmentsService } from '../../services/appointments.service';
import { addDays, mondayOf } from '../../utils/agenda-dates';
import { Agenda } from './agenda';

const TODAY = todayIso();

const appointment = (id: number, time: string, overrides: Partial<Appointment> = {}): Appointment => ({
  id, serviceId: 10, serviceName: 'Bozo', categoryName: 'Depilación', startsAt: `${TODAY}T${time}:00`,
  endsAt: `${TODAY}T${time.slice(0, 3)}30:00`, firstName: 'Carina', lastName: 'Diaz', phone: '3415551234',
  clientId: null, status: 'CONFIRMED', source: 'PUBLIC', notes: null, email: null, ...overrides,
});

describe('Agenda', () => {
  const appointments = { search: vi.fn(), changeStatus: vi.fn(), linkClient: vi.fn(), availability: vi.fn(), create: vi.fn(), update: vi.fn() };
  const catalog = { getCategories: vi.fn() };
  let fixture: ComponentFixture<Agenda>;

  const root = () => fixture.nativeElement as HTMLElement;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ');
  const button = (label: string) =>
    Array.from(root().querySelectorAll('button')).find(
      (candidate) => candidate.textContent?.trim() === label || candidate.getAttribute('aria-label') === label,
    ) as HTMLButtonElement;
  const click = (label: string) => {
    button(label).click();
    fixture.detectChanges();
  };
  const start = async () => {
    await TestBed.configureTestingModule({
      imports: [Agenda],
      providers: [
        { provide: AppointmentsService, useValue: appointments },
        { provide: CatalogService, useValue: catalog },
        { provide: ClientsService, useValue: { search: vi.fn(), create: vi.fn() } },
        { provide: TreatmentsService, useValue: { create: vi.fn() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Agenda);
    fixture.detectChanges();
  };

  beforeEach(() => {
    Object.values(appointments).forEach((mock) => mock.mockReset());
    catalog.getCategories.mockReturnValue(of([]));
    appointments.availability.mockReturnValue(of({ date: TODAY, durationMinutes: 30, times: [] }));
  });

  it('opens on today and lists its appointments in order', async () => {
    appointments.search.mockReturnValue(of([
      appointment(1, '09:00'),
      appointment(2, '10:00', { status: 'CANCELLED', source: 'ADMIN' }),
    ]));
    await start();

    expect(appointments.search).toHaveBeenCalledWith(TODAY, TODAY);
    const items = Array.from(root().querySelectorAll('.appointment'));
    expect(items).toHaveLength(2);
    expect(items[0].textContent).toContain('09:00');
    expect(items[0].textContent).toContain('Web');
    expect(items[0].textContent).toContain('Confirmado');
    expect(items[1].classList).toContain('finished');
    expect(items[1].querySelector('.actions')).toBeNull();
  });

  it('switches to the week from Monday to Sunday and moves by weeks', async () => {
    appointments.search.mockReturnValue(of([]));
    await start();
    const monday = mondayOf(TODAY);

    click('Semana');
    expect(appointments.search).toHaveBeenLastCalledWith(monday, addDays(monday, 6));
    expect(root().querySelectorAll('.day')).toHaveLength(7);

    click('Semana siguiente');
    expect(appointments.search).toHaveBeenLastCalledWith(addDays(monday, 7), addDays(monday, 13));

    click('Hoy');
    expect(appointments.search).toHaveBeenLastCalledWith(monday, addDays(monday, 6));
  });

  it('moves day by day', async () => {
    appointments.search.mockReturnValue(of([]));
    await start();
    expect(text()).toContain('Sin turnos.');

    click('Día anterior');

    expect(appointments.search).toHaveBeenLastCalledWith(addDays(TODAY, -1), addDays(TODAY, -1));
  });

  it('cancels an appointment after confirming and reloads', async () => {
    appointments.search.mockReturnValue(of([appointment(1, '09:00')]));
    appointments.changeStatus.mockReturnValue(of(appointment(1, '09:00', { status: 'CANCELLED' })));
    await start();

    click('Cancelar el turno de Carina Diaz');
    expect(text()).toContain('El horario vuelve a quedar libre.');
    (root().querySelector('app-confirm-dialog .btn-danger') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(appointments.changeStatus).toHaveBeenCalledWith(1, 'CANCELLED');
    expect(appointments.search).toHaveBeenCalledTimes(2);
    expect(root().querySelector('app-confirm-dialog')).toBeNull();
  });

  it('marks a no-show', async () => {
    appointments.search.mockReturnValue(of([appointment(1, '09:00')]));
    appointments.changeStatus.mockReturnValue(of(appointment(1, '09:00', { status: 'NO_SHOW' })));
    await start();

    click('Marcar que no vino Carina Diaz');
    (root().querySelector('app-confirm-dialog .btn-danger') as HTMLButtonElement).click();

    expect(appointments.changeStatus).toHaveBeenCalledWith(1, 'NO_SHOW');
  });

  it('after marking it attended offers to register the treatment', async () => {
    const linked = appointment(1, '09:00', { clientId: 7 });
    appointments.search.mockReturnValue(of([linked]));
    appointments.changeStatus.mockReturnValue(of({ ...linked, status: 'ATTENDED' }));
    await start();

    click('Marcar atendido el turno de Carina Diaz');
    click('Marcar atendido');

    expect(appointments.changeStatus).toHaveBeenCalledWith(1, 'ATTENDED');
    expect(root().querySelector('app-treatment-form-dialog')).not.toBeNull();
    expect(text()).toContain('Diaz, Carina');
  });

  it('opens the form to book or move an appointment', async () => {
    appointments.search.mockReturnValue(of([appointment(1, '09:00')]));
    await start();

    click('+ Nuevo turno');
    expect(root().querySelector('app-appointment-form-dialog h2')?.textContent).toContain('Nuevo turno');
    click('Cancelar');

    click('Reprogramar el turno de Carina Diaz');
    expect(root().querySelector('app-appointment-form-dialog h2')?.textContent).toContain('Reprogramar turno');
  });

  it('offers to retry when the agenda cannot be loaded', async () => {
    appointments.search.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await start();

    expect(root().querySelector('[role="alert"]')?.textContent).toContain('No pudimos cargar la agenda');
  });
});
