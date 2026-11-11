import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BookableCategory, ChosenService } from '../../models/booking.models';
import { ServicePicker } from './service-picker';

@Component({
  imports: [ServicePicker],
  template: `<app-service-picker [categories]="categories()" (chosen)="log.push($event)" />`,
})
class Host {
  readonly categories = signal<BookableCategory[]>([]);
  readonly log: ChosenService[] = [];
}

const waxing: BookableCategory = {
  categoryId: 1, categoryName: 'Depilación', durationMinutes: 30,
  services: [{ id: 10, name: 'Bozo', description: 'Zona del rostro' }, { id: 11, name: 'Axilas', description: null }],
};
const massages: BookableCategory = {
  categoryId: 2, categoryName: 'Masajes', durationMinutes: 60,
  services: [{ id: 20, name: 'Descontracturante', description: null }],
};

describe('ServicePicker', () => {
  let fixture: ComponentFixture<Host>;

  const root = () => fixture.nativeElement as HTMLElement;
  const toggle = (name: string) => {
    (Array.from(root().querySelectorAll<HTMLButtonElement>('button.toggle')).find((button) =>
      button.textContent?.includes(name)) as HTMLButtonElement).click();
    fixture.detectChanges();
  };
  const services = () => Array.from(root().querySelectorAll('button.service')).map((button) => button.querySelector('.name')?.textContent);
  const start = async (categories: BookableCategory[]) => {
    await TestBed.configureTestingModule({ imports: [Host] }).compileComponents();
    fixture = TestBed.createComponent(Host);
    fixture.componentInstance.categories.set(categories);
    fixture.detectChanges();
  };

  it('shows only the categories, with how many services and how long their appointments last', async () => {
    await start([waxing, massages]);

    expect(services()).toEqual([]);
    expect(root().textContent).toContain('2 servicios · turnos de 30 minutos');
    expect(root().textContent).toContain('1 servicio · turnos de 1 hora');
    expect(root().querySelector('button.toggle')?.getAttribute('aria-expanded')).toBe('false');
  });

  it('opens one category at a time and emits the chosen service', async () => {
    await start([waxing, massages]);

    toggle('Depilación');
    expect(services()).toEqual(['Bozo', 'Axilas']);
    expect(root().textContent).toContain('Zona del rostro');

    toggle('Masajes');
    expect(services()).toEqual(['Descontracturante']);

    (root().querySelector('button.service') as HTMLButtonElement).click();
    expect(fixture.componentInstance.log).toEqual([
      { id: 20, name: 'Descontracturante', description: null, categoryName: 'Masajes', durationMinutes: 60 },
    ]);
  });

  it('closes the open category when tapped again', async () => {
    await start([waxing, massages]);

    toggle('Depilación');
    toggle('Depilación');

    expect(services()).toEqual([]);
  });

  it('starts open when there is a single category', async () => {
    await start([waxing]);

    expect(services()).toEqual(['Bozo', 'Axilas']);
  });

  it('explains when nothing can be booked online', async () => {
    await start([]);

    expect(root().textContent).toContain('Por ahora no hay servicios para reservar online');
  });
});
