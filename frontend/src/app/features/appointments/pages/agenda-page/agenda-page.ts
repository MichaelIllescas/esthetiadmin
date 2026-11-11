import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageHelp } from '../../../../shared/components/page-help/page-help';
import { SettingsReminder } from '../../../settings/components/settings-reminder/settings-reminder';
import { Agenda } from '../../components/agenda/agenda';
import { BookingLink } from '../../components/booking-link/booking-link';

@Component({
  selector: 'app-agenda-page',
  imports: [PageHelp, Agenda, BookingLink, SettingsReminder],
  template: `
    <header class="page-header">
      <h1>Agenda</h1>
      <p class="intro">Los turnos del día o de la semana: los que cargás vos y los que sacan las clientas desde la web.</p>
    </header>

    <app-settings-reminder />

    <app-booking-link />

    <app-page-help summary="¿Cómo funciona la agenda?">
      <ul>
        <li>Los turnos que dicen <strong>Web</strong> los sacaron las clientas desde la página de turnos. Quedan confirmados solos.</li>
        <li><strong>+ Nuevo turno</strong> te muestra los horarios libres del servicio y el día que elijas. Si querés dar un turno fuera de tu horario habitual, activá <strong>Fuera del horario de atención</strong>. Lo que nunca se puede es superponer dos turnos.</li>
        <li><strong>Reprogramar</strong> cambia el día, la hora o el servicio, y libera el horario anterior.</li>
        <li><strong>Cancelar</strong> y <strong>No vino</strong> liberan el horario para que lo pueda tomar otra persona. "No vino" queda registrado.</li>
        <li><strong>Emails</strong>: cuando una clienta saca turno desde la web y deja su email, le llega la confirmación sola. Cuando vos cargás, reprogramás o cancelás un turno, te preguntamos si querés avisarle por email; solo se puede si el turno tiene email cargado. <strong>Enviar email</strong> le vuelve a mandar la confirmación.</li>
        <li><strong>Atendido</strong>: si el turno vino de la web, primero te pide decir si es un cliente que ya tenés o uno nuevo. Después te ofrece registrar el tratamiento con el servicio y la fecha ya cargados. El turno solo no cuenta como ingreso: lo que suma en los reportes es el tratamiento.</li>
      </ul>
    </app-page-help>

    <app-agenda />
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

  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AgendaPage {}
