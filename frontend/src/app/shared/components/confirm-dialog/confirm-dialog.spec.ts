import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmDialog } from './confirm-dialog';

@Component({
  imports: [ConfirmDialog],
  template: `
    <app-confirm-dialog
      heading="Eliminar servicio"
      message="¿Eliminar Bozo?"
      [errorMessage]="error()"
      [alternativeLabel]="alternative()"
      [busy]="busy()"
      (confirmed)="log.push('confirmed')"
      (cancelled)="log.push('cancelled')"
      (alternative)="log.push('alternative')"
    />
  `,
})
class Host {
  readonly error = signal<string | null>(null);
  readonly alternative = signal<string | null>('Desactivar');
  readonly busy = signal(false);
  readonly log: string[] = [];
}

describe('ConfirmDialog', () => {
  let fixture: ComponentFixture<Host>;
  const buttons = () => Array.from(fixture.nativeElement.querySelectorAll('.form-actions button')) as HTMLButtonElement[];
  const labels = () => buttons().map((button) => button.textContent?.trim());

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Host] }).compileComponents();
    fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
  });

  it('shows the question with cancel and confirm actions', () => {
    expect(fixture.nativeElement.textContent).toContain('¿Eliminar Bozo?');
    expect(labels()).toEqual(['Cancelar', 'Eliminar']);
  });

  it('emits confirmed and cancelled', () => {
    buttons()[1].click();
    buttons()[0].click();

    expect(fixture.componentInstance.log).toEqual(['confirmed', 'cancelled']);
  });

  it('shows the refusal reason and offers the alternative only after an error', () => {
    fixture.componentInstance.error.set('El servicio tiene tratamientos registrados');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('tratamientos registrados');
    expect(labels()).toEqual(['Cancelar', 'Desactivar', 'Eliminar']);

    buttons()[1].click();
    expect(fixture.componentInstance.log).toEqual(['alternative']);
  });

  it('disables the actions while working', () => {
    fixture.componentInstance.busy.set(true);
    fixture.detectChanges();

    expect(buttons()[1].disabled).toBe(true);
    expect(labels()[1]).toBe('Procesando...');
  });
});
