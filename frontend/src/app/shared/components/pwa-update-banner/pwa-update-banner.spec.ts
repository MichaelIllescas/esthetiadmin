import { TestBed } from '@angular/core/testing';

import { PwaUpdateService } from '../../../core/services/pwa-update.service';
import { PwaUpdateBanner } from './pwa-update-banner';

describe('PwaUpdateBanner', () => {
  const activateUpdate = vi.fn().mockResolvedValue(undefined);
  const updates = {
    updateAvailable: () => true,
    activateUpdate,
  };

  beforeEach(async () => {
    activateUpdate.mockClear();

    await TestBed.configureTestingModule({
      imports: [PwaUpdateBanner],
      providers: [{ provide: PwaUpdateService, useValue: updates }],
    }).compileComponents();
  });

  it('shows the available update and activates it', async () => {
    const fixture = TestBed.createComponent(PwaUpdateBanner);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    button.click();
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('Nueva versión disponible');
    expect(activateUpdate).toHaveBeenCalledOnce();
  });
});
