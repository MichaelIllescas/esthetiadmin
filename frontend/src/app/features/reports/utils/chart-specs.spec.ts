import { CategoryIncome, MonthlyRow, TopService } from '../models/report.models';
import { barsHeight, breakdownSpec, monthlySpec, SLICE_COLORS, topServicesSpec, truncateLabel } from './chart-specs';

const months: MonthlyRow[] = [
  { month: '2026-08', income: 4500, expenses: 800, balance: 3700 },
  { month: '2026-09', income: 300, expenses: 5000, balance: -4700 },
];

describe('chart specs', () => {
  it('draws income, expenses and balance as three lines, one label per month', () => {
    const spec = monthlySpec(months);
    const datasets = spec.data.datasets as unknown as Array<{ label: string; data: number[]; borderDash: number[]; pointStyle: string }>;

    expect((spec.data.labels as string[]).map((label) => label.replace(/\s/g, ' '))).toHaveLength(2);
    expect((spec.data.labels as string[])[0]).toMatch(/^ago/);
    expect(spec.type).toBe('line');
    expect(datasets.map((d) => d.label)).toEqual(['Ingresos', 'Gastos', 'Resultado']);
    expect(datasets[0].data).toEqual([4500, 300]);
    expect(datasets[1].data).toEqual([800, 5000]);
    expect(datasets[2].data).toEqual([3700, -4700]);
  });

  it('tells the three lines apart without colors: a different point each and the balance dashed', () => {
    const datasets = monthlySpec(months).data.datasets as unknown as Array<{ borderDash: number[]; pointStyle: string }>;

    expect(new Set(datasets.map((d) => d.pointStyle)).size).toBe(3);
    expect(datasets[0].borderDash).toEqual([]);
    expect(datasets[1].borderDash).toEqual([]);
    expect(datasets[2].borderDash.length).toBeGreaterThan(0);
  });

  it('writes the amounts of the monthly tooltip as money', () => {
    const tooltip = monthlySpec(months).options?.plugins?.tooltip?.callbacks?.label as (item: unknown) => string;

    expect(tooltip({ dataset: { label: 'Gastos' }, raw: 1500 }).replace(/\s/g, ' ')).toBe('Gastos: $ 1.500');
  });

  it('draws the services as horizontal bars of the number of treatments', () => {
    const services: TopService[] = [
      { serviceId: 1, serviceName: 'Bozo', categoryId: 1, categoryName: 'Depilación', treatments: 6, income: 2800 },
      { serviceId: 2, serviceName: 'Limpieza', categoryId: 2, categoryName: 'Faciales', treatments: 1, income: 4000 },
    ];

    const spec = topServicesSpec(services);

    expect(spec.options?.indexAxis).toBe('y');
    expect(spec.data.labels).toEqual(['Bozo', 'Limpieza']);
    expect(spec.data.datasets[0].data).toEqual([6, 1]);
  });

  it('draws the income of each category as a slice, repeating the colors when there are many', () => {
    const categories: CategoryIncome[] = Array.from({ length: 10 }, (_, index) => ({
      categoryId: index,
      categoryName: `Categoría ${index}`,
      income: 100 * (index + 1),
      percentage: 10,
      treatments: 1,
      services: [],
    }));

    const spec = breakdownSpec(categories);
    const colors = spec.data.datasets[0].backgroundColor as string[];

    expect(spec.type).toBe('doughnut');
    expect(colors).toHaveLength(10);
    expect(colors[0]).toBe(SLICE_COLORS[0]);
    expect(colors[SLICE_COLORS.length]).toBe(SLICE_COLORS[0]);
  });

  it('explains each slice with its amount and its share', () => {
    const categories: CategoryIncome[] = [
      { categoryId: 1, categoryName: 'Depilación', income: 4800, percentage: 54.55, treatments: 7, services: [] },
    ];
    const tooltip = breakdownSpec(categories).options?.plugins?.tooltip?.callbacks?.label as (item: unknown) => string;

    expect(tooltip({ dataIndex: 0 }).replace(/\s/g, ' ')).toBe('Depilación: $ 4.800 (54,55 %)');
  });

  it('cuts a very long name for the axis and leaves a short one alone', () => {
    expect(truncateLabel('Bozo')).toBe('Bozo');
    expect(truncateLabel('x'.repeat(24))).toBe('x'.repeat(24));
    const cut = truncateLabel('DepilacionLaserDefinitivaZonaCompletaSinEspacios');
    expect(cut).toHaveLength(24);
    expect(cut.endsWith('…')).toBe(true);
  });

  it('gives the horizontal bars room for every label, with a minimum', () => {
    expect(barsHeight(1)).toBe(120);
    expect(barsHeight(10)).toBe(390);
  });
});
