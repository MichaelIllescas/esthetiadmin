import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BusinessSettings } from '../../models/settings.models';
import { SettingsService } from '../../services/settings.service';

/**
 * Reminds the owner of the contact data still missing in Configuración (ADR 016): without the phone the client
 * emails go out with no way to cancel or change; without the notification email she gets no web booking notices.
 * Shows nothing when both are set or when the settings cannot be read.
 */
@Component({
  selector: 'app-settings-reminder',
  imports: [RouterLink],
  template: `
    @if (missing().length > 0) {
      <div class="reminder" role="status">
        <ul>
          @for (item of missing(); track item) {
            <li>{{ item }}</li>
          }
        </ul>
        <a class="btn btn-small" routerLink="/configuracion">Completar en Configuración</a>
      </div>
    }
  `,
  styles: `
    .reminder {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 12px 16px;
      background: #fff8e6;
      border: 1px solid var(--line);
      border-left: 4px solid var(--gold);
      border-radius: 10px;
    }

    ul {
      margin: 0;
      padding-left: 20px;
      line-height: 1.5;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsReminder implements OnInit {
  private readonly settings = inject(SettingsService);
  private readonly current = signal<BusinessSettings | null>(null);

  protected readonly missing = computed(() => {
    const current = this.current();
    if (!current) {
      return [];
    }
    const items: string[] = [];
    if (!current.contactPhone) {
      items.push('Falta tu celular de contacto: los emails a las clientas salen sin un número para cancelar o cambiar el turno.');
    }
    if (!current.notificationEmail) {
      items.push('Falta tu email de avisos: no te llegan los turnos que sacan las clientas desde la web.');
    }
    return items;
  });

  ngOnInit(): void {
    this.settings.get().subscribe({ next: (current) => this.current.set(current), error: () => undefined });
  }
}
