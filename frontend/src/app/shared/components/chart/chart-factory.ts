import { Injectable } from '@angular/core';
import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  ChartConfiguration,
  DoughnutController,
  Legend,
  LinearScale,
  LineController,
  LineElement,
  PointElement,
  Tooltip,
} from 'chart.js';

// Only what the reports use is registered, so the rest of Chart.js stays out of the bundle.
Chart.register(
  BarController,
  BarElement,
  LineController,
  LineElement,
  PointElement,
  DoughnutController,
  ArcElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
);

Chart.defaults.font.family = "'Trebuchet MS', 'Segoe UI', sans-serif";
Chart.defaults.color = '#3d3032';

/** The kinds of chart the app draws. */
export type ChartSpec = ChartConfiguration<'bar' | 'line' | 'doughnut'>;

export interface ChartHandle {
  update(spec: ChartSpec): void;
  destroy(): void;
}

/** The only place that talks to Chart.js, so screens and tests can work without a real canvas. */
@Injectable({ providedIn: 'root' })
export class ChartFactory {
  create(canvas: HTMLCanvasElement, spec: ChartSpec): ChartHandle {
    const chart = new Chart(canvas, this.withAccessibleMotion(spec));
    return {
      update: (next) => {
        const config = this.withAccessibleMotion(next);
        chart.data = config.data;
        chart.options = config.options ?? {};
        chart.update();
      },
      destroy: () => chart.destroy(),
    };
  }

  /** People who asked their system for less motion get charts without animation. */
  private withAccessibleMotion(spec: ChartSpec): ChartSpec {
    const reduce = typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
    return reduce ? { ...spec, options: { ...spec.options, animation: false } } : spec;
  }
}
