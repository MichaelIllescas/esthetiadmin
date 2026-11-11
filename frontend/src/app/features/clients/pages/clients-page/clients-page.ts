import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { PageHelp } from '../../../../shared/components/page-help/page-help';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Icon } from '../../../../shared/components/icon/icon';
import { catchError, debounceTime, distinctUntilChanged, finalize, map, of, Subject, switchMap, tap } from 'rxjs';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { Page } from '../../../../shared/models/page.model';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { BirthdaysPanel } from '../../components/birthdays-panel/birthdays-panel';
import { ClientFormDialog } from '../../components/client-form-dialog/client-form-dialog';
import { Client } from '../../models/client.models';
import { ClientsService } from '../../services/clients.service';

const PAGE_SIZE = 20;
const SEARCH_DELAY_MS = 300;

type Outcome = { ok: true; page: Page<Client> } | { ok: false; message: string };
type DialogState = { type: 'form'; client: Client | null } | { type: 'delete'; client: Client };

@Component({
  selector: 'app-clients-page',
  imports: [PageHelp, ReactiveFormsModule, RouterLink, BirthdaysPanel, ClientFormDialog, ConfirmDialog, Icon],
  templateUrl: './clients-page.html',
  styleUrl: './clients-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientsPage implements OnInit {
  private readonly clients = inject(ClientsService);
  private readonly load$ = new Subject<void>();

  protected readonly searchControl = new FormControl('', { nonNullable: true });
  protected readonly searchTerm = signal('');
  protected readonly pageIndex = signal(0);
  protected readonly result = signal<Page<Client> | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly birthdaysToken = signal(0);

  protected readonly dialog = signal<DialogState | null>(null);
  protected readonly dialogBusy = signal(false);
  protected readonly dialogError = signal<string | null>(null);

  protected readonly rows = computed(() => this.result()?.content ?? []);
  protected readonly totalPages = computed(() => this.result()?.totalPages ?? 0);
  protected readonly hasPrevious = computed(() => this.pageIndex() > 0);
  protected readonly hasNext = computed(() => this.pageIndex() + 1 < this.totalPages());

  constructor() {
    // switchMap drops the answer of an outdated search when the user keeps typing.
    this.load$
      .pipe(
        tap(() => {
          this.isLoading.set(true);
          this.loadError.set(null);
        }),
        switchMap(() =>
          this.clients.search(this.searchTerm(), this.pageIndex(), PAGE_SIZE).pipe(
            map((page): Outcome => ({ ok: true, page })),
            catchError((error: unknown) =>
              of<Outcome>({ ok: false, message: apiErrorMessage(error, 'No pudimos cargar los clientes. Intentá nuevamente.') }),
            ),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((outcome) => {
        this.isLoading.set(false);
        if (outcome.ok) {
          this.result.set(outcome.page);
        } else {
          this.loadError.set(outcome.message);
        }
      });

    this.searchControl.valueChanges
      .pipe(
        debounceTime(SEARCH_DELAY_MS),
        map((value) => value.trim()),
        distinctUntilChanged(),
        takeUntilDestroyed(),
      )
      .subscribe((term) => {
        this.searchTerm.set(term);
        this.pageIndex.set(0);
        this.load$.next();
      });
  }

  ngOnInit(): void {
    this.reload();
  }

  protected reload(): void {
    this.load$.next();
  }

  protected goToPage(index: number): void {
    this.pageIndex.set(index);
    this.reload();
  }

  protected openForm(client: Client | null): void {
    this.dialog.set({ type: 'form', client });
  }

  protected requestDelete(client: Client): void {
    this.dialogError.set(null);
    this.dialog.set({ type: 'delete', client });
  }

  protected closeDialog(): void {
    this.dialog.set(null);
    this.dialogError.set(null);
  }

  protected onSaved(): void {
    this.closeDialog();
    this.birthdaysToken.update((token) => token + 1);
    this.reload();
  }

  protected confirmDelete(client: Client): void {
    this.dialogBusy.set(true);
    this.dialogError.set(null);
    this.clients
      .delete(client.id)
      .pipe(finalize(() => this.dialogBusy.set(false)))
      .subscribe({
        next: () => {
          // Deleting the only client of the last page would leave an empty page: go back one.
          if (this.rows().length === 1 && this.pageIndex() > 0) {
            this.pageIndex.update((index) => index - 1);
          }
          this.onSaved();
        },
        error: (error: unknown) =>
          this.dialogError.set(apiErrorMessage(error, 'No pudimos eliminar al cliente. Intentá nuevamente.')),
      });
  }
}
