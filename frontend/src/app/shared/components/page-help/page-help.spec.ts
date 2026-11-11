import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { PageHelp } from './page-help';

@Component({
  imports: [PageHelp],
  template: `
    <app-page-help><ul><li>Primer paso</li></ul></app-page-help>
    <app-page-help summary="¿Cómo funciona la agenda?"><p>Otro</p></app-page-help>
  `,
})
class Host {}

describe('PageHelp', () => {
  it('starts closed with its summary and shows the projected explanation', async () => {
    await TestBed.configureTestingModule({ imports: [Host] }).compileComponents();
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const boxes = root.querySelectorAll('details');

    expect(boxes[0].open).toBe(false);
    expect(boxes[0].querySelector('summary')?.textContent).toContain('¿Cómo funciona esta sección?');
    expect(boxes[0].textContent).toContain('Primer paso');
    expect(boxes[1].querySelector('summary')?.textContent).toContain('¿Cómo funciona la agenda?');
  });
});
