import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthTokenStore } from '../services/auth-token-store';

/** Only lets a signed-in user through; anyone else is sent to the login. */
export const authGuard: CanActivateFn = () => {
  const isSignedIn = inject(AuthTokenStore).token() !== null;
  return isSignedIn ? true : inject(Router).createUrlTree(['/login']);
};

/** Keeps a signed-in user away from the login page. */
export const guestGuard: CanActivateFn = () => {
  const isSignedIn = inject(AuthTokenStore).token() !== null;
  return isSignedIn ? inject(Router).createUrlTree(['/']) : true;
};
