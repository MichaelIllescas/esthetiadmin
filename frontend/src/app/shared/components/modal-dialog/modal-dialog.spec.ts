import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ModalDialog } from './modal-dialog';

@Component({
  imports: [ModalDialog],
  template: `<app-modal-dialog heading="Nueva categoría" (closed)="closedCount = closedCount + 1"><p>Contenido</p></app-modal-dialog>`,
})
class Host {
  closedCount = 0;
}

describe('ModalDialog', () => {
  let fixture: ComponentFixture<Host>;

  const dialog = () => fixture.nativeElement.querySelector('dialog') as HTMLDialogElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Host] }).compileComponents();
    fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
  });

  it('opens with its heading and projected content, labelled for screen readers', () => {
    expect(dialog().hasAttribute('open')).toBe(true);
    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Nueva categoría');
    expect(fixture.nativeElement.textContent).toContain('Contenido');
    const headingId = fixture.nativeElement.querySelector('h2').id;
    expect(dialog().getAttribute('aria-labelledby')).toBe(headingId);
  });

  it('emits closed from the close button', () => {
    (fixture.nativeElement.querySelector('.dialog-close') as HTMLButtonElement).click();

    expect(fixture.componentInstance.closedCount).toBe(1);
  });

  it('emits closed when the user presses Escape (native cancel event)', () => {
    const event = new Event('cancel', { cancelable: true });
    dialog().dispatchEvent(event);

    expect(event.defaultPrevented).toBe(true);
    expect(fixture.componentInstance.closedCount).toBe(1);
  });

  it('emits closed when the backdrop is clicked, but not when the content is', () => {
    (fixture.nativeElement.querySelector('.dialog-body') as HTMLElement).click();
    expect(fixture.componentInstance.closedCount).toBe(0);

    dialog().click();
    expect(fixture.componentInstance.closedCount).toBe(1);
  });
});
