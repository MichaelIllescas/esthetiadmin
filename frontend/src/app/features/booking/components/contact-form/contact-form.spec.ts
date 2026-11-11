import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Contact, ContactForm } from './contact-form';

@Component({
  imports: [ContactForm],
  template: `<app-contact-form [busy]="busy()" (submitted)="log.push($event)" />`,
})
class Host {
  readonly busy = signal(false);
  readonly log: Contact[] = [];
}

describe('ContactForm', () => {
  let fixture: ComponentFixture<Host>;

  const root = () => fixture.nativeElement as HTMLElement;
  const fill = (id: string, value: string) => {
    const input = root().querySelector(`#${id}`) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  };
  const submit = () => {
    root().querySelector('form')?.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Host] }).compileComponents();
    fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
  });

  it('sends the trimmed data', () => {
    fill('booking-first-name', ' Carina ');
    fill('booking-last-name', 'Diaz');
    fill('booking-phone', '341 555-1234');
    submit();

    expect(fixture.componentInstance.log).toEqual([{ firstName: 'Carina', lastName: 'Diaz', phone: '341 555-1234', email: null }]);
  });

  it('asks for every field and a phone with enough digits', () => {
    fill('booking-phone', '555-12');
    submit();

    const text = root().textContent;
    expect(text).toContain('Ingresá tu nombre.');
    expect(text).toContain('Ingresá tu apellido.');
    expect(text).toContain('Ingresá un teléfono de 8 a 15 números.');
    expect(fixture.componentInstance.log).toEqual([]);
  });

  it('disables the button while booking', () => {
    fixture.componentInstance.busy.set(true);
    fixture.detectChanges();

    const button = root().querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    expect(button.textContent).toContain('Reservando...');
  });
});
