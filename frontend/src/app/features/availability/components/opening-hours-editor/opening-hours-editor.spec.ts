import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AvailabilityRule } from '../../models/availability.models';
import { AvailabilityService } from '../../services/availability.service';
import { OpeningHoursEditor } from './opening-hours-editor';

const mondayMorning: AvailabilityRule = { dayOfWeek: 1, startTime: '09:00', endTime: '13:00' };

describe('OpeningHoursEditor', () => {
  const availability = { getRules: vi.fn(), saveRules: vi.fn() };
  let fixture: ComponentFixture<OpeningHoursEditor>;

  const root = () => fixture.nativeElement as HTMLElement;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ');
  const byLabel = (label: string) =>
    root().querySelector(`[aria-label="${label}"]`) as HTMLButtonElement;
  const saveButton = () =>
    Array.from(root().querySelectorAll('button')).find((button) =>
      button.textContent?.includes('Guardar horarios'),
    ) as HTMLButtonElement;
  const edit = () => {
    (Array.from(root().querySelectorAll('button')).find((button) =>
      button.textContent?.includes('Editar horarios'),
    ) as HTMLButtonElement).click();
    fixture.detectChanges();
  };
  const choose = (id: string, value: string) => {
    const select = root().querySelector(`#${id}`) as HTMLSelectElement;
    select.value = value;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };
  const start = async (rules: AvailabilityRule[] = [mondayMorning]) => {
    availability.getRules.mockReturnValue(of(rules));
    await TestBed.configureTestingModule({
      imports: [OpeningHoursEditor],
      providers: [{ provide: AvailabilityService, useValue: availability }],
    }).compileComponents();
    fixture = TestBed.createComponent(OpeningHoursEditor);
    fixture.detectChanges();
  };

  beforeEach(() => {
    availability.getRules.mockReset();
    availability.saveRules.mockReset();
  });

  it('shows the saved week as text, one line per day', async () => {
    await start([mondayMorning, { dayOfWeek: 1, startTime: '15:00', endTime: '18:30' }]);

    const rows = Array.from(root().querySelectorAll('.summary-row')).map((row) =>
      Array.from(row.children).map((cell) => cell.textContent?.trim()).join(' '),
    );
    expect(rows[0]).toBe('Lunes 09:00 a 13:00 y 15:00 a 18:30');
    expect(rows[1]).toBe('Martes Cerrado');
    expect(rows).toHaveLength(7);
    expect(root().querySelector('select')).toBeNull();
  });

  it('opens straight in edit mode when nothing is configured yet', async () => {
    await start([]);

    expect(root().querySelector('.summary')).toBeNull();
    expect(root().querySelectorAll('.closed')).toHaveLength(7);
  });

  it('cancelling an edit goes back to the saved week', async () => {
    await start();
    edit();
    byLabel('Quitar el horario 1 del Lunes').click();
    fixture.detectChanges();

    (Array.from(root().querySelectorAll('button')).find((button) => button.textContent?.trim() === 'Cancelar') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(root().querySelector('.summary-row')?.textContent).toContain('09:00 a 13:00');
  });

  it('shows every day in edit mode, with its ranges or as closed', async () => {
    await start();
    edit();

    expect(text()).toContain('Lunes');
    expect(text()).toContain('Domingo');
    expect((root().querySelector('#start-1-0') as HTMLSelectElement).value).toBe('09:00');
    expect((root().querySelector('#end-1-0') as HTMLSelectElement).value).toBe('13:00');
    expect(root().querySelectorAll('.closed')).toHaveLength(6);
    expect(saveButton().disabled).toBe(true);
  });

  it('adds an afternoon range after a morning one and saves the whole week', async () => {
    await start();
    edit();
    availability.saveRules.mockImplementation((rules: AvailabilityRule[]) => of(rules));

    byLabel('Agregar un horario al Lunes').click();
    fixture.detectChanges();
    byLabel('Agregar un horario al Sábado').click();
    fixture.detectChanges();
    expect(text()).toContain('Tenés cambios sin guardar.');

    saveButton().click();
    fixture.detectChanges();

    expect(availability.saveRules).toHaveBeenCalledWith([
      mondayMorning,
      { dayOfWeek: 1, startTime: '15:00', endTime: '19:00' },
      { dayOfWeek: 6, startTime: '09:00', endTime: '13:00' },
    ]);
    expect(text()).toContain('Horarios guardados.');
    expect(root().querySelector('select')).toBeNull();
  });

  it('does not let overlapping ranges be saved', async () => {
    await start();
    edit();
    byLabel('Agregar un horario al Lunes').click();
    fixture.detectChanges();

    choose('start-1-1', '12:00');

    expect(text()).toContain('Los rangos se superponen.');
    expect(saveButton().disabled).toBe(true);
  });

  it('closes a day by removing its only range', async () => {
    await start();
    edit();
    availability.saveRules.mockReturnValue(of([]));

    byLabel('Quitar el horario 1 del Lunes').click();
    fixture.detectChanges();
    saveButton().click();

    expect(availability.saveRules).toHaveBeenCalledWith([]);
  });

  it('shows why the week could not be saved', async () => {
    await start();
    edit();
    availability.saveRules.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: { message: 'Los rangos del lunes se superponen' } })),
    );

    choose('end-1-0', '14:00');
    saveButton().click();
    fixture.detectChanges();

    expect(root().querySelector('[role="alert"]')?.textContent).toContain('Los rangos del lunes se superponen');
  });

  it('offers to retry when the schedule cannot be loaded', async () => {
    availability.getRules.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await TestBed.configureTestingModule({
      imports: [OpeningHoursEditor],
      providers: [{ provide: AvailabilityService, useValue: availability }],
    }).compileComponents();
    fixture = TestBed.createComponent(OpeningHoursEditor);
    fixture.detectChanges();

    expect(root().querySelector('[role="alert"]')?.textContent).toContain('No pudimos cargar los horarios');
  });
});
