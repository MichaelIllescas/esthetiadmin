import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Page } from '../../../../shared/models/page.model';
import { Client } from '../../models/client.models';
import { ClientsService } from '../../services/clients.service';
import { ClientsPage } from './clients-page';

const carina: Client = { id: 1, firstName: 'Carina', lastName: 'Diaz', phone: '3415551234', birthDate: '1985-11-03', age: 40, notes: null, email: null };
const rosa: Client = { id: 2, firstName: 'Rosa', lastName: 'Funes', phone: null, birthDate: null, age: null, notes: null, email: null };

const page = (content: Client[], index = 0, totalPages = 1, total = content.length): Page<Client> => ({
  content,
  page: index,
  size: 20,
  totalElements: total,
  totalPages,
});

describe('ClientsPage', () => {
  const clients = {
    search: vi.fn(),
    delete: vi.fn(),
    birthdays: vi.fn(),
    nameExists: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
  };
  let fixture: ComponentFixture<ClientsPage>;

  const root = () => fixture.nativeElement as HTMLElement;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ');
  const flush = () => fixture.detectChanges();
  const buttonByLabel = (label: string) =>
    (Array.from(root().querySelectorAll('button')) as HTMLButtonElement[]).find(
      (b) => b.getAttribute('aria-label') === label,
    ) as HTMLButtonElement;
  const buttonByText = (label: string) =>
    (Array.from(root().querySelectorAll('button')) as HTMLButtonElement[]).find(
      (b) => b.textContent?.trim() === label,
    ) as HTMLButtonElement;
  const dialogButton = (label: string) =>
    (Array.from(root().querySelectorAll('dialog button')) as HTMLButtonElement[]).find(
      (b) => b.textContent?.trim() === label,
    ) as HTMLButtonElement;
  const type = (value: string) => {
    const input = root().querySelector('#client-search') as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  };
  const start = async () => {
    await TestBed.configureTestingModule({
      imports: [ClientsPage],
      providers: [provideRouter([]), { provide: ClientsService, useValue: clients }],
    }).compileComponents();
    fixture = TestBed.createComponent(ClientsPage);
    flush();
  };

  beforeEach(() => {
    vi.useFakeTimers();
    Object.values(clients).forEach((fn) => fn.mockReset());
    clients.search.mockReturnValue(of(page([carina, rosa])));
    clients.birthdays.mockReturnValue(of([]));
  });

  afterEach(() => vi.useRealTimers());

  it('lists the clients, with a dash when the phone or the age is missing', async () => {
    await start();

    expect(clients.search).toHaveBeenCalledWith('', 0, 20);
    expect(text()).toContain('Diaz, Carina');
    expect(text()).toContain('3415551234');
    expect(text()).toContain('Funes, Rosa');
    expect(text()).toContain('2 clientes · Página 1 de 1');
    const links = Array.from(root().querySelectorAll('.client-link')) as HTMLAnchorElement[];
    expect(links.map((a) => a.getAttribute('href'))).toEqual(['/clientes/1', '/clientes/2']);
  });

  it('searches after the user stops typing, once, and goes back to the first page', async () => {
    await start();
    clients.search.mockClear();

    type('c');
    type('ca');
    type('carina');
    vi.advanceTimersByTime(299);
    expect(clients.search).not.toHaveBeenCalled();

    vi.advanceTimersByTime(1);
    flush();

    expect(clients.search).toHaveBeenCalledTimes(1);
    expect(clients.search).toHaveBeenCalledWith('carina', 0, 20);
  });

  it('says so when a search finds nobody', async () => {
    await start();
    clients.search.mockReturnValue(of(page([], 0, 0, 0)));

    type('zzz');
    vi.advanceTimersByTime(300);
    flush();

    expect(text()).toContain('No encontramos clientes para "zzz".');
  });

  it('invites the user to register the first client when there are none', async () => {
    clients.search.mockReturnValue(of(page([], 0, 0, 0)));
    await start();

    expect(text()).toContain('Todavía no cargaste clientes');
  });

  it('shows a retry when the list cannot be loaded', async () => {
    clients.search.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })));
    await start();
    expect(root().querySelector('.status-error')?.textContent).toContain('No pudimos cargar los clientes');

    buttonByText('Reintentar').click();
    flush();

    expect(text()).toContain('Diaz, Carina');
  });

  it('moves between pages', async () => {
    clients.search.mockReturnValue(of(page([carina], 0, 3, 45)));
    await start();
    expect(text()).toContain('45 clientes · Página 1 de 3');
    expect(buttonByText('Anterior').disabled).toBe(true);

    clients.search.mockReturnValue(of(page([rosa], 1, 3, 45)));
    buttonByText('Siguiente').click();
    flush();

    expect(clients.search).toHaveBeenLastCalledWith('', 1, 20);
    expect(text()).toContain('Página 2 de 3');
    expect(buttonByText('Anterior').disabled).toBe(false);
  });

  it('opens the form to register a client', async () => {
    await start();

    buttonByText('+ Nuevo cliente').click();
    flush();

    expect(root().querySelector('dialog h2')?.textContent).toContain('Nuevo cliente');
  });

  it('opens the form to edit the chosen client, already filled in', async () => {
    await start();

    buttonByLabel('Editar a Carina Diaz').click();
    flush();

    expect(root().querySelector('dialog h2')?.textContent).toContain('Editar cliente');
    expect((root().querySelector('#client-first-name') as HTMLInputElement).value).toBe('Carina');
  });

  it('reloads the list and the birthdays after saving', async () => {
    clients.nameExists.mockReturnValue(of(false));
    clients.create.mockReturnValue(of({}));
    await start();
    buttonByText('+ Nuevo cliente').click();
    flush();
    const set = (id: string, value: string) => {
      const input = root().querySelector(`#${id}`) as HTMLInputElement;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    };
    set('client-first-name', 'Ana');
    set('client-last-name', 'Lopez');
    clients.search.mockClear();
    clients.birthdays.mockClear();

    root().querySelector('dialog form')?.dispatchEvent(new Event('submit'));
    flush();

    expect(clients.create).toHaveBeenCalledTimes(1);
    expect(root().querySelector('dialog')).toBeNull();
    expect(clients.search).toHaveBeenCalledTimes(1);
    expect(clients.birthdays).toHaveBeenCalledTimes(1);
  });

  it('deletes a client after confirming', async () => {
    clients.delete.mockReturnValue(of(undefined));
    await start();

    buttonByLabel('Eliminar a Carina Diaz').click();
    flush();
    expect(text()).toContain('¿Eliminar a Carina Diaz?');
    dialogButton('Eliminar').click();
    flush();

    expect(clients.delete).toHaveBeenCalledWith(1);
    expect(root().querySelector('dialog')).toBeNull();
  });

  it('explains why a client with treatments cannot be deleted', async () => {
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

    buttonByLabel('Eliminar a Carina Diaz').click();
    flush();
    dialogButton('Eliminar').click();
    flush();

    expect(root().querySelector('dialog [role="alert"]')?.textContent).toContain('tratamientos registrados');
    expect(dialogButton('Eliminar')).toBeTruthy();
    expect(root().querySelector('dialog')).not.toBeNull();
  });

  it('goes back one page when the last client of the last page is deleted', async () => {
    clients.search.mockReturnValue(of(page([rosa], 0, 2, 21)));
    clients.delete.mockReturnValue(of(undefined));
    await start();

    // Second and last page, holding a single client.
    clients.search.mockReturnValue(of(page([carina], 1, 2, 21)));
    buttonByText('Siguiente').click();
    flush();
    expect(text()).toContain('Página 2 de 2');

    buttonByLabel('Eliminar a Carina Diaz').click();
    flush();
    clients.search.mockClear();
    dialogButton('Eliminar').click();
    flush();

    expect(clients.search).toHaveBeenLastCalledWith('', 0, 20);
  });
});
