import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { Client } from '../../models/client.models';
import { ClientsService } from '../../services/clients.service';
import { ClientPicker } from './client-picker';

const carina: Client = { id: 1, firstName: 'Carina', lastName: 'Diaz', phone: '3415551234', birthDate: null, age: null, notes: null, email: null };
const rosa: Client = { id: 2, firstName: 'Rosa', lastName: 'Funes', phone: null, birthDate: null, age: null, notes: null, email: null };
const page = (content: Client[]) => ({ content, page: 0, size: 8, totalElements: content.length, totalPages: 1 });

@Component({
  imports: [ClientPicker],
  template: `<app-client-picker label="Cliente" (picked)="log.push($event ? $event.id : null)" />`,
})
class Host {
  readonly log: Array<number | null> = [];
}

describe('ClientPicker', () => {
  const clients = { search: vi.fn() };
  let fixture: ComponentFixture<Host>;

  const input = () => fixture.nativeElement.querySelector('input') as HTMLInputElement;
  const options = () => Array.from(fixture.nativeElement.querySelectorAll('[role="option"]')) as HTMLElement[];
  const type = (value: string) => {
    input().value = value;
    input().dispatchEvent(new Event('input'));
    vi.advanceTimersByTime(250);
    fixture.detectChanges();
  };
  const key = (name: string) => {
    input().dispatchEvent(new KeyboardEvent('keydown', { key: name, bubbles: true, cancelable: true }));
    fixture.detectChanges();
  };

  beforeEach(async () => {
    vi.useFakeTimers();
    clients.search.mockReset();
    clients.search.mockReturnValue(of(page([carina, rosa])));
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [{ provide: ClientsService, useValue: clients }],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
  });

  afterEach(() => vi.useRealTimers());

  it('is an accessible combobox that starts closed', () => {
    expect(input().getAttribute('role')).toBe('combobox');
    expect(input().getAttribute('aria-expanded')).toBe('false');
    expect(fixture.nativeElement.querySelector('label').textContent).toContain('Cliente');
    expect(options()).toEqual([]);
  });

  it('searches once the user stops typing and lists the matches', () => {
    type('c');
    type('ca');
    expect(clients.search).toHaveBeenCalledTimes(2);

    expect(clients.search).toHaveBeenLastCalledWith('ca', 0, 8);
    expect(options().map((o) => o.querySelector('.option-name')?.textContent?.trim())).toEqual(['Diaz, Carina', 'Funes, Rosa']);
    expect(options()[0].querySelector('.option-detail')?.textContent?.trim()).toBe('3415551234');
    expect(options()[1].querySelector('.option-detail')).toBeNull();
    expect(input().getAttribute('aria-expanded')).toBe('true');
  });

  it('chooses a client with the mouse and shows it with a button to change it', () => {
    type('ca');

    options()[1].dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(fixture.componentInstance.log).toEqual([2]);
    expect(fixture.nativeElement.querySelector('.chosen-name').textContent).toContain('Funes, Rosa');
    expect(fixture.nativeElement.querySelector('input')).toBeNull();
  });

  it('lets the keyboard move through the list and choose', () => {
    type('ca');
    expect(options()[0].getAttribute('aria-selected')).toBe('true');

    key('ArrowDown');
    expect(options()[1].getAttribute('aria-selected')).toBe('true');
    expect(input().getAttribute('aria-activedescendant')).toBe(options()[1].id);

    key('ArrowDown');
    expect(options()[0].getAttribute('aria-selected')).toBe('true');
    key('ArrowUp');
    expect(options()[1].getAttribute('aria-selected')).toBe('true');

    key('Enter');
    expect(fixture.componentInstance.log).toEqual([2]);
  });

  it('closes the list with Escape without letting the dialog around it close too', () => {
    type('ca');
    const event = new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true });
    const stop = vi.spyOn(event, 'stopPropagation');

    input().dispatchEvent(event);
    fixture.detectChanges();

    expect(stop).toHaveBeenCalled();
    expect(input().getAttribute('aria-expanded')).toBe('false');
  });

  it('does not reserve any space under the field, so it sits as close to the next field as the others', () => {
    expect(fixture.nativeElement.querySelector('.hint')).toBeNull();
    expect(fixture.nativeElement.querySelector('.panel')).toBeNull();
  });

  it('shows that it is searching inside the dropdown while the answer arrives', () => {
    const answer = new Subject<ReturnType<typeof page>>();
    clients.search.mockReturnValue(answer);

    type('ca');
    expect(fixture.nativeElement.querySelector('.panel .panel-message').textContent).toContain('Buscando...');

    answer.next(page([carina]));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.panel .panel-message')).toBeNull();
    expect(options()).toHaveLength(1);
  });

  it('says so inside the dropdown when nobody matches, without leaving an empty list behind', () => {
    clients.search.mockReturnValue(of(page([])));

    type('zzz');

    const message = fixture.nativeElement.querySelector('.panel .panel-message') as HTMLElement;
    expect(message.textContent).toContain('No encontramos clientes con ese nombre.');
    expect(fixture.nativeElement.querySelector('[role="listbox"]')).toBeNull();
    expect(input().getAttribute('aria-expanded')).toBe('false');
  });

  it('warns inside the dropdown when the search fails', () => {
    clients.search.mockReturnValue(throwError(() => new Error('boom')));

    type('ca');

    const message = fixture.nativeElement.querySelector('.panel .panel-message') as HTMLElement;
    expect(message.textContent).toContain('No pudimos buscar clientes');
    expect(message.getAttribute('role')).toBe('alert');
  });

  it('closes the message when the user clears what they typed', () => {
    clients.search.mockReturnValue(of(page([])));
    type('zzz');
    expect(fixture.nativeElement.querySelector('.panel')).not.toBeNull();

    type('');

    expect(fixture.nativeElement.querySelector('.panel')).toBeNull();
  });

  it('lets the user change the chosen client and tells the parent', () => {
    type('ca');
    options()[0].dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.chosen button') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(fixture.componentInstance.log).toEqual([1, null]);
    expect(fixture.nativeElement.querySelector('input')).not.toBeNull();
  });
});
