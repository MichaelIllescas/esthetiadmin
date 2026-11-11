import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CatalogService } from '../../../catalog/services/catalog.service';
import { ClientsService } from '../../../clients/services/clients.service';
import { TreatmentsService } from '../../services/treatments.service';
import { TreatmentsPage } from './treatments-page';

describe('TreatmentsPage', () => {
  it('shows the title and the list of every client’s treatments', async () => {
    const treatments = { search: vi.fn().mockReturnValue(of({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })) };
    await TestBed.configureTestingModule({
      imports: [TreatmentsPage],
      providers: [
        { provide: TreatmentsService, useValue: treatments },
        { provide: CatalogService, useValue: { getCategories: () => of([]) } },
        { provide: ClientsService, useValue: { search: vi.fn() } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(TreatmentsPage);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('h1').textContent).toContain('Tratamientos');
    expect(fixture.nativeElement.querySelector('app-treatment-list')).not.toBeNull();
    expect(treatments.search).toHaveBeenCalledWith(expect.objectContaining({ clientId: null }), 0, 20);
  });
});
