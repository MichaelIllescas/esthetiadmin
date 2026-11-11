import { DOCUMENT } from '@angular/common';
import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SwUpdate, VersionReadyEvent } from '@angular/service-worker';
import { filter } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PwaUpdateService {
  private readonly swUpdate = inject(SwUpdate);
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);

  readonly updateAvailable = signal(false);

  constructor() {
    if (!this.swUpdate.isEnabled) return;

    this.swUpdate.versionUpdates
      .pipe(
        filter((event): event is VersionReadyEvent => event.type === 'VERSION_READY'),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.updateAvailable.set(true));
  }

  async activateUpdate(): Promise<void> {
    if (!this.swUpdate.isEnabled) return;

    await this.swUpdate.activateUpdate();
    this.document.location.reload();
  }
}
