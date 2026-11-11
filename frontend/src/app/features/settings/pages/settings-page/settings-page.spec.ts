import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { BusinessSettings } from '../../models/settings.models';
import { SettingsService } from '../../services/settings.service';
import { SettingsPage } from './settings-page';

describe('SettingsPage', () => {
  const settings = { get: vi.fn(), save: vi.fn() };
  let fixture: ComponentFixture<SettingsPage>;

  const root = () => fixture.nativeElement as HTMLElement;
  const input = (id: string) => root().querySelector(`#${id}`) as HTMLInputElement | null;
  const buttonWith = (text: string) =>
    Array.from(root().querySelectorAll('button')).find((button) => button.textContent?.trim() === text);
  const click = (text: string) => {
    buttonWith(text)?.click();
    fixture.detectChanges();
  };
  const fill = (id: string, value: string) => {
    input(id)!.value = value;
    input(id)!.dispatchEvent(new Event('input'));
  };
  const submit = () => {
    root().querySelector('form')?.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };

  function start(current: BusinessSettings): void {
    settings.get.mockReturnValue(of(current));
    TestBed.configureTestingModule({
      imports: [SettingsPage],
      providers: [{ provide: SettingsService, useValue: settings }],
    });
    fixture = TestBed.createComponent(SettingsPage);
    fixture.detectChanges();
  }

  beforeEach(() => vi.resetAllMocks());

  it('shows the saved settings as a card and edits them from there', () => {
    start({ notificationEmail: 'esthetiadmin@mail.com', contactPhone: null });

    expect(root().querySelector('form')).toBeNull();
    expect(root().querySelector('dl')?.textContent).toContain('esthetiadmin@mail.com');
    expect(root().querySelector('dl')?.textContent).toContain('Sin cargar');

    click('Editar');
    expect(input('settings-email')?.value).toBe('esthetiadmin@mail.com');

    click('Cancelar');
    expect(root().querySelector('form')).toBeNull();
  });

  it('opens the form when nothing is set yet', () => {
    start({ notificationEmail: null, contactPhone: null });

    expect(input('settings-email')?.value).toBe('');
    expect(buttonWith('Cancelar')).toBeUndefined();
  });

  it('saves trimmed values, empty ones as null, and goes back to the card', () => {
    start({ notificationEmail: null, contactPhone: null });
    settings.save.mockReturnValue(of({ notificationEmail: null, contactPhone: '3415559876' }));
    fill('settings-email', '  ');
    fill('settings-phone', ' 341 555-9876 ');
    submit();

    expect(settings.save).toHaveBeenCalledWith({ notificationEmail: null, contactPhone: '341 555-9876' });
    expect(root().textContent).toContain('Configuración guardada.');
    expect(root().querySelector('form')).toBeNull();
    expect(root().querySelector('dl')?.textContent).toContain('3415559876');
  });

  it('does not save an invalid email or phone', () => {
    start({ notificationEmail: null, contactPhone: null });
    fill('settings-email', 'esthetiadmin@');
    fill('settings-phone', '123');
    submit();

    expect(settings.save).not.toHaveBeenCalled();
    expect(root().textContent).toContain('Ingresá un email válido');
    expect(root().textContent).toContain('Ingresá un celular de 8 a 15 números');
  });
});
