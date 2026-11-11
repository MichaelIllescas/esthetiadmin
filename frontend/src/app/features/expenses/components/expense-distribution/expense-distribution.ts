import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ChartCanvas } from '../../../../shared/components/chart/chart-canvas';
import { formatPercent, formatPrice } from '../../../../shared/utils/format';
import { Expense } from '../../models/expense.models';
import { distributionByType, distributionSpec, sliceColor } from '../../utils/expense-distribution';

/** How the expenses on screen split by type. With a single type there is nothing to compare, so it stays hidden. */
@Component({
  selector: 'app-expense-distribution',
  imports: [ChartCanvas],
  templateUrl: './expense-distribution.html',
  styleUrl: './expense-distribution.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExpenseDistribution {
  readonly expenses = input.required<Expense[]>();

  protected readonly shares = computed(() => distributionByType(this.expenses()));
  protected readonly spec = computed(() => distributionSpec(this.shares()));
  protected readonly label = computed(() => {
    const parts = this.shares().map((share) => `${share.name} ${formatPercent(share.percentage)}`);
    return `Gráfico de anillo con la distribución de los gastos por tipo: ${parts.join(', ')}. Los mismos datos están en la lista de al lado.`;
  });

  protected readonly formatPrice = formatPrice;
  protected readonly formatPercent = formatPercent;
  protected readonly color = sliceColor;
}
