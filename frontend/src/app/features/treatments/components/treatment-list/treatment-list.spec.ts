import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Page } from '../../../../shared/models/page.model';
import { Category } from '../../../catalog/models/catalog.models';
import { CatalogService } from '../../../catalog/services/catalog.service';
import { ClientsService } from '../../../clients/services/clients.service';
import { Treatment } from '../../models/treatment.models';
import { TreatmentsService } from '../../services/treatments.service';
import { TreatmentList } from './treatment-list';

const treatment = (id: number, serviceName: string, amount: number, notes: string | null = null): Treatment => ({
  id, clientId: 7, clientName: 'Diaz, Carina', serviceId: 10, serviceName, categoryId: 1,
  categoryName: 'Depilación láser', performedOn: '2026-09-25', amount, notes,
});

const page = (content: Treatment[], index = 0, totalPages = 1, total = content.length): Page<Treatment> => ({
  content, page: index, size: 20, totalElements: total, totalPages,
});

const categories: Category[] = [
  {
    id: 1, name: 'Depilación láser', active: true, appointmentMinutes: 30,
    services: [
      { id: 10, categoryId: 1, name: 'Bozo', description: null, price: 15000, active: true, bookable: true },
      { id: 11, categoryId: 1, name: 'Axilas', description: null, price: 18000, active: true, bookable: true },
    ],
  },
  { id: 2, name: 'Faciales', active: true, appointmentMinutes: 60, services: [{ id: 20, categoryId: 2, name: 'Limpieza', description: null, price: 25000, active: true, bookable: true }] },
];

@Component({
  imports: [TreatmentList],
  template: `<app-treatment-list [clientId]="clientId()" [clientName]="clientName()" />`,
})
class Host {
  readonly clientId = signal<number | null>(null);
  readonly clientName = signal<string | null>(null);
}

