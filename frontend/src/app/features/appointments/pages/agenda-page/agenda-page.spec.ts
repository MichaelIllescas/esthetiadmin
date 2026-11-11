import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';
import { SettingsService } from '../../../settings/services/settings.service';
import { AppointmentsService } from '../../services/appointments.service';
import { AgendaPage } from './agenda-page';

describe('AgendaPage', () => {
  it('shows the explanation and the agenda', async () => {
    await TestBed.configureTestingModule({
      imports: [AgendaPage],
      providers: [
        provideRouter([]),
        { provide: AppointmentsService, useValue: { search: () => of([]) } },
        { provide: SettingsService, useValue: { get: () => of({ notificationEmail: null, contactPhone: null }) } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(AgendaPage);
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('h1')?.textContent).toContain('Agenda');
    expect(root.querySelector('details.help summary')?.textContent).toContain('¿Cómo funciona la agenda?');
    expect(root.querySelector('app-agenda')).not.toBeNull();
    expect(root.querySelector('app-booking-link')).not.toBeNull();
    expect(root.textContent).toContain('Falta tu celular de contacto');
  });
});
