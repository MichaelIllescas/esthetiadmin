import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Category, Offering } from '../../models/catalog.models';
import { CategoryBlock } from './category-block';

const bozo: Offering = { id: 10, categoryId: 1, name: 'Bozo', description: 'Zona del rostro', price: 15000, active: true, bookable: true };
const axilas: Offering = { id: 11, categoryId: 1, name: 'Axilas', description: null, price: 18000, active: false, bookable: true };

@Component({
  imports: [CategoryBlock],
  template: `
    <app-category-block
      [category]="category()"
      [showInactive]="showInactive()"
      [collapsed]="collapsed()"
      (toggleCollapse)="log.push('collapse')"
      (edit)="log.push('edit')"
      (toggleActive)="log.push('toggle')"
      (delete)="log.push('delete')"
      (addOffering)="log.push('add')"
      (editOffering)="log.push('edit-offering:' + $event.name)"
      (toggleOfferingActive)="log.push('toggle-offering:' + $event.name)"
      (deleteOffering)="log.push('delete-offering:' + $event.name)"
    />
  `,
})
class Host {
  readonly category = signal<Category>({ id: 1, name: 'Depilación láser', active: true, appointmentMinutes: 60, services: [bozo, axilas] });
  readonly showInactive = signal(false);
  readonly collapsed = signal(false);
  readonly log: string[] = [];
}

describe('CategoryBlock', () => {
  let fixture: ComponentFixture<Host>;
  const host = () => fixture.componentInstance;
  const text = () => (fixture.nativeElement as HTMLElement).textContent ?? '';
  const button = (label: string) =>
    (Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[]).find((b) =>
      b.getAttribute('aria-label')?.includes(label),
    ) as HTMLButtonElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Host] }).compileComponents();
    fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
  });

  it('shows the active services with their formatted price and hides the inactive ones', () => {
    expect(text()).toContain('Depilación láser');
    expect(text()).toContain('Bozo');
    expect(text().replace(/\s/g, ' ')).toContain('$ 15.000');
    expect(text()).toContain('Zona del rostro');
    expect(text()).not.toContain('Axilas');
  });

  it('shows the inactive services, marked, when asked to', () => {
    host().showInactive.set(true);
    fixture.detectChanges();

    expect(text()).toContain('Axilas');
    expect(text()).toContain('Inactivo');
    expect(text()).toContain('—');
  });

  it('warns that an inactive category is not offered and marks it', () => {
    host().category.set({ id: 1, name: 'Faciales', active: false, appointmentMinutes: 60, services: [] });
    fixture.detectChanges();

    expect(text()).toContain('Inactiva');
    expect(text()).toContain('no se ofrecen');
    expect(button('Activar la categoría')).toBeTruthy();
  });

  it('says so when there is nothing to list', () => {
    host().category.set({ id: 1, name: 'Masajes', active: true, appointmentMinutes: 60, services: [] });
    fixture.detectChanges();

    expect(text()).toContain('todavía no tiene servicios activos');
  });

  it('hides the body when collapsed and exposes it to assistive technology', () => {
    const toggle = fixture.nativeElement.querySelector('.collapse-toggle') as HTMLButtonElement;
    expect(toggle.getAttribute('aria-expanded')).toBe('true');

    host().collapsed.set(true);
    fixture.detectChanges();

    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
  });

  it('reports every action with the item it refers to', () => {
    (fixture.nativeElement.querySelector('.collapse-toggle') as HTMLButtonElement).click();
    button('Editar la categoría').click();
    button('Desactivar la categoría').click();
    button('Eliminar la categoría').click();
    button('Agregar un servicio').click();
    button('Editar el servicio Bozo').click();
    button('Desactivar el servicio Bozo').click();
    button('Eliminar el servicio Bozo').click();

    expect(host().log).toEqual([
      'collapse',
      'edit',
      'toggle',
      'delete',
      'add',
      'edit-offering:Bozo',
      'toggle-offering:Bozo',
      'delete-offering:Bozo',
    ]);
  });
});
