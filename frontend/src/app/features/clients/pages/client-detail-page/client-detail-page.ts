import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { whatsappUrl } from '../../../../shared/utils/contact';
import { formatDate } from '../../../../shared/utils/format';
import { TreatmentList } from '../../../treatments/components/treatment-list/treatment-list';
import { ClientFormDialog } from '../../components/client-form-dialog/client-form-dialog';
import { Client } from '../../models/client.models';
import { ClientsService } from '../../services/clients.service';

type DialogState = 'edit' | 'delete';

@Component({
  selector: 'app-client-detail-page',
  imports: [RouterLink, ClientFormDialog, ConfirmDialog, TreatmentList],
  templateUrl: './client-detail-page.html',
  styleUrl: './client-detail-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientDetailPage implements OnInit {
  private readonly clients = inject(ClientsService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly client = signal<Client | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly notFound = signal(false);

  protected readonly dialog = signal<DialogState | null>(null);
  protected readonly dialogBusy = signal(false);
  protected readonly dialogError = signal<string | null>(null);

  protected readonly formatDate = formatDate;
  protected readonly whatsappUrl = whatsappUrl;

  protected initials(client: Client): string {
    return `${client.firstName.trim().charAt(0)}${client.lastName.trim().charAt(0)}`.toUpperCase();
  }

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => this.load(Number(params.get('id'))));
  }

  protected reload(): void {
    const current = this.client();
    this.load(current ? current.id : Number(this.route.snapshot.paramMap.get('id')));
  }

  protected openDialog(state: DialogState): void {
    this.dialogError.set(null);
    this.dialog.set(state);
  }

  protected closeDialog(): void {
    this.dialog.set(null);
    this.dialogError.set(null);
  }

  protected onSaved(): void {
    this.closeDialog();
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
          this.closeDialog();
          void this.router.navigate(['/clientes']);
        },
        error: (error: unknown) =>
          this.dialogError.set(apiErrorMessage(error, 'No pudimos eliminar al cliente. Intentá nuevamente.')),
      });
  }

  private load(id: number): void {
    this.isLoading.set(true);
    this.loadError.set(null);
    this.notFound.set(false);
    if (!Number.isInteger(id)) {
      this.notFound.set(true);
      this.isLoading.set(false);
      return;
    }
    this.clients
      .get(id)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (client) => this.client.set(client),
        error: (error: unknown) => {
          this.client.set(null);
          if (error instanceof HttpErrorResponse && error.status === 404) {
            this.notFound.set(true);
          } else {
            this.loadError.set(apiErrorMessage(error, 'No pudimos cargar al cliente. Intentá nuevamente.'));
          }
        },
      });
  }
}
