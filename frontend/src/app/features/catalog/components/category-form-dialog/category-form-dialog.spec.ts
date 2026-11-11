import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Category } from '../../models/catalog.models';
import { CatalogService } from '../../services/catalog.service';
import { CategoryFormDialog } from './category-form-dialog';

@Component({
  imports: [CategoryFormDialog],
  template: `<app-category-form-dialog [category]="category()" (saved)="log.push('saved')" (cancelled)="log.push('cancelled')" />`,
})
class Host {
  readonly category = signal<Category | null>(null);
  readonly log: string[] = [];
}

describe('CategoryFormDialog', () => {
  const catalog = { createCategory: vi.fn(), updateCategory: vi.fn() };
  let fixture: ComponentFixture<Host>;

  const input = () => fixture.nativeElement.querySelector('#category-name') as HTMLInputElement;
  const type = (value: string) => {
    input().value = value;
    input().dispatchEvent(new Event('input'));
  };
  const duration = () => fixture.nativeElement.querySelector('#category-duration') as HTMLSelectElement;
  const chooseDuration = (label: string) => {
    const select = duration();
    select.selectedIndex = [...select.options].findIndex((option) => option.textContent?.trim() === label);
    select.dispatchEvent(new Event('change'));
  };
  const submit = () => {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };
  const start = async (category: Category | null = null) => {
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [{ provide: CatalogService, useValue: catalog }],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    fixture.componentInstance.category.set(category);
    fixture.detectChanges();
  };

  beforeEach(() => {
    catalog.createCategory.mockReset();
    catalog.updateCategory.mockReset();
  });

  it('creates a category and reports it as saved', async () => {
    catalog.createCategory.mockReturnValue(of({}));
    await start();
    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Nueva categoría');

    type('Masajes');
    submit();

    expect(catalog.createCategory).toHaveBeenCalledWith({ name: 'Masajes', appointmentMinutes: 60 });
    expect(fixture.componentInstance.log).toEqual(['saved']);
  });

  it('creates a category with half-hour appointments', async () => {
    catalog.createCategory.mockReturnValue(of({}));
    await start();

    type('Depilación');
    chooseDuration('30 minutos');
    submit();

    expect(catalog.createCategory).toHaveBeenCalledWith({ name: 'Depilación', appointmentMinutes: 30 });
  });

  it('edits an existing category, starting from its current data', async () => {
    catalog.updateCategory.mockReturnValue(of({}));
    await start({ id: 4, name: 'Depilacion', active: true, appointmentMinutes: 30, services: [] });
    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Editar categoría');
    expect(input().value).toBe('Depilacion');
    expect(duration().selectedOptions[0].textContent?.trim()).toBe('30 minutos');

    type('Depilación');
    submit();

    expect(catalog.updateCategory).toHaveBeenCalledWith(4, { name: 'Depilación', appointmentMinutes: 30 });
  });

  it('does not send an empty or blank name', async () => {
    await start();

    type('   ');
    submit();

    expect(fixture.nativeElement.textContent).toContain('Ingresá el nombre de la categoría.');
    expect(catalog.createCategory).not.toHaveBeenCalled();
    expect(fixture.componentInstance.log).toEqual([]);
  });

  it('shows the reason when the API refuses the name and stays open', async () => {
    catalog.createCategory.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { message: 'Ya existe una categoría con ese nombre' } })),
    );
    await start();

    type('Masajes');
    submit();

    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Ya existe una categoría');
    expect(fixture.componentInstance.log).toEqual([]);
  });

  it('reports cancelled', async () => {
    await start();

    (fixture.nativeElement.querySelector('.form-actions .btn') as HTMLButtonElement).click();

    expect(fixture.componentInstance.log).toEqual(['cancelled']);
  });
});
