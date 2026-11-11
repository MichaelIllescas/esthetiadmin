import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { BookableCategory, BookableService, ChosenService, durationLabel } from '../../models/booking.models';

/**
 * Step 1: the categories first and, on tapping one, its services, so a long catalog does not turn into an endless
 * list on a phone. One category is open at a time; when there is only one, it starts open.
 */
@Component({
  selector: 'app-service-picker',
  template: `
    @for (category of categories(); track category.categoryId) {
      <section class="category" [class.open]="isOpen(category)">
        <h3>
          <button
            class="toggle"
            type="button"
            [attr.aria-expanded]="isOpen(category)"
            [attr.aria-controls]="'services-' + category.categoryId"
            (click)="toggle(category)"
          >
            <span class="category-name">{{ category.categoryName }}</span>
            <span class="meta">
              {{ category.services.length }} {{ category.services.length === 1 ? 'servicio' : 'servicios' }} ·
              turnos de {{ durationLabel(category.durationMinutes) }}
            </span>
            <svg class="chevron" width="16" height="16" viewBox="0 0 16 16" aria-hidden="true">
              <path d="M3.5 6l4.5 4.5L12.5 6" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </button>
        </h3>
        @if (isOpen(category)) {
          <ul [id]="'services-' + category.categoryId">
            @for (service of category.services; track service.id) {
              <li>
                <button class="service" type="button" (click)="choose(category, service)">
                  <span class="name">{{ service.name }}</span>
                  @if (service.description) {
                    <span class="description">{{ service.description }}</span>
                  }
                </button>
              </li>
            }
          </ul>
        }
      </section>
    } @empty {
      <p class="empty">Por ahora no hay servicios para reservar online. Comunicate con el centro para sacar tu turno.</p>
    }
  `,
  styles: `
    :host { display: grid; gap: 10px; }
    .category {
      background: #ffffff;
      border: 1px solid var(--line);
      border-radius: 12px;
    }
    .category.open { border-color: var(--rose-deep); }
    h3 { margin: 0; font-size: inherit; }
    .toggle {
      display: grid;
      grid-template-columns: minmax(0, 1fr) auto;
      gap: 2px 12px;
      align-items: center;
      width: 100%;
      min-height: 60px;
      padding: 12px 16px;
      color: var(--ink);
      text-align: left;
      background: none;
      border: 0;
      border-radius: 12px;
      cursor: pointer;
    }
    .toggle:focus-visible { outline: 3px solid var(--gold); outline-offset: 2px; }
    .category-name {
      color: var(--rose-deep);
      font-family: Georgia, 'Times New Roman', serif;
      font-size: 1.2rem;
      font-weight: 400;
    }
    .meta { grid-column: 1; color: var(--muted); font-size: 0.85rem; }
    .chevron { grid-column: 2; grid-row: 1 / span 2; color: var(--rose-deep); transition: transform 0.15s ease; }
    .open .chevron { transform: rotate(180deg); }
    ul { display: grid; gap: 8px; margin: 0; padding: 0 12px 12px; list-style: none; }
    .service {
      display: grid;
      gap: 2px;
      width: 100%;
      min-height: 48px;
      padding: 10px 14px;
      color: var(--ink);
      text-align: left;
      background: var(--sand);
      border: 1px solid transparent;
      border-radius: 10px;
      cursor: pointer;
    }
    .service:hover { border-color: var(--rose-deep); }
    .service:focus-visible { outline: 3px solid var(--gold); outline-offset: 2px; }
    .name { font-weight: 700; }
    .description { color: var(--muted); font-size: 0.9rem; }
    .empty { margin: 0; color: var(--muted); }
    @media (prefers-reduced-motion: reduce) { .chevron { transition: none; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ServicePicker {
  readonly categories = input.required<readonly BookableCategory[]>();
  readonly chosen = output<ChosenService>();

  private readonly openId = signal<number | null>(null);
  /** With a single category there is nothing to choose between, so it is shown open. */
  private readonly onlyCategoryId = computed(() =>
    this.categories().length === 1 ? this.categories()[0].categoryId : null,
  );

  protected readonly durationLabel = durationLabel;

  protected isOpen(category: BookableCategory): boolean {
    return (this.openId() ?? this.onlyCategoryId()) === category.categoryId;
  }

  protected toggle(category: BookableCategory): void {
    this.openId.set(this.isOpen(category) ? -1 : category.categoryId);
  }

  protected choose(category: BookableCategory, service: BookableService): void {
    this.chosen.emit({ ...service, categoryName: category.categoryName, durationMinutes: category.durationMinutes });
  }
}
