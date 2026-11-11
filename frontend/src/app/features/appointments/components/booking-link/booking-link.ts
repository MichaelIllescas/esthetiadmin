import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { environment } from '../../../../../environments/environment';

type CopyState = 'idle' | 'copied' | 'manual';

/** The link of the public booking page, ready to copy and send to clients (ADR 014). */
@Component({
  selector: 'app-booking-link',
  template: `
    <section class="booking-link" aria-labelledby="booking-link-title">
      <div class="text">
        <h2 id="booking-link-title">Link para sacar turnos</h2>
        <p>Pasáselo a tus clientas para que reserven solas, sin registrarse.</p>
      </div>
      <div class="copy">
        <label class="visually-hidden" for="booking-link-url">Link de la página de turnos</label>
        <input #urlInput id="booking-link-url" type="text" readonly [value]="url" (focus)="urlInput.select()" />
        <button class="btn btn-primary" type="button" (click)="copy()">
          {{ state() === 'copied' ? '¡Copiado!' : 'Copiar link' }}
        </button>
        <a class="btn" [href]="url" target="_blank" rel="noopener">Abrir</a>
      </div>
      <p class="feedback" role="status" aria-live="polite">
        @if (state() === 'copied') {
          Link copiado. Ya podés pegarlo en WhatsApp o en tus redes.
        } @else if (state() === 'manual') {
          No se pudo copiar solo: el link quedó seleccionado, copialo con Ctrl+C (o mantené apretado en el celular).
        }
      </p>
    </section>
  `,
  styles: `
    :host { display: block; }
    .booking-link {
      display: grid;
      gap: 10px;
      padding: 14px 16px;
      background: #ffffff;
      border: 1px solid var(--line);
      border-left: 4px solid var(--rose-deep);
      border-radius: 10px;
    }
    h2 { margin: 0; color: var(--rose-deep); font-size: 1rem; }
    .text p { margin: 2px 0 0; color: var(--muted); font-size: 0.9rem; }
    .copy { display: flex; flex-wrap: wrap; gap: 8px; }
    input {
      flex: 1 1 220px;
      min-width: 0;
      min-height: 44px;
      padding: 8px 12px;
      color: var(--ink);
      background: var(--sand);
      border: 1px solid var(--line);
      border-radius: 6px;
    }
    input:focus-visible { outline: 3px solid var(--gold); outline-offset: 1px; }
    .btn { text-decoration: none; }
    .feedback { margin: 0; color: var(--muted); font-size: 0.85rem; }
    .feedback:empty { display: none; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingLink {
  private readonly document = inject(DOCUMENT);
  private readonly urlInput = viewChild.required<ElementRef<HTMLInputElement>>('urlInput');

  protected readonly url = environment.publicBookingUrl;
  protected readonly state = signal<CopyState>('idle');

  /** The clipboard API can be missing or refused (old browsers, no HTTPS): then the link is left selected. */
  protected async copy(): Promise<void> {
    const clipboard = this.document.defaultView?.navigator.clipboard;
    try {
      if (!clipboard) {
        throw new Error('Clipboard not available');
      }
      await clipboard.writeText(this.url);
      this.state.set('copied');
    } catch {
      this.urlInput().nativeElement.select();
      this.state.set('manual');
    }
  }
}
