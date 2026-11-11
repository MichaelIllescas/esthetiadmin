import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';
import { Client } from '../../models/client.models';
import { ClientsService } from '../../services/clients.service';

const MAX_OPTIONS = 8;
const SEARCH_DELAY_MS = 250;
let nextId = 0;

/**
 * Picks one client by typing part of the name (combobox with a list of matches). Once a client is chosen it
 * shows it with a button to change it; `picked` emits the client, or null when the choice is cleared.
 */
@Component({
  selector: 'app-client-picker',
  imports: [ReactiveFormsModule],
  templateUrl: './client-picker.html',
  styleUrl: './client-picker.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientPicker {
  private readonly clients = inject(ClientsService);

  readonly label = input('Cliente');
  readonly invalid = input(false);
  readonly errorId = input<string | null>(null);
  readonly picked = output<Client | null>();

  protected readonly id = nextId++;
  protected readonly inputId = `client-picker-${this.id}`;
  protected readonly listId = `client-picker-list-${this.id}`;

  protected readonly search = new FormControl('', { nonNullable: true });
  protected readonly selected = signal<Client | null>(null);
  protected readonly options = signal<Client[]>([]);
  protected readonly isOpen = signal(false);
  protected readonly isSearching = signal(false);
  protected readonly failed = signal(false);
  protected readonly activeIndex = signal(-1);
  protected readonly activeOptionId = computed(() =>
    this.activeIndex() >= 0 ? `${this.listId}-${this.activeIndex()}` : null,
  );
  protected readonly hasTerm = signal(false);

  constructor() {
    this.search.valueChanges
      .pipe(
        tap((value) => this.hasTerm.set(value.trim().length > 0)),
        debounceTime(SEARCH_DELAY_MS),
        map((value) => value.trim()),
        distinctUntilChanged(),
        tap(() => {
          this.isSearching.set(true);
          this.failed.set(false);
        }),
        switchMap((term) =>
          term
            ? this.clients.search(term, 0, MAX_OPTIONS).pipe(
                map((page) => page.content),
                catchError(() => {
                  this.failed.set(true);
                  return of<Client[]>([]);
                }),
              )
            : of<Client[]>([]),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((options) => {
        this.options.set(options);
        this.activeIndex.set(options.length > 0 ? 0 : -1);
        this.isSearching.set(false);
        this.isOpen.set(this.hasTerm());
      });
  }

  protected name(client: Client): string {
    return `${client.lastName}, ${client.firstName}`;
  }

  protected choose(client: Client, event?: Event): void {
    event?.preventDefault();
    this.selected.set(client);
    this.isOpen.set(false);
    this.search.setValue('', { emitEvent: false });
    this.hasTerm.set(false);
    this.picked.emit(client);
  }

  protected clear(): void {
    this.selected.set(null);
    this.options.set([]);
    this.picked.emit(null);
  }

  protected close(): void {
    this.isOpen.set(false);
  }

  protected onKeydown(event: KeyboardEvent): void {
    const count = this.options().length;
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.isOpen.set(this.hasTerm());
        this.activeIndex.update((index) => (count === 0 ? -1 : (index + 1) % count));
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.activeIndex.update((index) => (count === 0 ? -1 : (index - 1 + count) % count));
        break;
      case 'Enter':
        if (this.isOpen() && this.activeIndex() >= 0) {
          event.preventDefault();
          this.choose(this.options()[this.activeIndex()]);
        }
        break;
      case 'Escape':
        if (this.isOpen()) {
          // Closing the list must not also close the dialog that contains the picker.
          event.stopPropagation();
          event.preventDefault();
          this.isOpen.set(false);
        }
        break;
    }
  }
}
