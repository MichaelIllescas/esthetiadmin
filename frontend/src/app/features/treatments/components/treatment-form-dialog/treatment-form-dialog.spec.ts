import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { todayIso } from '../../../../shared/utils/format';
import { Category } from '../../../catalog/models/catalog.models';
import { CatalogService } from '../../../catalog/services/catalog.service';
import { ClientsService } from '../../../clients/services/clients.service';
import { Treatment } from '../../models/treatment.models';
import { TreatmentsService } from '../../services/treatments.service';
import { TreatmentFormDialog } from './treatment-form-dialog';

const categories = (): Category[] => [
  {
    id: 1,
    name: 'Depilación láser',
    active: true,
    appointmentMinutes: 60,
    services: [
      { id: 10, categoryId: 1, name: 'Bozo', description: null, price: 15000, active: true, bookable: true },
      { id: 11, categoryId: 1, name: 'Axilas', description: null, price: 18000, active: false, bookable: true },
    ],
  },
  {
    id: 2,
    name: 'Faciales',
    active: true,
    appointmentMinutes: 60,
    services: [{ id: 20, categoryId: 2, name: 'Limpieza facial', description: null, price: 25000, active: true, bookable: true }],
  },
  {
    id: 3,
    name: 'Masajes',
    active: false,
    appointmentMinutes: 60,
    services: [{ id: 30, categoryId: 3, name: 'Drenaje', description: null, price: 30000, active: true, bookable: true }],
  },
  { id: 4, name: 'Vacía', active: true, appointmentMinutes: 60, services: [] },
];

const treatmentOf = (serviceId: number, serviceName: string, categoryId: number, categoryName: string): Treatment => ({
  id: 31, clientId: 7, clientName: 'Diaz, Carina', serviceId, serviceName, categoryId, categoryName,
  performedOn: '2026-09-10', amount: 16000, notes: 'Con descuento',
});

@Component({
  imports: [TreatmentFormDialog],
  template: `
    <app-treatment-form-dialog
      [fixedClient]="fixedClient()"
      [treatment]="treatment()"
      [preset]="preset()"
      (saved)="log.push('saved')"
      (cancelled)="log.push('cancelled')"
    />
  `,
})
class Host {
  readonly fixedClient = signal<{ id: number; name: string } | null>(null);
  readonly treatment = signal<Treatment | null>(null);
  readonly preset = signal<{ serviceId: number; performedOn: string } | null>(null);
  readonly log: string[] = [];
}

