import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Appointment } from '../../models/appointment.models';
import { AppointmentsService } from '../../services/appointments.service';
import { NoticeDialog } from './notice-dialog';

const APPOINTMENT: Appointment = {
  id: 52,
  serviceId: 10,
  serviceName: 'Bozo',
  categoryName: 'Depilación',
  startsAt: '2026-10-12T10:30:00',
  endsAt: '2026-10-12T11:00:00',
  firstName: 'Carina',
  lastName: 'Diaz',
  phone: '3415551234',
  clientId: null,
  status: 'CONFIRMED',
  source: 'ADMIN',
  notes: null,
  email: 'carina@mail.com',
};

describe('NoticeDialog', () => {
  const appointments = { sendNotice: vi.fn() };
  let fixture: ComponentFixture<NoticeDialog>;

  const root = () => fixture.nativeElement as HTMLElement;
  const sendButton = () =>
    Array.from(root().querySelectorAll('button')).find((button) => button.textContent?.includes('Enviar email')) as HTMLButtonElement;

  function create(appointment: Appointment): void {
    TestBed.configureTestingModule({
      imports: [NoticeDialog],
      providers: [{ provide: AppointmentsService, useValue: appointments }],
    });
    fixture = TestBed.createComponent(NoticeDialog);
    fixture.componentRef.setInput('appointment', appointment);
    fixture.componentRef.setInput('type', 'BOOKED');
    fixture.detectChanges();
  }

  beforeEach(() => vi.resetAllMocks());

  it('sends the notice the owner asked for', () => {
    appointments.sendNotice.mockReturnValue(of(undefined));
    create(APPOINTMENT);
    const sent = vi.fn();
    fixture.componentInstance.sent.subscribe(sent);

    sendButton().click();

    expect(appointments.sendNotice).toHaveBeenCalledWith(52, 'BOOKED');
    expect(sent).toHaveBeenCalled();
  });

  it('cannot send without an email', () => {
    create({ ...APPOINTMENT, email: null });

    expect(sendButton().disabled).toBe(true);
    expect(root().textContent).toContain('La clienta no tiene email cargado');
  });

  it('shows why the email was not sent', () => {
    appointments.sendNotice.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 502, error: { message: 'No se pudo enviar el email. Probá de nuevo más tarde' } })),
    );
    create(APPOINTMENT);

    sendButton().click();
    fixture.detectChanges();

    expect(root().textContent).toContain('No se pudo enviar el email');
  });
});
