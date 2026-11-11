import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageHelp } from '../../../../shared/components/page-help/page-help';
import { ExpenseList } from '../../components/expense-list/expense-list';

@Component({
  selector: 'app-expenses-page',
  imports: [PageHelp, ExpenseList],
  template: `
    <header class="page-header">
      <h1>Gastos</h1>
      <p class="intro">Registrá los egresos del centro y controlá cuánto gastaste en cada período.</p>
    </header>

    <app-page-help>
      <ul>
        <li>Cada gasto lleva fecha, concepto, importe (mayor a cero) y tipo.</li>
        <li>Los tipos son fijos (alquiler, insumos, impuestos, etc.). Para lo que no encaje en ninguno, usá <strong>Varios</strong>.</li>
        <li>Filtrá por fechas y por tipo para ver cuánto gastaste en un período: el total aparece arriba de la lista.</li>
        <li>Los gastos se restan de los ingresos en <strong>Reportes</strong>.</li>
      </ul>
    </app-page-help>
    <app-expense-list />
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
export class ExpensesPage {}
