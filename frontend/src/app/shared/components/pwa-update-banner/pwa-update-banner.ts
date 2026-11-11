import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { PwaUpdateService } from '../../../core/services/pwa-update.service';

@Component({
  selector: 'app-pwa-update-banner',
  templateUrl: './pwa-update-banner.html',
  styleUrl: './pwa-update-banner.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PwaUpdateBanner {
  readonly updates = inject(PwaUpdateService);
  readonly isUpdating = signal(false);
  readonly updateFailed = signal(false);

  async update(): Promise<void> {
    this.isUpdating.set(true);
    this.updateFailed.set(false);

    try {
      await this.updates.activateUpdate();
    } catch {
      this.isUpdating.set(false);
      this.updateFailed.set(true);
    }
  }
}
