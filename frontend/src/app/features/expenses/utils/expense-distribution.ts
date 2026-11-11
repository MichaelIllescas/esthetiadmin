import { ChartSpec } from '../../../shared/components/chart/chart-factory';
import { formatPercent, formatPrice } from '../../../shared/utils/format';
import { SLICE_COLORS } from '../../reports/utils/chart-specs';
import { Expense } from '../models/expense.models';

export interface TypeShare {
  expenseTypeId: number;
  name: string;
  total: number;
  count: number;
  /** Share of the total, 0–100 with 2 decimals. */
  percentage: number;
}

/** How the given expenses split by type, biggest first. Types without expenses do not appear. */
export function distributionByType(expenses: Expense[]): TypeShare[] {
  const byType = new Map<number, TypeShare>();
  for (const expense of expenses) {
    const share = byType.get(expense.expenseTypeId) ?? {
      expenseTypeId: expense.expenseTypeId,
      name: expense.expenseTypeName,
      total: 0,
      count: 0,
      percentage: 0,
    };
    share.total += expense.amount;
    share.count += 1;
    byType.set(expense.expenseTypeId, share);
  }
  const shares = [...byType.values()];
  const grand = shares.reduce((sum, share) => sum + share.total, 0);
  for (const share of shares) {
    share.percentage = grand > 0 ? Math.round((share.total / grand) * 10000) / 100 : 0;
  }
  return shares.sort((a, b) => b.total - a.total || a.name.localeCompare(b.name, 'es'));
}

/** The palette is shared with the income breakdown, so the same look means the same kind of chart. */
export function sliceColor(index: number): string {
  return SLICE_COLORS[index % SLICE_COLORS.length];
}

export function distributionSpec(shares: TypeShare[]): ChartSpec {
  return {
    type: 'doughnut',
    data: {
      labels: shares.map((share) => share.name),
      datasets: [
        {
          data: shares.map((share) => share.total),
          backgroundColor: shares.map((_, index) => sliceColor(index)),
          borderColor: '#ffffff',
          borderWidth: 2,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '58%',
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (item) => {
              const share = shares[item.dataIndex];
              return `${share.name}: ${formatPrice(share.total)} (${formatPercent(share.percentage)})`;
            },
          },
        },
      },
    },
  };
}
