import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { Client } from '../../models/client.models';
import { CatalogService } from '../../../catalog/services/catalog.service';
import { TreatmentsService } from '../../../treatments/services/treatments.service';
import { ClientsService } from '../../services/clients.service';
import { ClientDetailPage } from './client-detail-page';

const carina: Client = {
  id: 7,
  firstName: 'Carina',
  lastName: 'Diaz',
  phone: '3415551234',
  birthDate: '1985-11-03',
  age: 40,
  notes: 'Piel sensible', email: null,
};

describe('ClientDetailPage', () => {
  const clients = { get: vi.fn(), delete: vi.fn(), nameExists: vi.fn(), update: vi.fn(), create: vi.fn() };
  const treatments = { search: vi.fn() };
  const catalog = { getCategories: vi.fn() };
  const params = new BehaviorSubject(convertToParamMap({ id: '7' }));
  const navigate = vi.fn().mockResolvedValue(true);
  let fixture: ComponentFixture<ClientDetailPage>;

  const root = () => fixture.nativeElement as HTMLElement;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ');
  const flush = () => fixture.detectChanges();
  const button = (label: string) =>
    (Array.from(root().querySelectorAll('button')) as HTMLButtonElement[]).find(
      (b) => b.textContent?.trim() === label,
    ) as HTMLButtonElement;
  const dialogButton = (label: string) =>
    (Array.from(root().querySelectorAll('dialog button')) as HTMLButtonElement[]).find(
      (b) => b.textContent?.trim() === label,
    ) as HTMLButtonElement;
  const start = async () => {
    await TestBed.configureTestingModule({
      imports: [ClientDetailPage],
      providers: [
        provideRouter([]),
        { provide: ClientsService, useValue: clients },
        { provide: TreatmentsService, useValue: treatments },
        { provide: CatalogService, useValue: catalog },
        { provide: ActivatedRoute, useValue: { paramMap: params, snapshot: { paramMap: params.value } } },
      ],
    }).compileComponents();
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockImplementation(navigate);
    fixture = TestBed.createComponent(ClientDetailPage);
    flush();
  };

  beforeEach(() => {
    Object.values(clients).forEach((fn) => fn.mockReset());
    navigate.mockClear();
    params.next(convertToParamMap({ id: '7' }));
    clients.get.mockReturnValue(of(carina));
    treatments.search.mockReset();
    treatments.search.mockReturnValue(of({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }));
    catalog.getCategories.mockReset();
    catalog.getCategories.mockReturnValue(of([]));
  });

  it('shows the data of the client with the age under the name and quick contact links', async () => {
    await start();

    expect(clients.get).toHaveBeenCalledWith(7);
    expect(root().querySelector('h1')?.textContent).toContain('Carina Diaz');
    expect(text()).toContain('3415551234');
    expect(text()).toContain('3 de noviembre de 1985');
    expect(root().querySelector('.age')?.textContent).toContain('40 años');
    expect(root().querySelector('.avatar')?.textContent).toBe('CD');
    expect(root().querySelector('a.chip[href="tel:3415551234"]')).not.toBeNull();
    expect(root().querySelector('a.chip[href="https://wa.me/5493415551234"]')).not.toBeNull();
    expect(text()).toContain('Piel sensible');
  });

  it('shows dashes for the data that was never filled in', async () => {
    clients.get.mockReturnValue(of({ ...carina, phone: null, birthDate: null, age: null, notes: null, email: null }));
    await start();

    const values = Array.from(root().querySelectorAll('dd')).map((dd) => dd.textContent?.trim());
    expect(values).toEqual(['—', '—', '—', '—']);
  });

  it('shows the treatment history of this client only', async () => {
    await start();

    expect(text()).toContain('Historial de tratamientos');
    expect(treatments.search).toHaveBeenCalledWith(expect.objectContaining({ clientId: 7 }), 0, 20);
    expect(text()).toContain('Todavía no hay tratamientos registrados para este cliente.');
  });

  it('says so when the client does not exist', async () => {
    clients.get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    await start();

    expect(text()).toContain('Este cliente no existe o ya fue eliminado.');
    expect(root().querySelector('h1')).toBeNull();
  });

  it('shows a retry when the client cannot be loaded', async () => {
    clients.get.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })));
    await start();
    expect(root().querySelector('[role="alert"]')?.textContent).toContain('No pudimos cargar al cliente');

    button('Reintentar').click();
    flush();

    expect(root().querySelector('h1')?.textContent).toContain('Carina Diaz');
  });

  it('edits the client and reloads the record', async () => {
    clients.nameExists.mockReturnValue(of(false));
    clients.update.mockReturnValue(of({}));
    await start();

    button('Editar').click();
    flush();
    expect((root().querySelector('#client-first-name') as HTMLInputElement).value).toBe('Carina');
    clients.get.mockClear();
    root().querySelector('dialog form')?.dispatchEvent(new Event('submit'));
    flush();

    expect(clients.update).toHaveBeenCalledWith(7, expect.objectContaining({ firstName: 'Carina' }));
    expect(root().querySelector('dialog')).toBeNull();
    expect(clients.get).toHaveBeenCalledTimes(1);
  });

  it('deletes the client after confirming and goes back to the list', async () => {
    clients.delete.mockReturnValue(of(undefined));
    await start();

    button('Eliminar').click();
    flush();
    dialogButton('Eliminar').click();
    flush();

    expect(clients.delete).toHaveBeenCalledWith(7);
    expect(navigate).toHaveBeenCalledWith(['/clientes']);
  });

  it('explains why a client with treatments cannot be deleted and stays on the record', async () => {
    clients.delete.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { message: 'El cliente tiene tratamientos registrados. Eliminá esos tratamientos primero' },
          }),
      ),
    );
    await start();

    button('Eliminar').click();
    flush();
    dialogButton('Eliminar').click();
    flush();

    expect(root().querySelector('dialog [role="alert"]')?.textContent).toContain('tratamientos registrados');
    expect(navigate).not.toHaveBeenCalled();
  });
});
