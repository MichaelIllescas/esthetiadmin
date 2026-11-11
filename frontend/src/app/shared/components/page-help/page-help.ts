import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * A closed "how does it work" box under a page title, so the owner can read how the section works without asking.
 * The explanation is projected as content (headings `h2` and lists).
 */
@Component({
  selector: 'app-page-help',
  template: `
    <details class="help">
      <summary>{{ summary() }}</summary>
      <div class="help-body">
        <ng-content />
      </div>
    </details>
  `,
  styles: `
    :host { display: block; }

    .help {
      background: #ffffff;
      border: 1px solid var(--line);
      border-left: 4px solid var(--gold);
      border-radius: 10px;
    }

    summary {
      min-height: 44px;
      padding: 12px 16px;
      color: var(--rose-deep);
      font-weight: 700;
      cursor: pointer;
    }

    summary:focus-visible {
      outline: 3px solid var(--gold);
      outline-offset: 2px;
    }

    .help-body {
      padding: 0 16px 16px;
      line-height: 1.5;
    }

    .help-body ::ng-deep h2 {
      margin: 12px 0 4px;
      color: var(--ink);
      font-family: inherit;
      font-size: 1rem;
      font-weight: 700;
    }

    .help-body ::ng-deep ul {
      margin: 0;
      padding-left: 20px;
    }

    .help-body ::ng-deep li + li { margin-top: 4px; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageHelp {
  readonly summary = input('¿Cómo funciona esta sección?');
}
