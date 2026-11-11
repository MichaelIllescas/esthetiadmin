import { computed, inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { AuthTokenStore } from './auth-token-store';
import { readJwtPayload } from './jwt';

/** The current session: who is signed in and how to end it. */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly tokenStore = inject(AuthTokenStore);
  private readonly router = inject(Router);

  readonly username = computed(() => {
    const token = this.tokenStore.token();
    return token ? (readJwtPayload(token)?.sub ?? null) : null;
  });

  logout(): void {
    this.tokenStore.clear();
    void this.router.navigate(['/login']);
  }
}
