import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Category } from '../../models/catalog.models';
import { CatalogService } from '../../services/catalog.service';
import { CatalogPage } from './catalog-page';

const catalogData = (): Category[] => [
  {
    id: 1,
    name: 'Depilación láser',
    active: true,
    appointmentMinutes: 60,
    services: [
      { id: 10, categoryId: 1, name: 'Bozo', description: null, price: 15000, active: true, bookable: true },
      { id: 11, categoryId: 1, name: 'Axilas', description: null, price: 18000, active: false, bookable: true },
    ],
  },
  { id: 2, name: 'Faciales', active: false, appointmentMinutes: 60, services: [] },
];

describe('CatalogPage', () => {
  const catalog = {
    getCategories: vi.fn(),
    setCategoryActive: vi.fn(),
    setOfferingActive: vi.fn(),
    deleteCategory: vi.fn(),
    deleteOffering: vi.fn(),
    createCategory: vi.fn(),
  };
  let fixture: ComponentFixture<CatalogPage>;

  const root = () => fixture.nativeElement as HTMLElement;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ');
  const flush = () => fixture.detectChanges();
  const button = (label: string) =>
    (Array.from(root().querySelectorAll('button')) as HTMLButtonElement[]).find(
      (b) => b.getAttribute('aria-label') === label || b.textContent?.trim() === label,
    ) as HTMLButtonElement;
  const dialogButton = (label: string) =>
    (Array.from(root().querySelectorAll('dialog button')) as HTMLButtonElement[]).find(
      (b) => b.textContent?.trim() === label,
    ) as HTMLButtonElement;
  const start = async () => {
    await TestBed.configureTestingModule({
      imports: [CatalogPage],
      providers: [{ provide: CatalogService, useValue: catalog }],
    }).compileComponents();
    fixture = TestBed.createComponent(CatalogPage);
    flush();
  };

  beforeEach(() => {
    Object.values(catalog).forEach((fn) => fn.mockReset());
    catalog.getCategories.mockReturnValue(of(catalogData()));
  });

  it('lists the active categories with their active services, hiding the rest', async () => {
    await start();

    expect(text()).toContain('Depilación láser');
    expect(text()).toContain('Bozo');
    expect(text()).not.toContain('Axilas');
    expect(text()).not.toContain('Faciales');
    expect(text()).toContain('1 categoría inactiva oculta');
  });

  it('shows the inactive categories and services when asked to', async () => {
    await start();

    (root().querySelector('.switch input') as HTMLInputElement).click();
    flush();

    expect(text()).toContain('Faciales');
    expect(text()).toContain('Axilas');
    expect(text()).not.toContain('inactiva oculta');
  });

  it('invites the user to create the first category when there are none', async () => {
    catalog.getCategories.mockReturnValue(of([]));
    await start();

    expect(text()).toContain('Todavía no hay categorías');
    expect(root().querySelector('.switch')).toBeNull();
  });

  it('shows a retry when the catalog cannot be loaded', async () => {
    catalog.getCategories.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })));
    await start();
    expect(root().querySelector('[role="alert"]')?.textContent).toContain('No pudimos cargar el catálogo');

    button('Reintentar').click();
    flush();

    expect(text()).toContain('Depilación láser');
    expect(catalog.getCategories).toHaveBeenCalledTimes(2);
  });

  it('collapses a category from its header', async () => {
    await start();
    expect(root().querySelector('table')).not.toBeNull();

    (root().querySelector('.collapse-toggle') as HTMLButtonElement).click();
    flush();

    expect(root().querySelector('table')).toBeNull();
  });

  it('opens the form to create a category', async () => {
    await start();

    button('+ Nueva categoría').click();
    flush();

    expect(root().querySelector('app-category-form-dialog')).not.toBeNull();
    expect(root().querySelector('dialog h2')?.textContent).toContain('Nueva categoría');
  });

  it('opens the form to add a service to a category, already on that category', async () => {
    await start();

    button('Agregar un servicio a Depilación láser').click();
    flush();

    expect(root().querySelector('dialog h2')?.textContent).toContain('Nuevo servicio');
    expect((root().querySelector('#offering-category') as HTMLSelectElement).selectedOptions[0].textContent).toContain(
      'Depilación láser',
    );
  });

  it('turns a service off and reloads the catalog', async () => {
    catalog.setOfferingActive.mockReturnValue(of({}));
    await start();

    button('Desactivar el servicio Bozo').click();
    flush();

    expect(catalog.setOfferingActive).toHaveBeenCalledWith(10, false);
    expect(catalog.getCategories).toHaveBeenCalledTimes(2);
  });

  it('shows the error when turning something off fails', async () => {
    catalog.setCategoryActive.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: { message: 'Error del servidor' } })),
    );
    await start();

    button('Desactivar la categoría Depilación láser').click();
    flush();

    expect(root().querySelector('.form-error')?.textContent).toContain('Error del servidor');
  });

  it('deletes a service after confirming', async () => {
    catalog.deleteOffering.mockReturnValue(of(undefined));
    await start();

    button('Eliminar el servicio Bozo').click();
    flush();
    expect(text()).toContain('¿Eliminar el servicio "Bozo"?');

    dialogButton('Eliminar').click();
    flush();

    expect(catalog.deleteOffering).toHaveBeenCalledWith(10);
    expect(root().querySelector('dialog')).toBeNull();
    expect(catalog.getCategories).toHaveBeenCalledTimes(2);
  });

  it('explains why a used service cannot be deleted and offers to turn it off instead', async () => {
    catalog.deleteOffering.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { message: 'El servicio tiene tratamientos registrados. Desactivalo para que deje de ofrecerse' },
          }),
      ),
    );
    catalog.setOfferingActive.mockReturnValue(of({}));
    await start();

    button('Eliminar el servicio Bozo').click();
    flush();
    dialogButton('Eliminar').click();
    flush();

    expect(root().querySelector('dialog [role="alert"]')?.textContent).toContain('tratamientos registrados');
    dialogButton('Desactivar en su lugar').click();
    flush();

    expect(catalog.setOfferingActive).toHaveBeenCalledWith(10, false);
    expect(root().querySelector('dialog')).toBeNull();
  });

  it('cancels a deletion without touching anything', async () => {
    await start();

    button('Eliminar la categoría Depilación láser').click();
    flush();
    dialogButton('Cancelar').click();
    flush();

    expect(catalog.deleteCategory).not.toHaveBeenCalled();
    expect(root().querySelector('dialog')).toBeNull();
  });
});
