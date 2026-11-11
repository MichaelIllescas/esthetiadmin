import { ChartSpec } from '../../../shared/components/chart/chart-factory';
import { formatCompactPrice, formatMonth, formatPercent, formatPrice } from '../../../shared/utils/format';
import { CategoryIncome, MonthlyRow, TopService } from '../models/report.models';

/** Colors of the chart series. They keep enough contrast against the white card and between themselves. */
export const COLORS = {
  income: '#643342',
  expenses: '#a47c3d',
  balance: '#3f654f',
  grid: 'rgba(164, 124, 61, 0.2)',
};

/** One color per slice; the palette repeats when there are more categories than colors. */
export const SLICE_COLORS = ['#643342', '#a47c3d', '#3f654f', '#b87a86', '#7a8f9e', '#c9a66b', '#5c4a3d', '#8e6f9a'];

/**
 * Income, expenses and balance per month as three lines. Besides the color, each line has its own point shape and
 * the balance is dashed, so they can be told apart without seeing colors.
 */
export function monthlySpec(months: MonthlyRow[]): ChartSpec {
  const line = (label: string, data: number[], color: string, pointStyle: 'circle' | 'rect' | 'triangle', dashed = false) => ({
    label,
    data,
    borderColor: color,
    backgroundColor: color,
    borderWidth: 2.5,
    borderDash: dashed ? [7, 4] : [],
    pointStyle,
    pointRadius: 4,
    pointHoverRadius: 6,
    tension: 0.25,
  });

  return {
    type: 'line',
    data: {
      labels: months.map((row) => formatMonth(row.month)),
      datasets: [
        line('Ingresos', months.map((row) => row.income), COLORS.income, 'circle'),
        line('Gastos', months.map((row) => row.expenses), COLORS.expenses, 'rect'),
        line('Resultado', months.map((row) => row.balance), COLORS.balance, 'triangle', true),
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { position: 'bottom', labels: { usePointStyle: true } },
        tooltip: { callbacks: { label: (item) => `${item.dataset.label}: ${formatPrice(Number(item.raw))}` } },
      },
      scales: {
        x: { grid: { display: false } },
        y: { grid: { color: COLORS.grid }, ticks: { callback: (value) => formatCompactPrice(Number(value)) } },
      },
    },
  };
}

/** Horizontal bars with how many times each service was performed. */
export function topServicesSpec(services: TopService[]): ChartSpec {
  return {
    type: 'bar',
    data: {
      labels: services.map((service) => service.serviceName),
      datasets: [{ label: 'Tratamientos', data: services.map((service) => service.treatments), backgroundColor: COLORS.income }],
    },
    options: {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: { callbacks: { label: (item) => `${item.raw} tratamientos` } },
      },
      scales: {
        x: { beginAtZero: true, grid: { color: COLORS.grid }, ticks: { precision: 0 } },
        y: {
          grid: { display: false },
          ticks: {
            callback(value) {
              return truncateLabel(String(this.getLabelForValue(Number(value))));
            },
          },
        },
      },
    },
  };
}

/** Share of the income of each category. */
export function breakdownSpec(categories: CategoryIncome[]): ChartSpec {
  return {
    type: 'doughnut',
    data: {
      labels: categories.map((category) => category.categoryName),
      datasets: [
        {
          data: categories.map((category) => category.income),
          backgroundColor: categories.map((_, index) => SLICE_COLORS[index % SLICE_COLORS.length]),
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
        legend: { position: 'bottom' },
        tooltip: {
          callbacks: {
            label: (item) => {
              const category = categories[item.dataIndex];
              return `${category.categoryName}: ${formatPrice(category.income)} (${formatPercent(category.percentage)})`;
            },
          },
        },
      },
    },
  };
}

/** Cuts a long name so it fits next to the bars; the tooltip still shows the whole name. */
export function truncateLabel(label: string, max = 24): string {
  return label.length > max ? `${label.slice(0, max - 1)}…` : label;
}

/** Height (px) for the horizontal bars: enough room for every label, whatever the number of services. */
export function barsHeight(rows: number): number {
  return Math.max(120, rows * 34 + 50);
}
