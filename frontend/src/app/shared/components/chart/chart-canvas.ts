import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  viewChild,
} from '@angular/core';
import { ChartFactory, ChartHandle, ChartSpec } from './chart-factory';

/**
 * Draws a chart. The chart is only a picture for a screen reader, so `label` must say what it shows and the
 * screen must also offer the same data as a table.
 */
@Component({
  selector: 'app-chart',
  template: `
    <div class="box" [style.height.px]="height()">
      <canvas #canvas role="img" [attr.aria-label]="label()"></canvas>
    </div>
  `,
  styles: `
    :host {
      display: block;
      min-width: 0;
      max-width: 100%;
    }

    .box {
      position: relative;
      width: 100%;
      min-width: 0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChartCanvas {
  private readonly factory = inject(ChartFactory);
  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private handle: ChartHandle | null = null;

  readonly spec = input.required<ChartSpec>();
  readonly label = input.required<string>();
  readonly height = input(300);

  constructor() {
    afterNextRender(() => {
      this.handle = this.factory.create(this.canvas().nativeElement, this.spec());
    });
    // The spec must be read on every run, even before the chart exists, or the effect would never run again.
    effect(() => {
      const spec = this.spec();
      this.handle?.update(spec);
    });
    inject(DestroyRef).onDestroy(() => this.handle?.destroy());
  }
}