describe('TreatmentFormDialog', () => {
  const treatments = { create: vi.fn(), update: vi.fn() };
  const catalog = { getCategories: vi.fn() };
  const clients = { search: vi.fn() };
  let fixture: ComponentFixture<Host>;

  const el = <T extends HTMLElement>(id: string) => fixture.nativeElement.querySelector(`#${id}`) as T | null;
  const set = (id: string, value: string) => {
    const element = el<HTMLInputElement>(id) as HTMLInputElement;
    element.value = value;
    element.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };
  const choose = (id: string, labelStart: string) => {
    const select = el<HTMLSelectElement>(id) as HTMLSelectElement;
    const option = Array.from(select.options).find((o) => o.textContent?.trim().startsWith(labelStart)) as HTMLOptionElement;
    select.value = option.value;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };
  const pick = (category: string, service: string) => {
    choose('treatment-category', category);
    choose('treatment-service', service);
  };
  const labels = (id: string) =>
    Array.from((el<HTMLSelectElement>(id) as HTMLSelectElement).options).map((o) => o.textContent?.replace(/\s/g, ' ').trim());
  const submit = () => {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };
  const start = async (setup?: (host: Host) => void) => {
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [
        { provide: TreatmentsService, useValue: treatments },
        { provide: CatalogService, useValue: catalog },
        { provide: ClientsService, useValue: clients },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    setup?.(fixture.componentInstance);
    fixture.detectChanges();
  };
  const withClient = (host: Host) => host.fixedClient.set({ id: 7, name: 'Diaz, Carina' });

  beforeEach(() => {
    Object.values(treatments).forEach((fn) => fn.mockReset());
    catalog.getCategories.mockReset();
    catalog.getCategories.mockReturnValue(of(categories()));
    clients.search.mockReset();
  });

  it('starts on today, editable, and never allows a future day', async () => {
    await start(withClient);

    expect((el<HTMLInputElement>('treatment-date') as HTMLInputElement).value).toBe(todayIso());
    expect((el<HTMLInputElement>('treatment-date') as HTMLInputElement).max).toBe(todayIso());
    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Registrar tratamiento');
  });

  it('shows the fixed client as read-only text instead of a picker', async () => {
    await start(withClient);

    expect(fixture.nativeElement.querySelector('.static-value').textContent).toContain('Diaz, Carina');
    expect(fixture.nativeElement.querySelector('app-client-picker')).toBeNull();
  });

  it('shows the client picker when no client was decided beforehand', async () => {
    await start();

    expect(fixture.nativeElement.querySelector('app-client-picker')).not.toBeNull();
  });

  it('offers first the categories that have something to offer today', async () => {
    await start();

    expect(labels('treatment-category')).toEqual(['Elegí una categoría', 'Depilación láser', 'Faciales']);
  });

  it('asks for the category first and only then shows the services of that category', async () => {
    await start();
    expect(el('treatment-service')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Primero elegí una categoría.');

    choose('treatment-category', 'Depilación láser');

    expect(labels('treatment-service')).toEqual(['Elegí un servicio', 'Bozo · $ 15.000']);
    choose('treatment-category', 'Faciales');
    expect(labels('treatment-service')).toEqual(['Elegí un servicio', 'Limpieza facial · $ 25.000']);
  });

  it('clears the chosen service when the category changes', async () => {
    treatments.create.mockReturnValue(of({}));
    await start(withClient);
    pick('Depilación láser', 'Bozo');

    choose('treatment-category', 'Faciales');
    submit();

    expect(treatments.create).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Elegí un servicio.');
  });

  it('fills in the price of the chosen service, which the user can then change', async () => {
    await start(withClient);

    pick('Depilación láser', 'Bozo');
    expect((el<HTMLInputElement>('treatment-amount') as HTMLInputElement).value).toBe('15000');
    expect(fixture.nativeElement.textContent).toContain('Precio del servicio');

    set('treatment-amount', '12000');
    pick('Faciales', 'Limpieza');
    expect((el<HTMLInputElement>('treatment-amount') as HTMLInputElement).value).toBe('12000');
  });

  it('starts from a preset service and date, with the service price', async () => {
    treatments.create.mockReturnValue(of({}));
    await start((host) => {
      withClient(host);
      host.preset.set({ serviceId: 20, performedOn: '2026-09-15' });
    });

    expect((el<HTMLSelectElement>('treatment-category') as HTMLSelectElement).selectedOptions[0].textContent).toContain('Faciales');
    expect((el<HTMLInputElement>('treatment-date') as HTMLInputElement).value).toBe('2026-09-15');
    expect((el<HTMLInputElement>('treatment-amount') as HTMLInputElement).value).toBe('25000');

    submit();

    expect(treatments.create).toHaveBeenCalledWith(expect.objectContaining({ clientId: 7, serviceId: 20, amount: 25000 }));
  });

  it('registers a treatment for the fixed client', async () => {
    treatments.create.mockReturnValue(of({}));
    await start(withClient);

    pick('Depilación láser', 'Bozo');
    set('treatment-date', '2026-09-20');
    set('treatment-notes', '  Sin reacciones ');
    submit();

    expect(treatments.create).toHaveBeenCalledWith({
      clientId: 7,
      serviceId: 10,
      performedOn: '2026-09-20',
      amount: 15000,
      notes: 'Sin reacciones',
    });
    expect(fixture.componentInstance.log).toEqual(['saved']);
  });

  it('accepts a free treatment', async () => {
    treatments.create.mockReturnValue(of({}));
    await start(withClient);

    pick('Depilación láser', 'Bozo');
    set('treatment-amount', '0');
    submit();

    expect(treatments.create).toHaveBeenCalledWith(expect.objectContaining({ amount: 0, notes: null }));
  });

  it('asks for the missing data and does not send anything', async () => {
    await start();

    submit();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Elegí un cliente.');
    expect(text).toContain('Elegí una categoría.');
    expect(text).toContain('Ingresá el importe (0 si fue gratis).');
    expect(treatments.create).not.toHaveBeenCalled();
  });

  it('rejects a negative amount and a future date', async () => {
    await start(withClient);

    pick('Depilación láser', 'Bozo');
    set('treatment-amount', '-1');
    set('treatment-date', '2999-01-01');
    submit();

    expect(fixture.nativeElement.textContent).toContain('Ingresá el importe');
    expect(fixture.nativeElement.textContent).toContain('no sea futura');
    expect(treatments.create).not.toHaveBeenCalled();
  });

  it('edits a treatment starting from its data, with its category already open', async () => {
    treatments.update.mockReturnValue(of({}));
    await start((host) => host.treatment.set(treatmentOf(10, 'Bozo', 1, 'Depilación láser')));

    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Editar tratamiento');
    expect(fixture.nativeElement.querySelector('.static-value').textContent).toContain('Diaz, Carina');
    expect((el<HTMLSelectElement>('treatment-category') as HTMLSelectElement).selectedOptions[0].textContent).toContain('Depilación láser');
    expect((el<HTMLSelectElement>('treatment-service') as HTMLSelectElement).selectedOptions[0].textContent).toContain('Bozo');
    expect((el<HTMLInputElement>('treatment-amount') as HTMLInputElement).value).toBe('16000');
    expect((el<HTMLInputElement>('treatment-date') as HTMLInputElement).value).toBe('2026-09-10');

    set('treatment-date', '2026-09-11');
    submit();

    expect(treatments.update).toHaveBeenCalledWith(31, {
      serviceId: 10,
      performedOn: '2026-09-11',
      amount: 16000,
      notes: 'Con descuento',
    });
  });

  it('keeps the current service of an old treatment even if it is not offered anymore', async () => {
    await start((host) => host.treatment.set(treatmentOf(11, 'Axilas', 1, 'Depilación láser')));

    expect(labels('treatment-service')).toEqual(['Elegí un servicio', 'Bozo · $ 15.000', 'Axilas (ya no se ofrece)']);
    expect((el<HTMLSelectElement>('treatment-service') as HTMLSelectElement).selectedOptions[0].textContent).toContain('Axilas');
  });

  it('keeps the current category of an old treatment even if the whole category was turned off', async () => {
    await start((host) => host.treatment.set(treatmentOf(30, 'Drenaje', 3, 'Masajes')));

    expect(labels('treatment-category')).toContain('Masajes (inactiva)');
    expect((el<HTMLSelectElement>('treatment-service') as HTMLSelectElement).selectedOptions[0].textContent).toContain('Drenaje');
  });

  it('shows the reason when the API refuses the treatment and stays open', async () => {
    treatments.create.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { message: 'El servicio no está disponible: está desactivado o su categoría está inactiva' } })),
    );
    await start(withClient);

    pick('Depilación láser', 'Bozo');
    submit();

    expect(fixture.nativeElement.querySelector('form [role="alert"]').textContent).toContain('no está disponible');
    expect(fixture.componentInstance.log).toEqual([]);
  });

  it('warns when there are no services to choose from', async () => {
    catalog.getCategories.mockReturnValue(of([]));
    await start(withClient);

    expect(fixture.nativeElement.textContent).toContain('No hay servicios disponibles');
  });

  it('shows a retry when the services cannot be loaded', async () => {
    catalog.getCategories.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })));
    await start(withClient);
    expect(fixture.nativeElement.textContent).toContain('No pudimos cargar los servicios');

    (Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[]).find((b) => b.textContent?.trim() === 'Reintentar')?.click();
    fixture.detectChanges();

    expect(labels('treatment-category')).toContain('Depilación láser');
  });

  it('reports cancelled', async () => {
    await start();

    (Array.from(fixture.nativeElement.querySelectorAll('.form-actions button')) as HTMLButtonElement[])[0].click();

    expect(fixture.componentInstance.log).toEqual(['cancelled']);
  });
});
