import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { PageHelp } from '../../../../shared/components/page-help/page-help';
import { finalize, Observable } from 'rxjs';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { CategoryBlock } from '../../components/category-block/category-block';
import { CategoryFormDialog } from '../../components/category-form-dialog/category-form-dialog';
import { OfferingFormDialog } from '../../components/offering-form-dialog/offering-form-dialog';
import { Category, Offering } from '../../models/catalog.models';
import { CatalogService } from '../../services/catalog.service';

type DeleteTarget = { kind: 'category'; item: Category } | { kind: 'offering'; item: Offering };

type DialogState =
  | { type: 'category-form'; category: Category | null }
  | { type: 'offering-form'; offering: Offering | null; categoryId: number | null }
  | { type: 'delete'; target: DeleteTarget };

@Component({
  selector: 'app-catalog-page',
  imports: [PageHelp, CategoryBlock, CategoryFormDialog, OfferingFormDialog, ConfirmDialog],
  templateUrl: './catalog-page.html',
  styleUrl: './catalog-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CatalogPage implements OnInit {
  private readonly catalog = inject(CatalogService);

  protected readonly categories = signal<Category[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly actionError = signal<string | null>(null);
  protected readonly showInactive = signal(false);
  protected readonly collapsedIds = signal<ReadonlySet<number>>(new Set());

  protected readonly dialog = signal<DialogState | null>(null);
  protected readonly dialogBusy = signal(false);
  protected readonly dialogError = signal<string | null>(null);

  protected readonly visibleCategories = computed(() =>
    this.showInactive() ? this.categories() : this.categories().filter((category) => category.active),
  );
  protected readonly hiddenCount = computed(() => this.categories().length - this.visibleCategories().length);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.isLoading.set(true);
    this.loadError.set(null);
    this.catalog
      .getCategories()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (categories) => this.categories.set(categories),
        error: (error: unknown) =>
          this.loadError.set(apiErrorMessage(error, 'No pudimos cargar el catálogo. Intentá nuevamente.')),
      });
  }

  protected setShowInactive(event: Event): void {
    this.showInactive.set((event.target as HTMLInputElement).checked);
  }

  protected toggleCollapse(category: Category): void {
    this.collapsedIds.update((ids) => {
      const next = new Set(ids);
      if (!next.delete(category.id)) {
        next.add(category.id);
      }
      return next;
    });
  }

  protected openCategoryForm(category: Category | null): void {
    this.dialog.set({ type: 'category-form', category });
  }

  protected openOfferingForm(offering: Offering | null, categoryId: number | null): void {
    this.dialog.set({ type: 'offering-form', offering, categoryId });
  }

  protected requestDelete(target: DeleteTarget): void {
    this.dialogError.set(null);
    this.dialog.set({ type: 'delete', target });
  }

  protected closeDialog(): void {
    this.dialog.set(null);
    this.dialogError.set(null);
  }

  protected onSaved(): void {
    this.closeDialog();
    this.load();
  }

  protected toggleCategoryActive(category: Category): void {
    this.runAction(this.catalog.setCategoryActive(category.id, !category.active));
  }

  protected toggleOfferingActive(offering: Offering): void {
    this.runAction(this.catalog.setOfferingActive(offering.id, !offering.active));
  }

  protected confirmDelete(target: DeleteTarget): void {
    const request$: Observable<unknown> =
      target.kind === 'category' ? this.catalog.deleteCategory(target.item.id) : this.catalog.deleteOffering(target.item.id);
    this.dialogBusy.set(true);
    this.dialogError.set(null);
    request$.pipe(finalize(() => this.dialogBusy.set(false))).subscribe({
      next: () => this.onSaved(),
      error: (error: unknown) => this.dialogError.set(apiErrorMessage(error, 'No pudimos eliminarlo. Intentá nuevamente.')),
    });
  }

  /** After a refused deletion, the user can turn the item off instead. */
  protected deactivateInstead(target: DeleteTarget): void {
    const request$: Observable<unknown> =
      target.kind === 'category'
        ? this.catalog.setCategoryActive(target.item.id, false)
        : this.catalog.setOfferingActive(target.item.id, false);
    this.dialogBusy.set(true);
    request$.pipe(finalize(() => this.dialogBusy.set(false))).subscribe({
      next: () => this.onSaved(),
      error: (error: unknown) => this.dialogError.set(apiErrorMessage(error, 'No pudimos desactivarlo. Intentá nuevamente.')),
    });
  }

  protected deleteHeading(target: DeleteTarget): string {
    return target.kind === 'category' ? 'Eliminar categoría' : 'Eliminar servicio';
  }

  protected deleteMessage(target: DeleteTarget): string {
    return target.kind === 'category'
      ? `¿Eliminar la categoría "${target.item.name}"? Esta acción no se puede deshacer.`
      : `¿Eliminar el servicio "${target.item.name}"? Esta acción no se puede deshacer.`;
  }

  private runAction(request$: Observable<unknown>): void {
    this.actionError.set(null);
    request$.subscribe({
      next: () => this.load(),
      error: (error: unknown) =>
        this.actionError.set(apiErrorMessage(error, 'No pudimos completar la acción. Intentá nuevamente.')),
    });
  }
}
