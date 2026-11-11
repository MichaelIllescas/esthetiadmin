import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Birthday } from '../../models/client.models';
import { ClientsService } from '../../services/clients.service';
import { BirthdaysPanel } from './birthdays-panel';

const birthdays: Birthday[] = [
  { id: 1, firstName: 'Ana', lastName: 'Lopez', birthDate: '1990-09-25', nextBirthday: '2026-09-25', turningAge: 36, daysUntil: 0 },
  { id: 2, firstName: 'Carina', lastName: 'Diaz', birthDate: '1985-09-26', nextBirthday: '2026-09-26', turningAge: 41, daysUntil: 1 },
  { id: 3, firstName: 'Rosa', lastName: 'Funes', birthDate: '1970-11-03', nextBirthday: '2026-11-03', turningAge: 56, daysUntil: 39 },
];

@Component({
  imports: [BirthdaysPanel],
  template: `<app-birthdays-panel [refreshToken]="token()" />`,
})
class Host {
  readonly token = signal(0);
}

describe('BirthdaysPanel', () => {
  const clients = { birthdays: vi.fn() };
  let fixture: ComponentFixture<Host>;

  const text = () => (fixture.nativeElement.textContent as string).replace(/\s+/g, ' ');
  const start = async () => {
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [provideRouter([]), { provide: ClientsService, useValue: clients }],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
  };

  beforeEach(() => {
    clients.birthdays.mockReset();
    clients.birthdays.mockReturnValue(of(birthdays));
  });

  it('lists who is about to have a birthday, with a friendly countdown', async () => {
    await start();

    expect(clients.birthdays).toHaveBeenCalledWith(30);
    expect(text()).toContain('Lopez, Ana');
    expect(text()).toContain('25 de septiembre · cumple 36');
    expect(text()).toContain('Hoy');
    expect(text()).toContain('Mañana');
    expect(text()).toContain('En 39 días');
    expect(fixture.nativeElement.querySelector('.count').textContent).toContain('3');
  });

  it('links every birthday to the client record', async () => {
    await start();

    const links = Array.from(fixture.nativeElement.querySelectorAll('a')) as HTMLAnchorElement[];
    expect(links.map((a) => a.getAttribute('href'))).toEqual(['/clientes/1', '/clientes/2', '/clientes/3']);
  });

  it('says so when nobody has a birthday soon', async () => {
    clients.birthdays.mockReturnValue(of([]));
    await start();

    expect(text()).toContain('Nadie cumple años en los próximos 30 días.');
    expect(fixture.nativeElement.querySelector('.count')).toBeNull();
  });

  it('can be collapsed', async () => {
    await start();
    const toggle = fixture.nativeElement.querySelector('.panel-toggle') as HTMLButtonElement;
    expect(toggle.getAttribute('aria-expanded')).toBe('true');

    toggle.click();
    fixture.detectChanges();

    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    expect(fixture.nativeElement.querySelector('ul')).toBeNull();
  });

  it('shows an error with a retry', async () => {
    clients.birthdays.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })));
    await start();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('No pudimos cargar');

    (fixture.nativeElement.querySelector('.retry') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(text()).toContain('Lopez, Ana');
  });

  it('reloads when told the data changed', async () => {
    await start();
    expect(clients.birthdays).toHaveBeenCalledTimes(1);

    fixture.componentInstance.token.set(1);
    fixture.detectChanges();

    expect(clients.birthdays).toHaveBeenCalledTimes(2);
  });
});
