import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { SwUpdate, VersionEvent } from '@angular/service-worker';
import { Subject } from 'rxjs';

import { PwaUpdateService } from './pwa-update.service';

describe('PwaUpdateService', () => {
  const versionUpdates = new Subject<VersionEvent>();
  const activateUpdate = vi.fn().mockResolvedValue(true);
  const reload = vi.fn();

  beforeEach(() => {
    activateUpdate.mockClear();
    reload.mockClear();

    TestBed.configureTestingModule({
      providers: [
        PwaUpdateService,
        {
          provide: SwUpdate,
          useValue: { isEnabled: true, versionUpdates, activateUpdate },
        },
        {
          provide: DOCUMENT,
          useValue: { location: { reload } },
        },
      ],
    });
  });

  it('announces when a new version is ready', () => {
    const service = TestBed.inject(PwaUpdateService);

    versionUpdates.next({
      type: 'VERSION_READY',
      currentVersion: { hash: 'old' },
      latestVersion: { hash: 'new' },
    });

    expect(service.updateAvailable()).toBe(true);
  });

  it('activates the update and reloads the application', async () => {
    const service = TestBed.inject(PwaUpdateService);

    await service.activateUpdate();

    expect(activateUpdate).toHaveBeenCalledOnce();
    expect(reload).toHaveBeenCalledOnce();
  });
});
