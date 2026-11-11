import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AvailabilityService } from '../../services/availability.service';
import { AvailabilityPage } from './availability-page';

describe('AvailabilityPage', () => {
  it('shows the opening hours and the blocks', async () => {
    await TestBed.configureTestingModule({
      imports: [AvailabilityPage],
      providers: [{ provide: AvailabilityService, useValue: { getRules: () => of([]), getBlocks: () => of([]) } }],
    }).compileComponents();
    const fixture = TestBed.createComponent(AvailabilityPage);
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('h1')?.textContent).toContain('Horarios');
    expect(root.querySelector('details.help summary')?.textContent).toContain('¿Cómo funciona esta sección?');
    expect(root.querySelector('details.help')?.textContent).toContain('no cancela los turnos que ya están dados');
    expect(root.querySelector('app-opening-hours-editor')).not.toBeNull();
    expect(root.querySelector('app-block-list')).not.toBeNull();
  });
});
