import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { PageHelp } from '../../../../shared/components/page-help/page-help';
import { apiErrorMessage } from '../../../../shared/utils/api-error';
import { EMAIL_PATTERN } from '../../../../shared/utils/contact';
import { BusinessSettings } from '../../models/settings.models';
import { SettingsService } from '../../services/settings.service';

/** Optional, but when given: 8 to 15 digits once spaces and dashes are left out, as the API keeps it. */
function optionalPhone(control: AbstractControl<string>): ValidationErrors | null {
  const digits = (control.value ?? '').replace(/\D/g, '').length;
  return !control.value?.trim() || (digits >= 8 && digits <= 15) ? null : { phone: true };
}

/**
 * The business data (ADR 016): where the owner gets the booking notices and the phone clients call. Once something
 * is saved it shows as a read-only card with an "Editar" button; the form opens while nothing is set.
 */
@Component({
  selector: 'app-settings-page',
  imports: [ReactiveFormsModule, PageHelp],
  template: `
    <header class="page-header">
      <h1>Configuración</h1>
      <p class="intro">Tus datos de contacto para los avisos de turnos.</p>
    </header>

    <app-page-help>
      <h2>Email de avisos</h2>
      <ul>
        <li>Cada vez que una clienta saca turno desde la página, te llega un email a esta dirección con sus datos y el horario.</li>
        <li>Si lo dejás vacío, no te llega ningún aviso. Los turnos se reservan igual.</li>
        <li>Este email no lo ven las clientas.</li>
      </ul>
      <h2>Celular de contacto</h2>
      <ul>
        <li>Es el número al que te escriben las clientas para <strong>cancelar o cambiar</strong> un turno.</li>
        <li>Aparece en los emails que reciben, con un botón para escribirte por <strong>WhatsApp</strong>, y en la página de turnos al terminar la reserva.</li>
        <li>Cargalo con la característica, por ejemplo <strong>341 555-1234</strong>. Si lo dejás vacío, esas partes no se muestran.</li>
      </ul>
    </app-page-help>

    <section class="card" aria-labelledby="settings-title">
      <h2 id="settings-title">Datos de contacto</h2>

      @if (loadError(); as error) {
        <div class="status status-error" role="alert">
          <p>{{ error }}</p>
          <button class="btn" type="button" (click)="load()">Reintentar</button>
        </div>
      } @else if (isLoading()) {
        <p class="status" role="status">Cargando...</p>
      } @else if (!isEditing()) {
        @if (savedNotice()) {
          <p class="notice" role="status">Configuración guardada.</p>
        }
        <dl class="summary">
          <div>
            <dt>Email de avisos</dt>
            <dd>{{ saved()?.notificationEmail ?? 'Sin cargar: no te llegan avisos de los turnos web' }}</dd>
          </div>
          <div>
            <dt>Celular de contacto</dt>
            <dd>{{ saved()?.contactPhone ?? 'Sin cargar: los emails a las clientas salen sin número de contacto' }}</dd>
          </div>
        </dl>
        <div class="form-actions">
          <button class="btn btn-primary" type="button" (click)="edit()">Editar</button>
        </div>
      } @else {
        <form class="stack-form" [formGroup]="form" (ngSubmit)="save()" novalidate>
          <div class="field">
            <label for="settings-email">Email de avisos <span class="field-hint">(opcional)</span></label>
            <input
              id="settings-email"
              type="email"
              inputmode="email"
              formControlName="notificationEmail"
              maxlength="254"
              autocomplete="email"
              [attr.aria-invalid]="isInvalid('notificationEmail')"
              [attr.aria-describedby]="isInvalid('notificationEmail') ? 'settings-email-error' : null"
            />
            @if (isInvalid('notificationEmail')) {
              <p class="field-error" id="settings-email-error">Ingresá un email válido o dejalo vacío.</p>
            }
          </div>

          <div class="field">
            <label for="settings-phone">Celular de contacto <span class="field-hint">(opcional)</span></label>
            <input
              id="settings-phone"
              type="tel"
              inputmode="tel"
              formControlName="contactPhone"
              maxlength="30"
              autocomplete="tel"
              [attr.aria-invalid]="isInvalid('contactPhone')"
              [attr.aria-describedby]="isInvalid('contactPhone') ? 'settings-phone-error' : null"
            />
            @if (isInvalid('contactPhone')) {
              <p class="field-error" id="settings-phone-error">Ingresá un celular de 8 a 15 números o dejalo vacío.</p>
            }
          </div>

          @if (saveError(); as error) {
            <p class="form-error" role="alert">{{ error }}</p>
          }
          <div class="form-actions">
            @if (hasSaved()) {
              <button class="btn" type="button" (click)="cancelEdit()">Cancelar</button>
            }
            <button class="btn btn-primary" type="submit" [disabled]="isSaving()">
              {{ isSaving() ? 'Guardando...' : 'Guardar' }}
            </button>
          </div>
        </form>
      }
    </section>
  `,
  styles: `
    :host {
      display: grid;
      grid-template-columns: minmax(0, 1fr);
      gap: 20px;
    }

    h1 {
      margin: 0;
      color: var(--rose-deep);
      font-family: Georgia, 'Times New Roman', serif;
      font-size: clamp(2rem, 4vw, 2.8rem);
      font-weight: 400;
    }

    .intro {
      margin: 6px 0 0;
      color: var(--muted);
    }

    .card {
      max-width: 560px;
      padding: 20px;
      background: #ffffff;
      border: 1px solid var(--line);
      border-radius: 10px;
    }

    .card h2 {
      margin: 0 0 12px;
      font-size: 1.1rem;
    }

    .summary {
      display: grid;
      gap: 14px;
      margin: 0 0 16px;
    }

    .summary dt {
      color: var(--muted);
      font-size: 0.85rem;
      font-weight: 700;
      letter-spacing: 0.04em;
      text-transform: uppercase;
    }

    .summary dd {
      margin: 2px 0 0;
      color: var(--ink);
      font-size: 1.05rem;
      overflow-wrap: anywhere;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsPage implements OnInit {
  private readonly settings = inject(SettingsService);

  protected readonly isLoading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly isSaving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly savedNotice = signal(false);
  protected readonly saved = signal<BusinessSettings | null>(null);
  protected readonly isEditing = signal(false);
  protected readonly hasSaved = computed(() => {
    const saved = this.saved();
    return !!saved && (saved.notificationEmail !== null || saved.contactPhone !== null);
  });

  protected readonly form = new FormGroup({
    notificationEmail: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(EMAIL_PATTERN), Validators.maxLength(254)],
    }),
    contactPhone: new FormControl('', { nonNullable: true, validators: [optionalPhone, Validators.maxLength(30)] }),
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.isLoading.set(true);
    this.loadError.set(null);
    this.settings
      .get()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (current) => {
          this.saved.set(current);
          this.isEditing.set(!this.hasSaved());
        },
        error: (error: unknown) =>
          this.loadError.set(apiErrorMessage(error, 'No pudimos cargar la configuración. Intentá nuevamente.')),
      });
  }

  protected edit(): void {
    const saved = this.saved();
    this.form.reset({ notificationEmail: saved?.notificationEmail ?? '', contactPhone: saved?.contactPhone ?? '' });
    this.saveError.set(null);
    this.savedNotice.set(false);
    this.isEditing.set(true);
  }

  protected cancelEdit(): void {
    this.saveError.set(null);
    this.isEditing.set(false);
  }

  protected isInvalid(control: 'notificationEmail' | 'contactPhone'): boolean {
    return this.form.controls[control].invalid && this.form.controls[control].touched;
  }

  protected save(): void {
    this.saveError.set(null);
    this.savedNotice.set(false);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { notificationEmail, contactPhone } = this.form.getRawValue();
    this.isSaving.set(true);
    this.settings
      .save({ notificationEmail: notificationEmail.trim() || null, contactPhone: contactPhone.trim() || null })
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: (saved) => {
          this.saved.set(saved);
          this.savedNotice.set(true);
          this.isEditing.set(!this.hasSaved());
        },
        error: (error: unknown) =>
          this.saveError.set(apiErrorMessage(error, 'No pudimos guardar la configuración. Intentá nuevamente.')),
      });
  }
}
