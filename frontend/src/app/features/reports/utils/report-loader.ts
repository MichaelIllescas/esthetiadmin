import { computed, Signal, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { catchError, map, Observable, of, switchMap, tap } from 'rxjs';
import { apiErrorMessage } from '../../../shared/utils/api-error';

export interface ReportLoader<T> {
  readonly data: Signal<T | null>;
  readonly isLoading: Signal<boolean>;
  readonly error: Signal<string | null>;
  readonly reload: () => void;
}

type Outcome<T> = { ok: true; value: T } | { ok: false; message: string };

/**
 * Loads a report every time its parameters change, dropping the answer of an outdated request, and keeps the
 * loading and error state for the screen. It must be called where injection is allowed (a field initializer).
 */
export function createReportLoader<P, T>(
  params: Signal<P>,
  fetch: (params: P) => Observable<T>,
  fallbackMessage: string,
): ReportLoader<T> {
  const data = signal<T | null>(null);
  const isLoading = signal(true);
  const error = signal<string | null>(null);
  const retries = signal(0);
  const trigger = computed(() => ({ params: params(), retries: retries() }));

  toObservable(trigger)
    .pipe(
      tap(() => {
        isLoading.set(true);
        error.set(null);
      }),
      switchMap(({ params: current }) =>
        fetch(current).pipe(
          map((value): Outcome<T> => ({ ok: true, value })),
          catchError((failure: unknown) => of<Outcome<T>>({ ok: false, message: apiErrorMessage(failure, fallbackMessage) })),
        ),
      ),
      takeUntilDestroyed(),
    )
    .subscribe((outcome) => {
      isLoading.set(false);
      if (outcome.ok) {
        data.set(outcome.value);
      } else {
        error.set(outcome.message);
      }
    });

  return {
    data: data.asReadonly(),
    isLoading: isLoading.asReadonly(),
    error: error.asReadonly(),
    reload: () => retries.update((count) => count + 1),
  };
}
