import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChartFactory, ChartSpec } from './chart-factory';
import { ChartCanvas } from './chart-canvas';

const specOf = (label: string): ChartSpec => ({
  type: 'bar',
  data: { labels: ['a'], datasets: [{ label, data: [1] }] },
});

@Component({
  imports: [ChartCanvas],
  template: `<app-chart [spec]="spec()" label="Ingresos por mes" [height]="height" />`,
})
class Host {
  readonly spec = signal<ChartSpec>(specOf('first'));
  height = 240;
}

describe('ChartCanvas', () => {
  const handle = { update: vi.fn(), destroy: vi.fn() };
  const factory = { create: vi.fn(() => handle) };
  let fixture: ComponentFixture<Host>;

  beforeEach(async () => {
    handle.update.mockClear();
    handle.destroy.mockClear();
    factory.create.mockClear();
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [{ provide: ChartFactory, useValue: factory }],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('draws the chart once on a canvas that describes it for screen readers', () => {
    const canvas = fixture.nativeElement.querySelector('canvas') as HTMLCanvasElement;

    expect(canvas.getAttribute('role')).toBe('img');
    expect(canvas.getAttribute('aria-label')).toBe('Ingresos por mes');
    expect(factory.create).toHaveBeenCalledTimes(1);
    expect(factory.create).toHaveBeenCalledWith(canvas, expect.objectContaining({ type: 'bar' }));
  });

  it('reserves the height it was given', () => {
    expect((fixture.nativeElement.querySelector('.box') as HTMLElement).style.height).toBe('240px');
  });

  it('follows the new data without drawing a second chart', () => {
    const next = specOf('second');

    fixture.componentInstance.spec.set(next);
    fixture.detectChanges();

    expect(factory.create).toHaveBeenCalledTimes(1);
    expect(handle.update).toHaveBeenLastCalledWith(next);
  });

  it('destroys the chart when the screen goes away', () => {
    fixture.destroy();

    expect(handle.destroy).toHaveBeenCalledTimes(1);
  });
});
