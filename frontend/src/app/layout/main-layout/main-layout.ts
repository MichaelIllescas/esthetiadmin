import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NAV_ITEMS } from '../nav-items';
import { UserMenu } from '../user-menu/user-menu';

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgOptimizedImage, UserMenu],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'closeMenu()' },
})
export class MainLayout {
  protected readonly navItems = NAV_ITEMS;
  protected readonly isMenuOpen = signal(false);

  protected toggleMenu(): void {
    this.isMenuOpen.update((open) => !open);
  }

  protected closeMenu(): void {
    this.isMenuOpen.set(false);
  }
}
