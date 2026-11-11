import { ChangeDetectionStrategy, Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { SessionService } from '../../core/services/session.service';

@Component({
  selector: 'app-user-menu',
  templateUrl: './user-menu.html',
  styleUrl: './user-menu.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:click)': 'closeOnOutsideClick($event)',
    '(document:keydown.escape)': 'closeAndRestoreFocus()',
  },
})
export class UserMenu {
  private readonly session = inject(SessionService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly toggleButton = viewChild<ElementRef<HTMLButtonElement>>('toggleButton');

  protected readonly isOpen = signal(false);
  protected readonly username = this.session.username;
  protected readonly initial = computed(() => (this.username()?.charAt(0) ?? '?').toUpperCase());

  protected toggle(): void {
    this.isOpen.update((open) => !open);
  }

  protected logout(): void {
    this.isOpen.set(false);
    this.session.logout();
  }

  protected closeOnOutsideClick(event: Event): void {
    if (this.isOpen() && !this.host.nativeElement.contains(event.target as Node)) {
      this.isOpen.set(false);
    }
  }

  protected closeAndRestoreFocus(): void {
    if (this.isOpen()) {
      this.isOpen.set(false);
      this.toggleButton()?.nativeElement.focus();
    }
  }
}
