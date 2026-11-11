import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type IconName = 'edit' | 'trash' | 'eye' | 'eye-off';

/**
 * A decorative icon: the button that holds it must carry its own accessible name (aria-label),
 * so the icon is hidden from assistive technology.
 */
@Component({
  selector: 'app-icon',
  template: `
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"
         stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">
      @switch (name()) {
        @case ('edit') {
          <path d="M12 20h9" />
          <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
        }
        @case ('eye') {
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8Z" />
          <circle cx="12" cy="12" r="3" />
        }
        @case ('eye-off') {
          <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
          <path d="M1 1l22 22" />
        }
        @case ('trash') {
          <path d="M3 6h18" />
          <path d="M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2" />
          <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
          <path d="M10 11v6" />
          <path d="M14 11v6" />
        }
      }
    </svg>
  `,
  styles: `
    :host { display: inline-flex; }
    svg { display: block; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Icon {
  readonly name = input.required<IconName>();
}