describe('TreatmentList', () => {
  const treatments = { search: vi.fn(), delete: vi.fn(), create: vi.fn(), update: vi.fn() };
  const catalog = { getCategories: vi.fn() };
  const clients = { search: vi.fn() };
  let fixture: ComponentFixture<Host>;

  const root = () => fixture.nativeElement as HTMLElement;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ');
  const flush = () => fixture.detectChanges();
  const byText = (label: string) =>
    (Array.from(root().querySelectorAll('button')) as HTMLButtonElement[]).find((b) => b.textContent?.trim() === label) as HTMLButtonElement;
  const byLabel = (label: string) =>
    (Array.from(root().querySelectorAll('button')) as HTMLButtonElement[]).find((b) => b.getAttribute('aria-label') === label) as HTMLButtonElement;
  const inDialog = (label: string) =>
    (Array.from(root().querySelectorAll('dialog button')) as HTMLButtonElement[]).find((b) => b.textContent?.trim() === label) as HTMLButtonElement;
  const lastFilters = () => treatments.search.mock.calls.at(-1)?.[0];
  const choose = (id: string, value: string) => {
    const select = root().querySelector(`#${id}`) as HTMLSelectElement;
    select.value = value;
    select.dispatchEvent(new Event('change'));
    flush();
  };
  const setDate = (id: string, value: string) => {
    const input = root().querySelector(`#${id}`) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('change'));
    flush();
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
    flush();
  };

  beforeEach(() => {
    Object.values(treatments).forEach((fn) => fn.mockReset());
    catalog.getCategories.mockReset();
    clients.search.mockReset();
    catalog.getCategories.mockReturnValue(of(categories));
    treatments.search.mockReturnValue(of(page([treatment(1, 'Bozo', 15000, 'Sin reacciones'), treatment(2, 'Axilas', 0)])));
  });

  it('lists every client’s treatments with the client, the service, the amount and the notes', async () => {
    await start();

    expect(lastFilters()).toEqual({ clientId: null, categoryId: null, serviceId: null, from: '', to: '' });
    const headers = Array.from(root().querySelectorAll('th')).map((th) => th.textContent?.trim());
    expect(headers).toContain('Cliente');
    expect(text()).toContain('Diaz, Carina');
    expect(text()).toContain('Bozo');
    expect(text().replace(/\s/g, ' ')).toContain('$ 15.000');
    expect(text()).toContain('Sin reacciones');
    expect(text()).toContain('25/09/2026');
    expect(text()).toContain('2 tratamientos · Página 1 de 1');
    expect(root().querySelector('app-client-picker')).not.toBeNull();
  });

  it('shows a free treatment as zero', async () => {
    await start();

    expect(text().replace(/\s/g, ' ')).toContain('$ 0');
  });

  it('for one client, always filters by it and hides the client column and filter', async () => {
    await start((host) => {
      host.clientId.set(7);
      host.clientName.set('Diaz, Carina');
    });

    expect(lastFilters().clientId).toBe(7);
    expect(Array.from(root().querySelectorAll('th')).map((th) => th.textContent?.trim())).not.toContain('Cliente');
    expect(root().querySelector('app-client-picker')).toBeNull();
  });

  it('filters by category and narrows the services to that category', async () => {
    await start();

    choose('filter-category', '2');

    expect(lastFilters()).toEqual(expect.objectContaining({ categoryId: 2, serviceId: null }));
    const serviceOptions = Array.from(root().querySelectorAll('#filter-service option')).map((o) => o.textContent?.trim());
    expect(serviceOptions).toEqual(['Todos', 'Limpieza']);
  });

  it('drops the chosen service when a category that does not contain it is picked', async () => {
    await start();
    choose('filter-service', '10');
    expect(lastFilters().serviceId).toBe(10);

    choose('filter-category', '2');

    expect(lastFilters()).toEqual(expect.objectContaining({ categoryId: 2, serviceId: null }));
  });

  it('filters by service and by date range, going back to the first page', async () => {
    treatments.search.mockReturnValue(of(page([treatment(1, 'Bozo', 1)], 0, 3, 45)));
    await start();
    treatments.search.mockReturnValue(of(page([treatment(1, 'Bozo', 1)], 1, 3, 45)));
    byText('Siguiente').click();
    flush();
    expect(treatments.search).toHaveBeenLastCalledWith(expect.anything(), 1, 20);

    choose('filter-service', '10');
    setDate('filter-from', '2026-09-01');
    setDate('filter-to', '2026-09-30');

    expect(lastFilters()).toEqual({ clientId: null, categoryId: null, serviceId: 10, from: '2026-09-01', to: '2026-09-30' });
    expect(treatments.search).toHaveBeenLastCalledWith(expect.anything(), 0, 20);
  });

  it('does not search with an inverted date range and says why', async () => {
    await start();
    setDate('filter-from', '2026-09-30');
    treatments.search.mockClear();

    setDate('filter-to', '2026-09-01');

    expect(treatments.search).not.toHaveBeenCalled();
    expect(root().querySelector('[role="alert"]')?.textContent).toContain('no puede ser posterior');
  });

  it('clears every filter at once', async () => {
    await start();
    choose('filter-category', '1');
    setDate('filter-from', '2026-09-01');
    expect(text()).toContain('Limpiar filtros');

    byText('Limpiar filtros').click();
    flush();

    expect(lastFilters()).toEqual({ clientId: null, categoryId: null, serviceId: null, from: '', to: '' });
    expect(text()).not.toContain('Limpiar filtros');
  });

  it('explains when nothing matches the filters and when there is no history yet', async () => {
    treatments.search.mockReturnValue(of(page([], 0, 0, 0)));
    await start();
    expect(text()).toContain('Todavía no hay tratamientos registrados.');

    choose('filter-category', '1');
    expect(text()).toContain('No hay tratamientos que coincidan con los filtros.');
  });

  it('says it is the client’s history when there is none yet', async () => {
    treatments.search.mockReturnValue(of(page([], 0, 0, 0)));
    await start((host) => {
      host.clientId.set(7);
      host.clientName.set('Diaz, Carina');
    });

    expect(text()).toContain('Todavía no hay tratamientos registrados para este cliente.');
  });

  it('shows a retry when the treatments cannot be loaded', async () => {
    treatments.search.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })));
    await start();
    expect(root().querySelector('.status-error')?.textContent).toContain('No pudimos cargar los tratamientos');

    byText('Reintentar').click();
    flush();

    expect(text()).toContain('Bozo');
  });

  it('moves between pages', async () => {
    treatments.search.mockReturnValue(of(page([treatment(1, 'Bozo', 1)], 0, 3, 45)));
    await start();
    expect(byText('Anterior').disabled).toBe(true);

    byText('Siguiente').click();
    flush();

    expect(treatments.search).toHaveBeenLastCalledWith(expect.anything(), 1, 20);
    expect(text()).toContain('Página 2 de 3');
  });

  it('opens the form to register, with the client already decided in a client record', async () => {
    await start((host) => {
      host.clientId.set(7);
      host.clientName.set('Diaz, Carina');
    });

    byText('+ Registrar tratamiento').click();
    flush();

    expect(root().querySelector('dialog h2')?.textContent).toContain('Registrar tratamiento');
    expect(root().querySelector('dialog .static-value')?.textContent).toContain('Diaz, Carina');
  });

  it('opens the form to edit the chosen treatment', async () => {
    await start();

    byLabel('Editar el tratamiento Bozo del 25 de septiembre de 2026').click();
    flush();

    expect(root().querySelector('dialog h2')?.textContent).toContain('Editar tratamiento');
  });

  it('reloads after saving', async () => {
    treatments.create.mockReturnValue(of({}));
    await start((host) => {
      host.clientId.set(7);
      host.clientName.set('Diaz, Carina');
    });
    byText('+ Registrar tratamiento').click();
    flush();
    const pickOption = (id: string, label: string) => {
      const select = root().querySelector(`#${id}`) as HTMLSelectElement;
      select.value = (Array.from(select.options).find((o) => o.textContent?.trim().startsWith(label)) as HTMLOptionElement).value;
      select.dispatchEvent(new Event('change'));
      flush();
    };
    pickOption('treatment-category', 'Depilación láser');
    pickOption('treatment-service', 'Bozo');
    treatments.search.mockClear();

    root().querySelector('dialog form')?.dispatchEvent(new Event('submit'));
    flush();

    expect(treatments.create).toHaveBeenCalledTimes(1);
    expect(root().querySelector('dialog')).toBeNull();
    expect(treatments.search).toHaveBeenCalledTimes(1);
  });

  it('deletes a treatment after confirming, warning that the income stops counting', async () => {
    treatments.delete.mockReturnValue(of(undefined));
    await start();

    byLabel('Eliminar el tratamiento Bozo del 25 de septiembre de 2026').click();
    flush();
    expect(text()).toContain('deja de contarse en los reportes');
    inDialog('Eliminar').click();
    flush();

    expect(treatments.delete).toHaveBeenCalledWith(1);
    expect(root().querySelector('dialog')).toBeNull();
  });

  it('keeps the confirmation open and explains when the deletion fails', async () => {
    treatments.delete.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404, error: { message: 'El tratamiento no existe' } })));
    await start();

    byLabel('Eliminar el tratamiento Bozo del 25 de septiembre de 2026').click();
    flush();
    inDialog('Eliminar').click();
    flush();

    expect(root().querySelector('dialog [role="alert"]')?.textContent).toContain('El tratamiento no existe');
  });

  it('goes back one page when the last treatment of the last page is deleted', async () => {
    treatments.search.mockReturnValue(of(page([treatment(1, 'Bozo', 1)], 0, 2, 21)));
    treatments.delete.mockReturnValue(of(undefined));
    await start();
    treatments.search.mockReturnValue(of(page([treatment(2, 'Axilas', 1)], 1, 2, 21)));
    byText('Siguiente').click();
    flush();

    byLabel('Eliminar el tratamiento Axilas del 25 de septiembre de 2026').click();
    flush();
    treatments.search.mockClear();
    inDialog('Eliminar').click();
    flush();

    expect(treatments.search).toHaveBeenLastCalledWith(expect.anything(), 0, 20);
  });
});
