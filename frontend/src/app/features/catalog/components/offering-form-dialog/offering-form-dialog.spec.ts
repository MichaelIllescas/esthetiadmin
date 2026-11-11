import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Category, Offering } from '../../models/catalog.models';
import { CatalogService } from '../../services/catalog.service';
import { OfferingFormDialog } from './offering-form-dialog';

const categories: Category[] = [
  { id: 1, name: 'Depilación láser', active: true, appointmentMinutes: 60, services: [] },
  { id: 2, name: 'Faciales', active: false, appointmentMinutes: 60, services: [] },
];

@Component({
  imports: [OfferingFormDialog],
  template: `
    <app-offering-form-dialog
      [categories]="categories"
      [offering]="offering()"
      [defaultCategoryId]="1"
      (saved)="log.push('saved')"
      (cancelled)="log.push('cancelled')"
    />
  `,
})
class Host {
  readonly categories = categories;
  readonly offering = signal<Offering | null>(null);
  readonly log: string[] = [];
}

describe('OfferingFormDialog', () => {
  const catalog = { createOffering: vi.fn(), updateOffering: vi.fn() };
  let fixture: ComponentFixture<Host>;

  const field = (id: string) => fixture.nativeElement.querySelector(`#${id}`) as HTMLInputElement;
  const fill = (id: string, value: string) => {
    field(id).value = value;
    field(id).dispatchEvent(new Event(id === 'offering-category' ? 'change' : 'input'));
  };
  const submit = () => {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };
  const start = async (offering: Offering | null = null) => {
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [{ provide: CatalogService, useValue: catalog }],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    fixture.componentInstance.offering.set(offering);
    fixture.detectChanges();
  };

  beforeEach(() => {
    catalog.createOffering.mockReset();
    catalog.updateOffering.mockReset();
  });

  it('starts on the default category and lists every category', async () => {
    await start();

    const options = Array.from(fixture.nativeElement.querySelectorAll('option')).map((o) =>
      (o as HTMLElement).textContent?.trim(),
    );
    expect(options).toEqual(['Elegí una categoría', 'Depilación láser', 'Faciales (inactiva)']);
    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Nuevo servicio');
  });

  it('creates a service with a numeric price and no description when it is blank', async () => {
    catalog.createOffering.mockReturnValue(of({}));
    await start();

    fill('offering-name', '  Bozo ');
    fill('offering-description', '   ');
    fill('offering-price', '15000');
    submit();

    expect(catalog.createOffering).toHaveBeenCalledWith({
      categoryId: 1,
      name: '  Bozo ',
      description: null,
      price: 15000,
      bookable: true,
    });
    expect(fixture.componentInstance.log).toEqual(['saved']);
  });

  it('edits an existing service, starting from its data', async () => {
    catalog.updateOffering.mockReturnValue(of({}));
    await start({ id: 10, categoryId: 2, name: 'Peeling', description: 'Para el rostro', price: 30000, active: true, bookable: true });
    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Editar servicio');
    expect(field('offering-name').value).toBe('Peeling');
    expect(field('offering-price').value).toBe('30000');

    fill('offering-price', '32000');
    submit();

    expect(catalog.updateOffering).toHaveBeenCalledWith(10, {
      categoryId: 2,
      name: 'Peeling',
      description: 'Para el rostro',
      price: 32000,
      bookable: true,
    });
  });

  it('can take a service off the public booking page', async () => {
    catalog.updateOffering.mockReturnValue(of({}));
    await start({ id: 10, categoryId: 2, name: 'Peeling', description: null, price: 30000, active: true, bookable: true });
    const bookable = fixture.nativeElement.querySelector('input[role="switch"]') as HTMLInputElement;
    expect(bookable.checked).toBe(true);

    bookable.click();
    submit();

    expect(catalog.updateOffering).toHaveBeenCalledWith(10, expect.objectContaining({ bookable: false }));
  });

  it('asks for the missing fields and does not send anything', async () => {
    await start();

    submit();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Ingresá el nombre del servicio.');
    expect(text).toContain('Ingresá un precio válido');
    expect(catalog.createOffering).not.toHaveBeenCalled();
  });

  it('rejects a negative price', async () => {
    await start();

    fill('offering-name', 'Bozo');
    fill('offering-price', '-5');
    submit();

    expect(fixture.nativeElement.textContent).toContain('Ingresá un precio válido');
    expect(catalog.createOffering).not.toHaveBeenCalled();
  });

  it('shows the reason when the API refuses the service and stays open', async () => {
    catalog.createOffering.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { message: 'Ya existe un servicio con ese nombre en esta categoría' } })),
    );
    await start();

    fill('offering-name', 'Bozo');
    fill('offering-price', '15000');
    submit();

    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Ya existe un servicio');
    expect(fixture.componentInstance.log).toEqual([]);
  });
});
