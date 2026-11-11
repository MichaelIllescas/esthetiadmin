import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { todayIso } from '../../../../shared/utils/format';
import { Client } from '../../models/client.models';
import { ClientsService } from '../../services/clients.service';
import { ClientFormDialog } from './client-form-dialog';

@Component({
  imports: [ClientFormDialog],
  template: `<app-client-form-dialog [client]="client()" (saved)="log.push('saved')" (cancelled)="log.push('cancelled')" />`,
})
class Host {
  readonly client = signal<Client | null>(null);
  readonly log: string[] = [];
}

describe('ClientFormDialog', () => {
  const clients = { nameExists: vi.fn(), create: vi.fn(), update: vi.fn() };
  let fixture: ComponentFixture<Host>;

  const field = (id: string) => fixture.nativeElement.querySelector(`#${id}`) as HTMLInputElement;
  const fill = (id: string, value: string) => {
    field(id).value = value;
    field(id).dispatchEvent(new Event('input'));
  };
  const fillRequired = () => {
    fill('client-first-name', 'Carina');
    fill('client-last-name', 'Diaz');
  };
  const submit = () => {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };
  const button = (label: string) =>
    (Array.from(fixture.nativeElement.querySelectorAll('.form-actions button')) as HTMLButtonElement[]).find(
      (b) => b.textContent?.trim() === label,
    ) as HTMLButtonElement;
  const start = async (client: Client | null = null) => {
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [{ provide: ClientsService, useValue: clients }],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    fixture.componentInstance.client.set(client);
    fixture.detectChanges();
  };

  beforeEach(() => {
    Object.values(clients).forEach((fn) => fn.mockReset());
    clients.nameExists.mockReturnValue(of(false));
  });

  it('creates a client, sending empty optional fields as null', async () => {
    clients.create.mockReturnValue(of({}));
    await start();
    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Nuevo cliente');

    fillRequired();
    submit();

    expect(clients.nameExists).toHaveBeenCalledWith('Carina', 'Diaz', null);
    expect(clients.create).toHaveBeenCalledWith({
      firstName: 'Carina',
      lastName: 'Diaz',
      phone: null,
      birthDate: null,
      notes: null, email: null,
    });
    expect(fixture.componentInstance.log).toEqual(['saved']);
  });

  it('sends the optional data when it is filled in', async () => {
    clients.create.mockReturnValue(of({}));
    await start();

    fillRequired();
    fill('client-phone', ' 3415551234 ');
    fill('client-birth-date', '1985-11-03');
    fill('client-notes', 'Piel sensible');
    submit();

    expect(clients.create).toHaveBeenCalledWith({
      firstName: 'Carina',
      lastName: 'Diaz',
      phone: '3415551234',
      birthDate: '1985-11-03',
      notes: 'Piel sensible', email: null,
    });
  });

  it('edits a client starting from its data, without treating itself as a homonym', async () => {
    clients.update.mockReturnValue(of({}));
    await start({ id: 7, firstName: 'Carina', lastName: 'Diaz', phone: '341', birthDate: '1985-11-03', age: 40, notes: null, email: null });
    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Editar cliente');
    expect(field('client-first-name').value).toBe('Carina');
    expect(field('client-birth-date').value).toBe('1985-11-03');

    fill('client-phone', '3415559999');
    submit();

    expect(clients.nameExists).toHaveBeenCalledWith('Carina', 'Diaz', 7);
    expect(clients.update).toHaveBeenCalledWith(7, expect.objectContaining({ phone: '3415559999' }));
  });

  it('asks for the first and last name and does not call the API', async () => {
    await start();

    submit();

    expect(fixture.nativeElement.textContent).toContain('Ingresá el nombre.');
    expect(fixture.nativeElement.textContent).toContain('Ingresá el apellido.');
    expect(clients.nameExists).not.toHaveBeenCalled();
  });

  it('rejects a birth date in the future', async () => {
    await start();

    fillRequired();
    fill('client-birth-date', '2999-01-01');
    submit();

    expect(fixture.nativeElement.textContent).toContain('no puede ser futura');
    expect(clients.create).not.toHaveBeenCalled();
    expect(field('client-birth-date').max).toBe(todayIso());
  });

  it('warns about a homonym and only saves after the user insists', async () => {
    clients.nameExists.mockReturnValue(of(true));
    clients.create.mockReturnValue(of({}));
    await start();

    fillRequired();
    submit();

    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Ya existe un cliente llamado Carina Diaz');
    expect(clients.create).not.toHaveBeenCalled();

    button('Guardar igual').click();
    fixture.detectChanges();

    expect(clients.create).toHaveBeenCalledTimes(1);
    expect(fixture.componentInstance.log).toEqual(['saved']);
  });

  it('lets the user go back to fix the data after the homonym warning', async () => {
    clients.nameExists.mockReturnValue(of(true));
    await start();
    fillRequired();
    submit();

    button('Revisar los datos').click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
    expect(button('Guardar')).toBeTruthy();
  });

  it('drops the warning when the name is changed', async () => {
    clients.nameExists.mockReturnValue(of(true));
    await start();
    fillRequired();
    submit();

    fill('client-last-name', 'Diaz Lopez');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
  });

  it('shows the reason when the API refuses the client and stays open', async () => {
    clients.create.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: { message: 'El teléfono no puede superar los 30 caracteres' } })),
    );
    await start();

    fillRequired();
    submit();

    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('El teléfono no puede superar');
    expect(fixture.componentInstance.log).toEqual([]);
  });

  it('reports cancelled', async () => {
    await start();

    button('Cancelar').click();

    expect(fixture.componentInstance.log).toEqual(['cancelled']);
  });
});
