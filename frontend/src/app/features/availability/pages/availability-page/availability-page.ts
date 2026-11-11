import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageHelp } from '../../../../shared/components/page-help/page-help';
import { BlockList } from '../../components/block-list/block-list';
import { OpeningHoursEditor } from '../../components/opening-hours-editor/opening-hours-editor';

@Component({
  selector: 'app-availability-page',
  imports: [PageHelp, OpeningHoursEditor, BlockList],
  template: `
    <header class="page-header">
      <h1>Horarios</h1>
      <p class="intro">Configurá cuándo atendés: estos son los horarios en que se pueden sacar turnos.</p>
    </header>

    <app-page-help>
      <h2>Horarios de atención</h2>
      <ul>
        <li>Son los días y horas en que atendés todas las semanas. Los turnos solo se ofrecen dentro de estos horarios.</li>
        <li>Se repiten <strong>todas las semanas hasta que los cambies</strong>: lo que cargues para el lunes vale para todos los lunes. Para un cambio de un solo día (un feriado, un día que salís antes) no cambies los horarios: usá un <strong>bloqueo</strong>.</li>
        <li>Un día puede tener más de un horario, por ejemplo de <strong>9:00 a 13:00</strong> y de <strong>15:00 a 19:00</strong>. En el medio no se dan turnos.</li>
        <li>Un día sin horarios queda <strong>cerrado</strong>.</li>
        <li>Las horas van siempre en punto o y media (9:00, 9:30, 10:00...), porque los turnos empiezan cada media hora.</li>
        <li>Para cambiarlos tocá <strong>Editar horarios</strong>, hacé los cambios y tocá <strong>Guardar horarios</strong>. Si te arrepentís, <strong>Cancelar</strong> deja todo como estaba.</li>
      </ul>

      <h2>Duración de los turnos</h2>
      <ul>
        <li>Los turnos de <strong>Depilación duran media hora</strong> y los del resto de los servicios, <strong>una hora</strong>. Se cambia por categoría desde <strong>Servicios</strong>.</li>
        <li>El turno tiene que terminar dentro del horario. Si atendés hasta las 13:00, el último turno de una hora es a las 12:00, y el último de media hora, a las 12:30.</li>
        <li>Se atiende a una persona a la vez: un horario ocupado no se vuelve a ofrecer.</li>
      </ul>

      <h2>Bloqueos</h2>
      <ul>
        <li>Sirven para los días u horas en que no vas a atender aunque te toque: feriados, vacaciones, un trámite o un turno médico.</li>
        <li>Para varios días seguidos (por ejemplo, vacaciones) creá <strong>un solo bloqueo</strong> con la fecha "Desde" y "Hasta". Para un solo día, poné la misma fecha en las dos.</li>
        <li>Un bloqueo de un solo día puede ser de <strong>todo el día</strong> o de un rango de horas. Uno de varios días siempre toma los días completos. El motivo es opcional y solo lo ves vos.</li>
        <li>Cuando el bloqueo pasa, deja de aparecer en la lista. Si lo eliminás antes, ese horario vuelve a quedar disponible.</li>
      </ul>

      <h2>Para tener en cuenta</h2>
      <ul>
        <li><strong>Cambiar los horarios o crear un bloqueo no cancela los turnos que ya están dados.</strong> Si un bloqueo cae sobre turnos tomados, te avisamos cuáles son para que los canceles o los reprogrames desde la agenda.</li>
        <li>Las clientas pueden sacar turno en la página pública hasta <strong>60 días</strong> hacia adelante, y nunca en horarios que ya pasaron.</li>
      </ul>
    </app-page-help>

    <app-opening-hours-editor />
    <app-block-list />
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
export class AvailabilityPage {}
