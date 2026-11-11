import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { BusinessSettings } from '../../models/settings.models';
import { SettingsService } from '../../services/settings.service';
import { SettingsReminder } from './settings-reminder';

describe('SettingsReminder', () => {
  const settings = { get: vi.fn() };
  let fixture: ComponentFixture<SettingsReminder>;

  const text = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  function start(response: Observable<BusinessSettings>): void {
    settings.get.mockReturnValue(response);
    TestBed.configureTestingModule({
      imports: [SettingsReminder],
      providers: [provideRouter([]), { provide: SettingsService, useValue: settings }],
    });
    fixture = TestBed.createComponent(SettingsReminder);
    fixture.detectChanges();
  }

  beforeEach(() => vi.resetAllMocks());

  it('reminds of the missing phone with a link to Configuración', () => {
    start(of({ notificationEmail: 'esthetiadmin@mail.com', contactPhone: null }));

    expect(text()).toContain('Falta tu celular de contacto');
    expect(text()).not.toContain('Falta tu email de avisos');
    expect((fixture.nativeElement as HTMLElement).querySelector('a')?.getAttribute('href')).toBe('/configuracion');
  });

  it('reminds of both when nothing is set', () => {
    start(of({ notificationEmail: null, contactPhone: null }));

    expect(text()).toContain('Falta tu celular de contacto');
    expect(text()).toContain('Falta tu email de avisos');
  });

  it('shows nothing when everything is set or the settings cannot be read', () => {
    start(of({ notificationEmail: 'esthetiadmin@mail.com', contactPhone: '3415559876' }));
    expect(text().trim()).toBe('');

    TestBed.resetTestingModule();
    start(throwError(() => new Error('offline')));
    expect(text().trim()).toBe('');
  });
});
