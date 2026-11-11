import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, map, of, Subject, switchMap, tap } from 'rxjs';
import { Icon } from '../../../../shared/components/icon/icon';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { Page } from '../../../../shared/models/page.model';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { formatDate, formatPrice, formatShortDate } from '../../../../shared/utils/format';
import { Category } from '../../../catalog/models/catalog.models';
import { CatalogService } from '../../../catalog/services/catalog.service';
import { ClientPicker } from '../../../clients/components/client-picker/client-picker';
import { Client } from '../../../clients/models/client.models';
import { Treatment, TreatmentFilters } from '../../models/treatment.models';
import { TreatmentsService } from '../../services/treatments.service';
import { TreatmentFormDialog } from '../treatment-form-dialog/treatment-form-dialog';

const PAGE_SIZE = 20;
const NO_FILTERS: TreatmentFilters = { clientId: null, categoryId: null, serviceId: null, from: '', to: '' };

type Outcome = { ok: true; page: Page<Treatment> } | { ok: false; message: string };
type DialogState =
  | { type: 'form'; treatment: Treatment | null }
  | { type: 'delete'; treatment: Treatment };

/**
 * Treatments with filters and pagination. With `clientId` it is the history of that client (used in the
 * client record); without it, it shows every client's treatments and offers a client filter.
 */
@Component({
  selector: 'app-treatment-list',
  imports: [ClientPicker, TreatmentFormDialog, ConfirmDialog, Icon],
  templateUrl: './treatment-list.html',
  styleUrl: './treatment-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TreatmentList implements OnInit {
  private readonly treatments = inject(TreatmentsService);
  private readonly catalog = inject(CatalogService);
  private readonly load$ = new Subject<void>();

  /** Fixed client (its record); null lists every client. */
  readonly clientId = input<number | null>(null);
  readonly clientName = input<string | null>(null);

  protected readonly filters = signal<TreatmentFilters>(NO_FILTERS);
  protected readonly pageIndex = signal(0);
  protected readonly result = signal<Page<Treatment> | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly categories = signal<Category[]>([]);

  protected readonly dialog = signal<DialogState | null>(null);
  protected readonly dialogBusy = signal(false);
  protected readonly dialogError = signal<string | null>(null);

  protected readonly showClient = computed(() => this.clientId() === null);
  protected readonly rows = computed(() => this.result()?.content ?? []);
  protected readonly totalPages = computed(() => this.result()?.totalPages ?? 0);
  protected readonly hasPrevious = computed(() => this.pageIndex() > 0);
  protected readonly hasNext = computed(() => this.pageIndex() + 1 < this.totalPages());
  protected readonly rangeInvalid = computed(() => {
    const { from, to } = this.filters();
    return from !== '' && to !== '' && from > to;
  });
  protected readonly hasFilters = computed(() => {
    const f = this.filters();
    return f.clientId !== null || f.categoryId !== null || f.serviceId !== null || f.from !== '' || f.to !== '';
  });
  protected readonly serviceChoices = computed(() => {
    const categoryId = this.filters().categoryId;
    const source = categoryId === null ? this.categories() : this.categories().filter((c) => c.id === categoryId);
    return source.flatMap((category) => category.services);
  });
  protected readonly fixedClient = computed(() => {
    const id = this.clientId();
    const name = this.clientName();
    return id !== null && name ? { id, name } : null;
  });

  protected readonly formatDate = formatDate;
  protected readonly formatShortDate = formatShortDate;
  protected readonly formatPrice = formatPrice;

  constructor() {
    this.load$
      .pipe(
        tap(() => {
          this.isLoading.set(true);
          this.loadError.set(null);
        }),
        switchMap(() =>
          this.treatments.search(this.effectiveFilters(), this.pageIndex(), PAGE_SIZE).pipe(
            map((page): Outcome => ({ ok: true, page })),
            catchError((error: unknown) =>
              of<Outcome>({ ok: false, message: apiErrorMessage(error, 'No pudimos cargar los tratamientos. Intentá nuevamente.') }),
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
  }

  ngOnInit(): void {
    // The filters only need the catalog for their lists; the treatments do not depend on it.
    this.catalog.getCategories().subscribe({ next: (categories) => this.categories.set(categories), error: () => undefined });
    this.reload();
  }

  protected reload(): void {
    this.load$.next();
  }

  protected goToPage(index: number): void {
    this.pageIndex.set(index);
    this.reload();
  }

  protected setFilter(change: Partial<TreatmentFilters>): void {
    this.filters.update((current) => ({ ...current, ...change }));
    this.pageIndex.set(0);
    if (!this.rangeInvalid()) {
      this.reload();
    }
  }

  protected onCategoryChange(event: Event): void {
    const categoryId = this.toId((event.target as HTMLSelectElement).value);
    const stillValid = this.filters().serviceId === null
      || this.categories().some((c) => (categoryId === null || c.id === categoryId) && c.services.some((s) => s.id === this.filters().serviceId));
    this.setFilter({ categoryId, serviceId: stillValid ? this.filters().serviceId : null });
  }

  protected onServiceChange(event: Event): void {
    this.setFilter({ serviceId: this.toId((event.target as HTMLSelectElement).value) });
  }

  protected onDateChange(field: 'from' | 'to', event: Event): void {
    this.setFilter({ [field]: (event.target as HTMLInputElement).value });
  }

  protected onClientPicked(client: Client | null): void {
    this.setFilter({ clientId: client?.id ?? null });
  }

  protected clearFilters(): void {
    this.filters.set(NO_FILTERS);
    this.pageIndex.set(0);
    this.reload();
    // The client picker keeps its own state: re-render it so it starts empty as well.
    this.pickerKey.update((key) => key + 1);
  }

  protected readonly pickerKey = signal(0);

  protected openForm(treatment: Treatment | null): void {
    this.dialog.set({ type: 'form', treatment });
  }

  protected requestDelete(treatment: Treatment): void {
    this.dialogError.set(null);
    this.dialog.set({ type: 'delete', treatment });
  }

  protected closeDialog(): void {
    this.dialog.set(null);
    this.dialogError.set(null);
  }

  protected onSaved(): void {
    this.closeDialog();
    this.reload();
  }

  protected confirmDelete(treatment: Treatment): void {
    this.dialogBusy.set(true);
    this.dialogError.set(null);
    this.treatments
      .delete(treatment.id)
      .pipe(finalize(() => this.dialogBusy.set(false)))
      .subscribe({
        next: () => {
          // Deleting the only treatment of the last page would leave an empty page: go back one.
          if (this.rows().length === 1 && this.pageIndex() > 0) {
            this.pageIndex.update((index) => index - 1);
          }
          this.onSaved();
        },
        error: (error: unknown) =>
          this.dialogError.set(apiErrorMessage(error, 'No pudimos eliminar el tratamiento. Intentá nuevamente.')),
      });
  }

  protected deleteMessage(treatment: Treatment): string {
    const whose = this.showClient() ? ` de ${treatment.clientName}` : '';
    return `¿Eliminar el tratamiento "${treatment.serviceName}"${whose} del ${formatDate(treatment.performedOn)}? Esta acción no se puede deshacer y ese ingreso deja de contarse en los reportes.`;
  }

  private effectiveFilters(): TreatmentFilters {
    const fixed = this.clientId();
    const current = this.filters();
    return fixed !== null ? { ...current, clientId: fixed } : current;
  }

  private toId(value: string): number | null {
    return value === '' ? null : Number(value);
  }
}
