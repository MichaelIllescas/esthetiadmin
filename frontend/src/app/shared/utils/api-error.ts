import { HttpErrorResponse } from '@angular/common/http';

/** The message the API sends (already in Spanish) or, when there is none, a fallback for the user. */
export function apiErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    const message = (error.error as { message?: unknown } | null)?.message;
    if (typeof message === 'string' && message.trim()) {
      return message;
    }
    if (error.status === 0) {
      return 'No pudimos conectarnos con el servidor. Revisá tu conexión e intentá nuevamente.';
    }
  }
  return fallback;
}
