import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageHelp } from '../../../../shared/components/page-help/page-help';
import { TreatmentList } from '../../components/treatment-list/treatment-list';

@Component({
  selector: 'app-treatments-page',
  imports: [PageHelp, TreatmentList],
  template: `
    <header class="page-header">
      <h1>Tratamientos</h1>
      <p class="intro">Registrá lo que hiciste con cada cliente y consultá el historial.</p>
    </header>

    <app-page-help>
      <ul>
        <li>Un tratamiento es un servicio <strong>ya hecho y cobrado</strong>. Es lo que suma como ingreso en los reportes.</li>
        <li>El importe se completa solo con el precio del servicio, pero lo podés cambiar si hiciste un descuento o un recargo. Un tratamiento sin cargo va con importe 0.</li>
        <li>La fecha no puede ser futura.</li>
        <li>Cuando marcás un turno como <strong>atendido</strong> en la Agenda, te ofrece registrar el tratamiento con todo ya cargado.</li>
        <li>Con los filtros buscás por cliente, categoría, servicio o fechas.</li>
        <li>Editar y eliminar sirven para corregir errores. Al editar no se puede cambiar el cliente.</li>
      </ul>
    </app-page-help>
    <app-treatment-list />
  `,
  styles: `
    :host {
      display: grid;
      width: 100%;
      min-width: 0;
      max-width: 100%;
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
export class TreatmentsPage {}
