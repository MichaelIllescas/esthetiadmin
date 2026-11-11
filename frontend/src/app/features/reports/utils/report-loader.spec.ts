import { HttpErrorResponse } from '@angular/common/http';
import { Injector, runInInjectionContext, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { createReportLoader } from './report-loader';

describe('createReportLoader', () => {
  const create = <P, T>(params: () => P, fetch: (p: P) => import('rxjs').Observable<T>) =>
    runInInjectionContext(TestBed.inject(Injector), () => createReportLoader(params as never, fetch, 'No pudimos cargar el reporte.'));

  it('loads on start and exposes the data', () => {
    const loader = create(signal('a'), (p: string) => of(`data of ${p}`));
    TestBed.tick();

    expect(loader.data()).toBe('data of a');
    expect(loader.isLoading()).toBe(false);
    expect(loader.error()).toBeNull();
  });

  it('is loading until the answer arrives', () => {
    const answer = new Subject<string>();
    const loader = create(signal('a'), () => answer);
    TestBed.tick();
    expect(loader.isLoading()).toBe(true);

    answer.next('ready');
    TestBed.tick();

    expect(loader.isLoading()).toBe(false);
    expect(loader.data()).toBe('ready');
  });

  it('loads again when the parameters change', () => {
    const params = signal('a');
    const fetch = vi.fn((p: string) => of(p.toUpperCase()));
    const loader = create(params, fetch);
    TestBed.tick();

    params.set('b');
    TestBed.tick();

    expect(fetch).toHaveBeenCalledTimes(2);
    expect(loader.data()).toBe('B');
  });

  it('ignores the answer of an outdated request', () => {
    const first = new Subject<string>();
    const second = new Subject<string>();
    const params = signal(1);
    const loader = create(params, (p: number) => (p === 1 ? first : second));
    TestBed.tick();

    params.set(2);
    TestBed.tick();
    second.next('new');
    first.next('old');
    TestBed.tick();

    expect(loader.data()).toBe('new');
  });

  it('shows the message of the API when it fails, and can try again', () => {
    let attempt = 0;
    const loader = create(signal('a'), () =>
      ++attempt === 1
        ? throwError(() => new HttpErrorResponse({ status: 400, error: { message: 'El período no puede superar los 5 años' } }))
        : of('ok'),
    );
    TestBed.tick();
    expect(loader.error()).toBe('El período no puede superar los 5 años');
    expect(loader.data()).toBeNull();

    loader.reload();
    TestBed.tick();

    expect(loader.error()).toBeNull();
    expect(loader.data()).toBe('ok');
  });

  it('falls back to a generic message when the API sends none', () => {
    const loader = create(signal('a'), () => throwError(() => new HttpErrorResponse({ status: 500 })));
    TestBed.tick();

    expect(loader.error()).toBe('No pudimos cargar el reporte.');
  });
});
