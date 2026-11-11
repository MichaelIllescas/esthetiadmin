import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthTokenStore } from '../services/auth-token-store';

const LOGIN_URL = `${environment.apiUrl}/auth/login`;
/** The public booking API (ADR 014): no session is involved, so it never gets the token nor sends anyone to the login. */
const PUBLIC_URL = `${environment.apiUrl}/public/`;

/**
 * Adds the session token to API requests only (never to third parties) and, when the API rejects
 * the token, ends the session and goes back to the login.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const tokenStore = inject(AuthTokenStore);
  const router = inject(Router);

  const isApiRequest = request.url.startsWith(environment.apiUrl) && !request.url.startsWith(PUBLIC_URL);
  const token = tokenStore.token();
  const authorized =
    isApiRequest && token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;

  return next(authorized).pipe(
    catchError((error: unknown) => {
      const sessionRejected =
        error instanceof HttpErrorResponse && error.status === 401 && isApiRequest && request.url !== LOGIN_URL;
      if (sessionRejected) {
        tokenStore.clear();
        void router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
