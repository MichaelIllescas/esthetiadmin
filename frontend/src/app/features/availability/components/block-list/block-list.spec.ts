import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AvailabilityBlock } from '../../models/availability.models';
import { AvailabilityService } from '../../services/availability.service';
import { BlockList } from './block-list';

const holiday: AvailabilityBlock = {
  id: 3,
  startDate: '2099-10-12',
  endDate: '2099-10-12',
  startTime: null,
  endTime: null,
  reason: 'Feriado',
  affectedAppointments: [],
};
const afternoon: AvailabilityBlock = {
  id: 4,
  startDate: '2099-10-15',
  endDate: '2099-10-15',
  startTime: '15:00',
  endTime: '19:00',
  reason: null,
  affectedAppointments: [],
};

describe('BlockList', () => {
  const availability = { getBlocks: vi.fn(), createBlock: vi.fn(), deleteBlock: vi.fn() };
  let fixture: ComponentFixture<BlockList>;

  const root = () => fixture.nativeElement as HTMLElement;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ');
  const button = (label: string) =>
    Array.from(root().querySelectorAll('button')).find(
      (candidate) => candidate.textContent?.trim() === label || candidate.getAttribute('aria-label') === label,
    ) as HTMLButtonElement;
  const start = async () => {
    await TestBed.configureTestingModule({
      imports: [BlockList],
      providers: [{ provide: AvailabilityService, useValue: availability }],
    }).compileComponents();
    fixture = TestBed.createComponent(BlockList);
    fixture.detectChanges();
  };

  beforeEach(() => {
    Object.values(availability).forEach((mock) => mock.mockReset());
  });

  it('lists the upcoming blocks with their hours and reason', async () => {
    availability.getBlocks.mockReturnValue(of([holiday, afternoon]));
    await start();

    expect(availability.getBlocks).toHaveBeenCalledWith();
    const items = Array.from(root().querySelectorAll('.block-text')).map((item) =>
      Array.from(item.children).map((part) => part.textContent?.trim()),
    );
    expect(items).toEqual([
      ['12 de octubre de 2099', 'Todo el día', 'Feriado'],
      ['15 de octubre de 2099', '15:00 a 19:00'],
    ]);
  });

  it('shows a several-day block as one range', async () => {
    availability.getBlocks.mockReturnValue(of([{ ...holiday, startDate: '2099-01-04', endDate: '2099-01-15' }]));
    await start();

    expect(root().querySelector('.block-text strong')?.textContent).toBe('4 al 15 de enero de 2099');
  });

  it('says when there are no blocks', async () => {
    availability.getBlocks.mockReturnValue(of([]));
    await start();

    expect(text()).toContain('No hay bloqueos de hoy en adelante.');
  });

  it('creates a block and reloads, warning about the appointments left inside', async () => {
    availability.getBlocks.mockReturnValue(of([]));
    await start();
    availability.createBlock.mockReturnValue(
      of({
        ...holiday,
        affectedAppointments: [{ id: 52, startsAt: '2099-10-12T10:30:00', firstName: 'Carina', lastName: 'Diaz' }],
      }),
    );
    availability.getBlocks.mockReturnValue(of([holiday]));

    button('+ Bloquear agenda').click();
    fixture.detectChanges();
    root().querySelector('app-block-form-dialog form')?.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(availability.createBlock).toHaveBeenCalled();
    expect(availability.getBlocks).toHaveBeenCalledTimes(2);
    expect(text()).toContain('12 de octubre de 2099 10:30, Carina Diaz');
    expect(root().querySelector('app-block-form-dialog')).toBeNull();
  });

  it('deletes a block after confirming', async () => {
    availability.getBlocks.mockReturnValue(of([holiday]));
    await start();
    availability.deleteBlock.mockReturnValue(of(undefined));

    button('Eliminar el bloqueo del 12 de octubre de 2099').click();
    fixture.detectChanges();
    (root().querySelector('app-confirm-dialog .btn-danger') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(availability.deleteBlock).toHaveBeenCalledWith(3);
    expect(root().querySelector('app-confirm-dialog')).toBeNull();
  });

  it('offers to retry when the blocks cannot be loaded', async () => {
    availability.getBlocks.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await start();

    expect(root().querySelector('[role="alert"]')?.textContent).toContain('No pudimos cargar los bloqueos');
  });
});
